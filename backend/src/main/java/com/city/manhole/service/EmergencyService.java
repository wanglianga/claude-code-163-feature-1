package com.city.manhole.service;

import com.city.manhole.common.AuthContext;
import com.city.manhole.common.BizException;
import com.city.manhole.common.Constants;
import com.city.manhole.model.EmergencyMeasure;
import com.city.manhole.model.Incident;
import com.city.manhole.model.User;
import com.city.manhole.repo.EmergencyMeasureRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 应急处置线：街道值班员派单 → 到场 → 临时围护。
 * 只考核现场风险与到场时效，不与权属确认挂钩。
 */
@Service
@RequiredArgsConstructor
public class EmergencyService {
    private final EmergencyMeasureRepo emergencyRepo;
    private final IncidentService incidentService;
    private final NotificationService notificationService;

    public static final List<String> MEASURE_TYPES = List.of("警示锥", "临时盖板", "夜间反光标识");

    /** 派单（值班员接单出发） */
    @Transactional
    public EmergencyMeasure dispatch(Long incidentId) {
        User me = AuthContext.requireRole(Constants.Role.STREET_DUTY, Constants.Role.ADMIN);
        Incident inc = incidentService.getOrThrow(incidentId);
        if (!Constants.EmergencyStatus.PENDING.equals(inc.getEmergencyStatus())) {
            throw new BizException("该事件不在待派单状态");
        }
        EmergencyMeasure em = emergencyRepo.findByIncidentId(incidentId).orElseGet(EmergencyMeasure::new);
        em.setIncidentId(incidentId);
        em.setOfficerId(me.getId());
        em.setDispatchedAt(LocalDateTime.now());
        emergencyRepo.save(em);

        inc.setEmergencyStatus(Constants.EmergencyStatus.DISPATCHED);
        incidentService.save(inc);
        return em;
    }

    /** 到场并完成临时围护：记录措施、费用，事件进入权属排查 */
    @Transactional
    public EmergencyMeasure secure(Long incidentId, List<String> measures, BigDecimal cost, String note) {
        User me = AuthContext.requireRole(Constants.Role.STREET_DUTY, Constants.Role.ADMIN);
        Incident inc = incidentService.getOrThrow(incidentId);
        if (Constants.EmergencyStatus.SECURED.equals(inc.getEmergencyStatus())) {
            throw new BizException("该事件已完成临时围护");
        }
        if (measures == null || measures.isEmpty()) {
            throw new BizException("请至少选择一项临时围护措施");
        }
        for (String m : measures) {
            if (!MEASURE_TYPES.contains(m)) throw new BizException("不支持的围护措施: " + m);
        }
        LocalDateTime now = LocalDateTime.now();
        EmergencyMeasure em = emergencyRepo.findByIncidentId(incidentId).orElseGet(EmergencyMeasure::new);
        em.setIncidentId(incidentId);
        em.setOfficerId(me.getId());
        if (em.getDispatchedAt() == null) {
            em.setDispatchedAt(now); // 未派单直接处置的，视为即时派单
        }
        em.setArrivedAt(now);
        em.setSecuredAt(now);
        em.setMeasures(String.join(",", measures));
        em.setCost(cost == null ? BigDecimal.ZERO : cost);
        em.setNote(note);
        emergencyRepo.save(em);

        inc.setEmergencyStatus(Constants.EmergencyStatus.SECURED);
        if (Constants.IncidentStatus.REPORTED.equals(inc.getStatus())) {
            inc.setStatus(Constants.IncidentStatus.SECURED);
        }
        // 应急完成后自动进入权属排查
        if (Constants.OwnerStatus.UNCONFIRMED.equals(inc.getOwnerStatus())) {
            inc.setOwnerStatus(Constants.OwnerStatus.INVESTIGATING);
        }
        incidentService.save(inc);

        notificationService.notify(inc.getReporterId(), inc.getId(),
                "现场已临时围护 " + inc.getCode(), "街道值班员已完成临时围护措施：" + em.getMeasures());
        return em;
    }
}
