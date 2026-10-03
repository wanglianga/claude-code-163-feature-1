package com.city.manhole.service;

import com.city.manhole.common.AuthContext;
import com.city.manhole.common.BizException;
import com.city.manhole.common.Constants;
import com.city.manhole.model.*;
import com.city.manhole.repo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

/**
 * 夜间临时围护失效处理。
 *
 * 时间链：首次上报 → 围护完成（围护照片/措施）→ 夜间巡查到场 → 警示锥被撞移位 → 二次事故。
 * 据时间链判定失效原因（围护方式不足 / 巡查间隔过长 / 外力破坏），
 * 值班员决定加派反光围挡、临时封道或专人值守；
 * 学校门口/公交站附近记录人流高峰、照明、监控编号，并选择绕行提示/交警协助。
 */
@Service
@RequiredArgsConstructor
public class NightFailureService {
    private final NightFailureRepo failureRepo;
    private final NightPatrolRepo patrolRepo;
    private final EmergencyMeasureRepo emergencyRepo;
    private final IncidentService incidentService;
    private final CompensationRepo compensationRepo;
    private final NotificationService notificationService;

    @Value("${app.night-patrol-limit-minutes:180}")
    private long patrolLimitMinutes;

    public static final List<String> CAUSES = List.of(
            Constants.FailureCause.INADEQUATE,
            Constants.FailureCause.INTERVAL_TOO_LONG,
            Constants.FailureCause.EXTERNAL_DAMAGE);
    public static final List<String> REINFORCE_MEASURES = List.of(
            Constants.ReinforceMeasure.REFLECTIVE_BARRIER,
            Constants.ReinforceMeasure.TEMP_CLOSURE,
            Constants.ReinforceMeasure.GUARD_DUTY);
    public static final List<String> LOCATION_TYPES = List.of("学校门口", "公交站");
    public static final List<String> LIGHTINGS = List.of("良好", "一般", "昏暗无路灯");

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("MM-dd HH:mm");

    /** 夜间巡查到场 */
    @Transactional
    public NightPatrol patrol(Long incidentId, String roundName, LocalDateTime arrivedAt, Boolean intact,
                              String siteStatus, List<String> photos, LocalDateTime coneMovedAt, String note) {
        User me = AuthContext.requireRole(Constants.Role.STREET_DUTY, Constants.Role.INSPECTOR, Constants.Role.ADMIN);
        Incident inc = incidentService.getOrThrow(incidentId);
        if (!Constants.EmergencyStatus.SECURED.equals(inc.getEmergencyStatus())) {
            throw new BizException("事件尚未完成临时围护，不能登记夜间巡查");
        }
        LocalDateTime at = arrivedAt == null ? LocalDateTime.now() : arrivedAt;
        NightPatrol p = new NightPatrol();
        p.setIncidentId(incidentId);
        p.setOfficerId(me.getId());
        p.setRoundName(roundName);
        p.setArrivedAt(at);
        p.setIntact(!Boolean.FALSE.equals(intact));
        p.setSiteStatus(siteStatus);
        p.setPhotos(incidentService.writeJson(photos == null ? List.of() : photos));
        p.setConeMovedAt(coneMovedAt);
        p.setNote(note);
        patrolRepo.save(p);

        if (Boolean.FALSE.equals(intact) || coneMovedAt != null) {
            notificationService.notifyRole(Constants.Role.STREET_DUTY, inc.getId(),
                    "夜间巡查发现围护异常 " + inc.getCode(),
                    roundName == null ? "巡查发现围护可能失效，请立即核实并登记处理。"
                            : roundName + "发现围护可能失效，请立即核实并登记处理。");
        }
        return p;
    }

