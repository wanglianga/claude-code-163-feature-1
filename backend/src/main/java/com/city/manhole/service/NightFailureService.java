package com.city.manhole.service;

import com.city.manhole.common.AuthContext;
import com.city.manhole.common.BizException;
import com.city.manhole.common.Constants;
import com.city.manhole.model.CompensationCase;
import com.city.manhole.model.EmergencyMeasure;
import com.city.manhole.model.Incident;
import com.city.manhole.model.NightProtectionFailure;
import com.city.manhole.model.User;
import com.city.manhole.repo.CompensationRepo;
import com.city.manhole.repo.EmergencyMeasureRepo;
import com.city.manhole.repo.NightProtectionFailureRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 夜间临时围护失效处理：市民首次上报、街道围护后，警示锥夜间被车辆撞倒，
 * 第二名骑车人摔伤。平台串联首次上报 → 围护照片 → 巡查到场 → 警示锥移位时间 → 二次事故，
 * 认定失效原因并决定加派措施；学校门口/公交站附近记录人流高峰、照明、监控编号，
 * 值班员选择绕行提示或交警协助。
 */
@Service
@RequiredArgsConstructor
public class NightFailureService {
    private final NightProtectionFailureRepo nightRepo;
    private final EmergencyMeasureRepo emergencyRepo;
    private final CompensationRepo compensationRepo;
    private final IncidentService incidentService;
    private final NotificationService notificationService;

    public static final List<String> CAUSES = List.of(
            Constants.NightFailureCause.INADEQUATE_PROTECTION,
            Constants.NightFailureCause.PATROL_GAP,
            Constants.NightFailureCause.EXTERNAL_DAMAGE);
    public static final List<String> DECISIONS = List.of(
            Constants.NightDecision.REFLECTIVE_BARRIER,
            Constants.NightDecision.ROAD_CLOSURE,
            Constants.NightDecision.GUARD_DUTY);
    public static final List<String> SENSITIVE_LOCATIONS = List.of(
            Constants.SensitiveLocation.NONE,
            Constants.SensitiveLocation.SCHOOL_GATE,
            Constants.SensitiveLocation.BUS_STOP);
    public static final List<String> LIGHTINGS = List.of("充足", "一般", "昏暗", "无照明");