    /** 登记围护失效（可同时登记二次事故、原因判定与处置决策） */
    @Transactional
    public NightFailure reportFailure(Long incidentId, ReqFields r) {
        User me = AuthContext.requireRole(Constants.Role.STREET_DUTY, Constants.Role.ADMIN);
        Incident inc = incidentService.getOrThrow(incidentId);
        EmergencyMeasure em = emergencyRepo.findByIncidentId(incidentId)
                .orElseThrow(() -> new BizException("该事件尚无临时围护记录"));
        if (r.coneMovedAt() == null) throw new BizException("请填写警示锥移位/被撞倒时间");
        if (r.hasSecondAccident() != null && r.hasSecondAccident() && r.secondAccidentAt() == null) {
            throw new BizException("请填写二次事故发生时间");
        }

        NightFailure nf = new NightFailure();
        nf.setCode(genCode());
        nf.setIncidentId(incidentId);
        nf.setOfficerId(me.getId());
        nf.setConeMovedAt(r.coneMovedAt());
        nf.setReportedBy(r.reportedBy());
        nf.setPhotos(incidentService.writeJson(r.photos() == null ? List.of() : r.photos()));
        nf.setHasSecondAccident(Boolean.TRUE.equals(r.hasSecondAccident()));

        // 最近巡查到场时间：表单优先，否则取移位时间之前最近一次巡查
        LocalDateTime lastPatrol = r.lastPatrolAt();
        if (lastPatrol == null) {
            lastPatrol = patrolRepo
                    .findFirstByIncidentIdAndArrivedAtLessThanEqualOrderByArrivedAtDesc(incidentId, r.coneMovedAt())
                    .map(NightPatrol::getArrivedAt).orElse(em.getArrivedAt());
        }
        nf.setLastPatrolAt(lastPatrol);
        if (lastPatrol != null) {
            nf.setPatrolGapMinutes(ChronoUnit.MINUTES.between(lastPatrol, r.coneMovedAt()));
        }

        if (nf.getHasSecondAccident()) {
            fillAccident(nf, r);
        }
        fillSensitive(nf, r);

        // 原因判定（未选择时按时间链自动建议）
        String cause = validateCause(r.cause());
        if (cause == null) cause = suggestCause(em, nf.getPatrolGapMinutes());
        nf.setCause(cause);
        nf.setCauseAnalysis(r.causeAnalysis() != null && !r.causeAnalysis().isBlank()
                ? r.causeAnalysis() : buildAnalysis(inc, em, nf, cause));

        // 处置决策
        if (r.reinforceMeasures() != null && !r.reinforceMeasures().isEmpty()) {
            applyDecision(nf, me.getId(), r.reinforceMeasures(), r.decisionNote(),
                    r.detourNotice(), r.detourNote(), r.trafficPoliceAssist(), r.trafficPoliceNote());
        } else {
            nf.setDetourNotice(Boolean.TRUE.equals(r.detourNotice()));
            nf.setDetourNote(r.detourNote());
            nf.setTrafficPoliceAssist(Boolean.TRUE.equals(r.trafficPoliceAssist()));
            nf.setTrafficPoliceNote(r.trafficPoliceNote());
        }
        failureRepo.save(nf);

        // 二次事故自动立赔付案件
        if (nf.getHasSecondAccident()) {
            CompensationCase c = new CompensationCase();
            c.setIncidentId(incidentId);
            c.setCaseType("SECOND");
            c.setNightFailureId(nf.getId());
            c.setClaimantName(nf.getInjuredName() == null || nf.getInjuredName().isBlank()
                    ? "二次事故骑车人" : nf.getInjuredName());
            c.setClaimantPhone(nf.getInjuredPhone());
            c.setDamageType("人身伤害");
            c.setAccidentProof("夜间围护失效二次事故：" + safe(nf.getInjuryDesc()) + "；" + safe(nf.getAccidentDesc()));
            c.setStatus(Constants.CompStatus.FILED);
            compensationRepo.save(c);
            nf.setCompensationId(c.getId());
            failureRepo.save(nf);

            notificationService.notifyRole(Constants.Role.COMPENSATOR, inc.getId(),
                    "二次事故赔付案件 " + inc.getCode() + "（" + nf.getCode() + "）",
                    "夜间警示锥被撞移位后骑车人摔伤，请结合围护失效记录定责。");
        }

        notificationService.notifyRole(Constants.Role.STREET_DUTY, inc.getId(),
                "夜间围护失效已登记 " + nf.getCode(),
                "失效原因：" + causeLabel(cause) + "，请尽快落实加固处置。");
        if (nf.getReinforceMeasures() != null && nf.getReinforceMeasures().contains(Constants.ReinforceMeasure.TEMP_CLOSURE)) {
            notificationService.notifyRole(Constants.Role.ADMIN, inc.getId(),
                    "已决定临时封道 " + nf.getCode(), "请协调封道资源并跟踪撤除时间。");
        }
        return nf;
    }