    /** 登记夜间围护失效与二次事故（街道值班员） */
    @Transactional
    public NightProtectionFailure register(Long incidentId, ReqData r) {
        User me = AuthContext.requireRole(Constants.Role.STREET_DUTY, Constants.Role.ADMIN);
        Incident inc = incidentService.getOrThrow(incidentId);
        if (nightRepo.findByIncidentId(incidentId).isPresent()) {
            throw new BizException("该事件已登记夜间围护失效记录");
        }
        EmergencyMeasure em = emergencyRepo.findByIncidentId(incidentId).orElseThrow(() ->
                new BizException("街道尚未到场放置围护，不能登记围护失效"));
        if (r.displacedAt() == null) throw new BizException("请填写警示锥（围护）移位时间");
        if (r.secondAccidentAt() == null) throw new BizException("请填写二次事故发生时间");
        if (r.secondAccidentDesc() == null || r.secondAccidentDesc().isBlank()) {
            throw new BizException("请描述二次事故经过");
        }
        if (r.displacedAt().isAfter(r.secondAccidentAt())) {
            throw new BizException("警示锥移位时间不能晚于二次事故时间");
        }
        if (r.secondAccidentAt().isBefore(inc.getCreatedAt())) {
            throw new BizException("二次事故时间不能早于首次上报时间");
        }
        String sensitive = r.sensitiveLocation() == null ? Constants.SensitiveLocation.NONE : r.sensitiveLocation();
        if (!SENSITIVE_LOCATIONS.contains(sensitive)) throw new BizException("敏感场所类型不合法");
        if (r.lighting() != null && !LIGHTINGS.contains(r.lighting())) throw new BizException("照明条件不合法");
        if (!Constants.SensitiveLocation.NONE.equals(sensitive)) {
            if (r.peakHours() == null || r.peakHours().isBlank()) throw new BizException("请填写人流高峰时段");
            if (r.cameraIds() == null || r.cameraIds().isBlank()) throw new BizException("请填写周边监控编号");
        }

        NightProtectionFailure f = new NightProtectionFailure();
        f.setIncidentId(incidentId);
        f.setOfficerId(me.getId());
        f.setFirstReportAt(inc.getCreatedAt());
        f.setDisplacedAt(r.displacedAt());
        f.setSecondAccidentAt(r.secondAccidentAt());
        f.setSecondAccidentDesc(r.secondAccidentDesc());
        f.setHasInjury(Boolean.TRUE.equals(r.hasInjury()));
        f.setInjuryDesc(r.injuryDesc());
        f.setProtectionPhotos(incidentService.writeJson(r.protectionPhotos() == null ? List.of() : r.protectionPhotos()));
        f.setSensitiveLocation(sensitive);
        f.setPeakHours(r.peakHours());
        f.setLighting(r.lighting());
        f.setCameraIds(r.cameraIds());
        f.setDetourNotice(Boolean.TRUE.equals(r.detourNotice()));
        f.setDetourNote(r.detourNote());
        f.setTrafficPoliceAssist(Boolean.TRUE.equals(r.trafficPoliceAssist()));
        f.setTrafficPoliceNote(r.trafficPoliceNote());
        if (r.patrolArrivedAt() != null) {
            f.setPatrolId(me.getId());
            f.setPatrolArrivedAt(r.patrolArrivedAt());
            f.setStatus(Constants.NightFailureStatus.VERIFIED);
        } else {
            f.setStatus(Constants.NightFailureStatus.REPORTED);
        }
        nightRepo.save(f);

        // 二次事故造成人车损伤：自动立赔付案件（已有案件则补充二次事故说明）
        if (f.getHasInjury()) {
            String proof = "二次事故（夜间围护失效）：" + f.getSecondAccidentAt() + " " +
                    (f.getInjuryDesc() == null ? f.getSecondAccidentDesc() : f.getInjuryDesc());
            compensationRepo.findByIncidentId(incidentId).ifPresentOrElse(c -> {
                c.setAccidentProof((c.getAccidentProof() == null ? "" : c.getAccidentProof() + "；") + proof);
                compensationRepo.save(c);
            }, () -> {
                CompensationCase c = new CompensationCase();
                c.setIncidentId(incidentId);
                c.setClaimantName(me.getName());
                c.setClaimantPhone(me.getPhone());
                c.setDamageType("待补充");
                c.setAccidentProof(proof);
                c.setStatus(Constants.CompStatus.FILED);
                compensationRepo.save(c);
            });
            inc.setHasDamage(true);
            incidentService.save(inc);
            notificationService.notifyRole(Constants.Role.COMPENSATOR, incidentId,
                    "夜间二次事故赔付预警 " + inc.getCode(),
                    "警示锥夜间被撞倒后发生二次事故，请结合围护失效记录定责。");
        }
        notificationService.notifyRole(Constants.Role.STREET_DUTY, incidentId,
                "夜间围护失效待处置 " + inc.getCode(),
                "二次事故：" + f.getSecondAccidentDesc() + "，请尽快到场核查、认定原因并加派围护。");
        notificationService.notifyRole(Constants.Role.ADMIN, incidentId,
                "夜间围护失效 " + inc.getCode(), "事件发生围护失效及二次事故，请关注处置时效。");
        return f;
    }

    /** 巡查到场核查：记录到场时间与围护/移位现场照片 */
    @Transactional
    public NightProtectionFailure patrol(Long incidentId, List<String> photos, String note) {
        User me = AuthContext.requireRole(Constants.Role.STREET_DUTY,
                Constants.Role.INSPECTOR, Constants.Role.ADMIN);
        NightProtectionFailure f = getOrThrow(incidentId);
        if (Constants.NightFailureStatus.HANDLED.equals(f.getStatus())) {
            throw new BizException("该失效记录已处置完毕");
        }
        f.setPatrolId(me.getId());
        f.setPatrolArrivedAt(LocalDateTime.now());
        if (photos != null && !photos.isEmpty()) {
            List<String> all = incidentService.readJson(f.getProtectionPhotos());
            List<String> merged = new ArrayList<>(all);
            photos.stream().filter(p -> !merged.contains(p)).forEach(merged::add);
            f.setProtectionPhotos(incidentService.writeJson(merged));
        }
        if (note != null && !note.isBlank()) {
            f.setCauseAnalysis(note);
        }
        f.setStatus(Constants.NightFailureStatus.VERIFIED);
        nightRepo.save(f);
        return f;
    }