    /** 补充/修改原因判定与处置决策（值班员研判后提交） */
    @Transactional
    public NightFailure decide(Long failureId, String cause, String causeAnalysis, List<String> reinforceMeasures,
                               String decisionNote, Boolean detourNotice, String detourNote,
                               Boolean trafficPoliceAssist, String trafficPoliceNote) {
        User me = AuthContext.requireRole(Constants.Role.STREET_DUTY, Constants.Role.ADMIN);
        NightFailure nf = getOrThrow(failureId);
        if (cause != null && !cause.isBlank()) {
            String c = validateCause(cause);
            nf.setCause(c);
        }
        if (causeAnalysis != null && !causeAnalysis.isBlank()) nf.setCauseAnalysis(causeAnalysis);
        if (reinforceMeasures != null) {
            if (reinforceMeasures.isEmpty()) throw new BizException("请至少选择一项加固措施");
            applyDecision(nf, me.getId(), reinforceMeasures, decisionNote,
                    detourNotice, detourNote, trafficPoliceAssist, trafficPoliceNote);
        } else {
            if (detourNotice != null) nf.setDetourNotice(detourNotice);
            if (detourNote != null) nf.setDetourNote(detourNote);
            if (trafficPoliceAssist != null) nf.setTrafficPoliceAssist(trafficPoliceAssist);
            if (trafficPoliceNote != null) nf.setTrafficPoliceNote(trafficPoliceNote);
        }
        return failureRepo.save(nf);
    }

    /** 确认加固处置已落实 */
    @Transactional
    public NightFailure confirmReinforced(Long failureId, String note) {
        AuthContext.requireRole(Constants.Role.STREET_DUTY, Constants.Role.INSPECTOR, Constants.Role.ADMIN);
        NightFailure nf = getOrThrow(failureId);
        if (nf.getReinforceMeasures() == null || nf.getReinforceMeasures().isBlank()) {
            throw new BizException("尚未作出加固处置决策");
        }
        nf.setReinforced(true);
        nf.setReinforcedAt(LocalDateTime.now());
        if (note != null && !note.isBlank()) nf.setDecisionNote(safe(nf.getDecisionNote()) + "；落实情况：" + note);
        failureRepo.save(nf);

        Incident inc = incidentService.getOrThrow(nf.getIncidentId());
        notificationService.notify(inc.getReporterId(), inc.getId(),
                "夜间围护已加固 " + nf.getCode(), "现场已落实：" + nf.getReinforceMeasures() + "，请注意绕行。");
        return nf;
    }

    public List<NightFailure> listAll() {
        AuthContext.requireLogin();
        return failureRepo.findAllByOrderByCreatedAtDesc();
    }

    // ------------------------------------------------------------------

    private void applyDecision(NightFailure nf, Long officerId, List<String> measures, String note,
                               Boolean detourNotice, String detourNote,
                               Boolean trafficPoliceAssist, String trafficPoliceNote) {
        for (String m : measures) {
            if (!REINFORCE_MEASURES.contains(m)) throw new BizException("不支持的加固措施: " + m);
        }
        nf.setReinforceMeasures(String.join(",", measures));
        nf.setDecisionAt(LocalDateTime.now());
        nf.setDecisionOfficerId(officerId);
        nf.setDecisionNote(note);
        nf.setDetourNotice(Boolean.TRUE.equals(detourNotice));
        nf.setDetourNote(detourNote);
        nf.setTrafficPoliceAssist(Boolean.TRUE.equals(trafficPoliceAssist));
        nf.setTrafficPoliceNote(trafficPoliceNote);
    }

    private void fillAccident(NightFailure nf, ReqFields r) {
        nf.setSecondAccidentAt(r.secondAccidentAt());
        nf.setInjuredName(r.injuredName());
        nf.setInjuredPhone(r.injuredPhone());
        nf.setInjuryDesc(r.injuryDesc());
        nf.setAccidentDesc(r.accidentDesc());
        nf.setAccidentPhotos(incidentService.writeJson(
                r.accidentPhotos() == null ? List.of() : r.accidentPhotos()));
    }

    private void fillSensitive(NightFailure nf, ReqFields r) {
        boolean sensitive = Boolean.TRUE.equals(r.sensitiveLocation());
        nf.setSensitiveLocation(sensitive);
        if (!sensitive) return;
        if (r.locationType() != null && !LOCATION_TYPES.contains(r.locationType())) {
            throw new BizException("敏感地点类型不合法");
        }
        nf.setLocationType(r.locationType());
        nf.setLocationName(r.locationName());
        nf.setPeakHours(r.peakHours());
        if (r.lighting() != null && !r.lighting().isBlank() && !LIGHTINGS.contains(r.lighting())) {
            throw new BizException("照明条件不合法");
        }
        nf.setLighting(r.lighting());
        nf.setCameraIds(r.cameraIds());
    }