    /** 认定失效原因：围护方式不足 / 巡查间隔过长 / 外力破坏 */
    @Transactional
    public NightProtectionFailure assess(Long incidentId, String cause, String causeAnalysis) {
        AuthContext.requireRole(Constants.Role.STREET_DUTY, Constants.Role.ADMIN);
        NightProtectionFailure f = getOrThrow(incidentId);
        if (Constants.NightFailureStatus.HANDLED.equals(f.getStatus())) {
            throw new BizException("该失效记录已处置完毕");
        }
        if (!CAUSES.contains(cause)) throw new BizException("失效原因不合法");
        if (causeAnalysis == null || causeAnalysis.isBlank()) {
            throw new BizException("请填写原因认定分析");
        }
        f.setCause(cause);
        f.setCauseAnalysis(causeAnalysis);
        f.setStatus(Constants.NightFailureStatus.ASSESSED);
        nightRepo.save(f);
        return f;
    }

    /** 加派措施处置：反光围挡 / 临时封道 / 专人值守，可追加街道垫付费用；
     * 学校门口/公交站可同步确认绕行提示与交警协助 */
    @Transactional
    public NightProtectionFailure handle(Long incidentId, List<String> decisions, String handleNote,
                                         Boolean detourNotice, String detourNote,
                                         Boolean trafficPoliceAssist, String trafficPoliceNote,
                                         BigDecimal addCost) {
        User me = AuthContext.requireRole(Constants.Role.STREET_DUTY, Constants.Role.ADMIN);
        Incident inc = incidentService.getOrThrow(incidentId);
        NightProtectionFailure f = getOrThrow(incidentId);
        if (decisions == null || decisions.isEmpty()) throw new BizException("请至少选择一项加派措施");
        for (String d : decisions) {
            if (!DECISIONS.contains(d)) throw new BizException("不支持的处置措施: " + d);
        }
        f.setDecisions(String.join(",", decisions));
        f.setHandleNote(handleNote);
        if (detourNotice != null) f.setDetourNotice(detourNotice);
        if (detourNote != null) f.setDetourNote(detourNote);
        if (trafficPoliceAssist != null) f.setTrafficPoliceAssist(trafficPoliceAssist);
        if (trafficPoliceNote != null) f.setTrafficPoliceNote(trafficPoliceNote);
        if (!Constants.SensitiveLocation.NONE.equals(f.getSensitiveLocation())
                && !f.getDetourNotice() && !f.getTrafficPoliceAssist()) {
            throw new BizException("学校门口/公交站附近须至少安排绕行提示或申请交警协助");
        }
        f.setStatus(Constants.NightFailureStatus.HANDLED);
        f.setHandledBy(me.getId());
        f.setHandledAt(LocalDateTime.now());
        nightRepo.save(f);

        // 加派措施费用并入街道应急垫付，供后续费用复核分摊
        EmergencyMeasure em = emergencyRepo.findByIncidentId(incidentId).orElse(null);
        if (em != null && addCost != null && addCost.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal cost = em.getCost() == null ? BigDecimal.ZERO : em.getCost();
            em.setCost(cost.add(addCost));
            emergencyRepo.save(em);
        }

        String detail = "加派措施：" + f.getDecisions()
                + (f.getDetourNotice() ? "；已安排绕行提示" : "")
                + (f.getTrafficPoliceAssist() ? "；已请交警协助" : "");
        notificationService.notify(inc.getReporterId(), incidentId,
                "夜间围护已加固 " + inc.getCode(), detail + "，请注意安全。");
        notificationService.notifyRole(Constants.Role.COMPENSATOR, incidentId,
                "围护失效处置完成 " + inc.getCode(),
                "失效原因：" + causeName(f.getCause()) + "；" + detail + "，可供赔付定责参考。");
        return f;
    }

    /**
     * 依据原因、危险等级、敏感场所、照明给出处置建议（值班员可调整）。
     */
    public Map<String, Object> suggest(Long incidentId) {
        AuthContext.requireLogin();
        Incident inc = incidentService.getOrThrow(incidentId);
        NightProtectionFailure f = nightRepo.findByIncidentId(incidentId)
                .orElseThrow(() -> new BizException("夜间围护失效记录不存在"));
        List<String> decisions = new ArrayList<>();
        List<String> reasons = new ArrayList<>();
        boolean dim = f.getLighting() != null && List.of("昏暗", "无照明").contains(f.getLighting());
        boolean sensitive = !Constants.SensitiveLocation.NONE.equals(f.getSensitiveLocation());

        switch (f.getCause() == null ? "" : f.getCause()) {
            case Constants.NightFailureCause.INADEQUATE_PROTECTION -> {
                decisions.add(Constants.NightDecision.REFLECTIVE_BARRIER);
                reasons.add("原仅放置警示锥，夜间辨识度与抗撞能力不足，应加派反光围挡");
                if ("高".equals(inc.getDangerLevel()) || dim) {
                    decisions.add(Constants.NightDecision.GUARD_DUTY);
                    reasons.add("高危/照明不足，反光围挡外应安排专人值守至维修完成");
                }
            }
            case Constants.NightFailureCause.PATROL_GAP -> {
                decisions.add(Constants.NightDecision.GUARD_DUTY);
                reasons.add("巡查间隔过长导致围护移位未被及时发现，应安排专人值守");
                decisions.add(Constants.NightDecision.REFLECTIVE_BARRIER);
                reasons.add("同步加派反光围挡提升夜间可视距离");
            }
            case Constants.NightFailureCause.EXTERNAL_DAMAGE -> {
                decisions.add(Constants.NightDecision.REFLECTIVE_BARRIER);
                if (sensitive || "高".equals(inc.getDangerLevel())) {
                    decisions.add(Constants.NightDecision.ROAD_CLOSURE);
                    reasons.add("存在外力撞击且现场人流/风险高，应临时封道防止再次冲撞");
                } else {
                    decisions.add(Constants.NightDecision.GUARD_DUTY);
                    reasons.add("外力破坏重复发生风险高，应专人值守并保留监控证据");
                }
                reasons.add("围护遭车辆撞击移位，应先恢复反光围挡并调取周边监控");
            }
            default -> {
                decisions.add(Constants.NightDecision.REFLECTIVE_BARRIER);
                reasons.add("尚未认定原因，默认先加派反光围挡控制现场风险");
            }
        }
        boolean suggestDetour = sensitive && (dim || f.getPeakHours() != null);
        boolean suggestPolice = sensitive
                && Constants.NightFailureCause.EXTERNAL_DAMAGE.equals(f.getCause());
        if (sensitive) {
            if (suggestDetour) reasons.add(placeName(f.getSensitiveLocation())
                    + "为人流密集点，高峰时段（" + f.getPeakHours() + "）建议安排绕行提示");
            if (dim) reasons.add("照明条件为「" + f.getLighting() + "」，应协调补强照明并提示绕行");
            if (suggestPolice) reasons.add("外力破坏发生在" + placeName(f.getSensitiveLocation())
                    + "，建议请交警协助查缉肇事车辆并维持秩序");
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("decisions", decisions);
        m.put("suggestDetourNotice", suggestDetour);
        m.put("suggestTrafficPolice", suggestPolice);
        m.put("reasons", reasons);
        return m;
    }

    public NightProtectionFailure getOrThrow(Long incidentId) {
        return nightRepo.findByIncidentId(incidentId)
                .orElseThrow(() -> new BizException("夜间围护失效记录不存在"));
    }

    public static String causeName(String c) {
        return switch (c == null ? "" : c) {
            case Constants.NightFailureCause.INADEQUATE_PROTECTION -> "围护方式不足";
            case Constants.NightFailureCause.PATROL_GAP -> "巡查间隔过长";
            case Constants.NightFailureCause.EXTERNAL_DAMAGE -> "外力破坏";
            default -> "未认定";
        };
    }

    public static String placeName(String p) {
        return switch (p == null ? "" : p) {
            case Constants.SensitiveLocation.SCHOOL_GATE -> "学校门口";
            case Constants.SensitiveLocation.BUS_STOP -> "公交站附近";
            default -> "现场";
        };
    }

    /** 服务层入参（避免直接依赖 Web DTO 记录） */
    public record ReqData(LocalDateTime displacedAt, LocalDateTime secondAccidentAt,
                          String secondAccidentDesc, Boolean hasInjury, String injuryDesc,
                          List<String> protectionPhotos, String sensitiveLocation,
                          String peakHours, String lighting, String cameraIds,
                          Boolean detourNotice, String detourNote,
                          Boolean trafficPoliceAssist, String trafficPoliceNote,
                          LocalDateTime patrolArrivedAt) {}
}