    /**
     * 原因自动建议：
     * 1. 距上次巡查超过夜间巡查时限 → 巡查间隔过长；
     * 2. 仅设警示锥、无反光标识/盖板等 → 围护方式不足；
     * 3. 短时内仍被撞倒 → 外力破坏。
     */
    private String suggestCause(EmergencyMeasure em, Long gapMinutes) {
        if (gapMinutes != null && gapMinutes > patrolLimitMinutes) {
            return Constants.FailureCause.INTERVAL_TOO_LONG;
        }
        Set<String> ms = Set.of(safe(em.getMeasures()).split(","));
        if (!ms.contains("夜间反光标识") && !ms.contains("临时盖板")) {
            return Constants.FailureCause.INADEQUATE;
        }
        return Constants.FailureCause.EXTERNAL_DAMAGE;
    }

    private String buildAnalysis(Incident inc, EmergencyMeasure em, NightFailure nf, String cause) {
        StringBuilder sb = new StringBuilder();
        sb.append("时间链：首次上报 ").append(inc.getCreatedAt().format(FMT));
        sb.append("；围护完成 ").append(em.getSecuredAt() == null ? "-" : em.getSecuredAt().format(FMT))
          .append("（措施：").append(safe(em.getMeasures())).append("）");
        if (nf.getLastPatrolAt() != null) {
            sb.append("；最近巡查到场 ").append(nf.getLastPatrolAt().format(FMT));
        }
        sb.append("；警示锥移位 ").append(nf.getConeMovedAt().format(FMT));
        if (nf.getPatrolGapMinutes() != null) {
            sb.append("（距上次巡查 ").append(nf.getPatrolGapMinutes()).append(" 分钟，夜间巡查时限 ")
              .append(patrolLimitMinutes).append(" 分钟）");
        }
        if (nf.getHasSecondAccident() && nf.getSecondAccidentAt() != null) {
            sb.append("；二次事故 ").append(nf.getSecondAccidentAt().format(FMT));
        }
        sb.append("。判定：").append(causeLabel(cause)).append("。");
        switch (cause) {
            case Constants.FailureCause.INADEQUATE -> sb.append("原围护仅警示锥、夜间可视性与抗冲击不足，应加派反光围挡。");
            case Constants.FailureCause.INTERVAL_TOO_LONG -> sb.append("失效时已超过夜间巡查时限，应缩短巡查间隔或安排专人值守。");
            default -> sb.append("巡查间隔内围护遭车辆撞倒，属外力破坏，宜临时封道并协调交警核查监控。");
        }
        return sb.toString();
    }

    private String validateCause(String cause) {
        if (cause == null || cause.isBlank()) return null;
        if (!CAUSES.contains(cause)) throw new BizException("失效原因不合法");
        return cause;
    }

    public static String causeLabel(String cause) {
        return switch (cause) {
            case Constants.FailureCause.INADEQUATE -> "围护方式不足";
            case Constants.FailureCause.INTERVAL_TOO_LONG -> "巡查间隔过长";
            case Constants.FailureCause.EXTERNAL_DAMAGE -> "外力破坏";
            default -> cause;
        };
    }

    public static String reinforceLabel(String m) {
        return switch (m) {
            case Constants.ReinforceMeasure.REFLECTIVE_BARRIER -> "加派反光围挡";
            case Constants.ReinforceMeasure.TEMP_CLOSURE -> "临时封道";
            case Constants.ReinforceMeasure.GUARD_DUTY -> "专人值守";
            default -> m;
        };
    }

    private NightFailure getOrThrow(Long id) {
        return failureRepo.findById(id).orElseThrow(() -> new BizException("夜间围护失效记录不存在"));
    }

    private String genCode() {
        String prefix = "NF" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return prefix + "-" + String.format("%04d", failureRepo.countByCodeStartingWith(prefix) + 1);
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    /** 便于服务内部传递的字段集合 */
    public record ReqFields(LocalDateTime lastPatrolAt, LocalDateTime coneMovedAt, String reportedBy,
                            List<String> photos, Boolean hasSecondAccident, LocalDateTime secondAccidentAt,
                            String injuredName, String injuredPhone, String injuryDesc, String accidentDesc,
                            List<String> accidentPhotos, String cause, String causeAnalysis,
                            List<String> reinforceMeasures, String decisionNote,
                            Boolean sensitiveLocation, String locationType, String locationName,
                            String peakHours, String lighting, String cameraIds,
                            Boolean detourNotice, String detourNote,
                            Boolean trafficPoliceAssist, String trafficPoliceNote) {}
}
