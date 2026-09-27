package com.city.manhole.service;

import com.city.manhole.common.AuthContext;
import com.city.manhole.common.BizException;
import com.city.manhole.common.Constants;
import com.city.manhole.model.CompensationCase;
import com.city.manhole.model.CostAllocation;
import com.city.manhole.model.Incident;
import com.city.manhole.model.User;
import com.city.manhole.repo.CompensationRepo;
import com.city.manhole.repo.CostAllocationRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 赔付定责：依据上报时间、临时围护时效、权属确认、维修时长、事故证明综合判断责任。
 */
@Service
@RequiredArgsConstructor
public class CompensationService {
    private final CompensationRepo compensationRepo;
    private final CostAllocationRepo costRepo;
    private final IncidentService incidentService;
    private final NotificationService notificationService;

    public List<CompensationCase> listAll() {
        AuthContext.requireRole(Constants.Role.COMPENSATOR, Constants.Role.ADMIN);
        return compensationRepo.findAllByOrderByCreatedAtDesc();
    }

    /** 补充/更新案件信息（赔付专员） */
    @Transactional
    public CompensationCase updateCase(Long id, String damageType, BigDecimal claimAmount, String accidentProof) {
        AuthContext.requireRole(Constants.Role.COMPENSATOR);
        CompensationCase c = getOrThrow(id);
        if (!Constants.CompStatus.FILED.equals(c.getStatus())) {
            throw new BizException("案件已审核，不能修改");
        }
        if (damageType != null && !damageType.isBlank()) c.setDamageType(damageType);
        if (claimAmount != null) c.setClaimAmount(claimAmount);
        if (accidentProof != null && !accidentProof.isBlank()) c.setAccidentProof(accidentProof);
        return compensationRepo.save(c);
    }

    /** 定责审核：APPROVED 核定赔付 / REJECTED 驳回 */
    @Transactional
    public CompensationCase decide(Long id, String result, String responsibility,
                                   Integer ownerRatio, Integer streetRatio,
                                   BigDecimal approvedAmount, String analysis) {
        User me = AuthContext.requireRole(Constants.Role.COMPENSATOR);
        CompensationCase c = getOrThrow(id);
        if (!Constants.CompStatus.FILED.equals(c.getStatus())) {
            throw new BizException("案件已审核");
        }
        if (analysis == null || analysis.isBlank()) {
            throw new BizException("请填写责任分析（上报时间、临时围护、权属确认、维修时长、事故证明）");
        }
        c.setSpecialistId(me.getId());
        c.setAnalysis(analysis);
        c.setDecidedAt(LocalDateTime.now());

        if ("APPROVED".equals(result)) {
            if (responsibility == null || responsibility.isBlank()) throw new BizException("请选择责任认定");
            if (approvedAmount == null || approvedAmount.compareTo(BigDecimal.ZERO) < 0) {
                throw new BizException("核定金额不合法");
            }
            int or = ownerRatio == null ? 0 : ownerRatio;
            int sr = streetRatio == null ? 0 : streetRatio;
            if (or + sr != 100 && !Constants.Responsibility.UNDETERMINED.equals(responsibility)) {
                throw new BizException("权属单位与街道分摊比例之和须为 100%");
            }
            c.setResponsibility(responsibility);
            c.setOwnerRatio(or);
            c.setStreetRatio(sr);
            c.setApprovedAmount(approvedAmount);
            c.setStatus(Constants.CompStatus.APPROVED);
        } else if ("REJECTED".equals(result)) {
            c.setStatus(Constants.CompStatus.REJECTED);
            c.setResponsibility(Constants.Responsibility.UNDETERMINED);
            c.setApprovedAmount(BigDecimal.ZERO);
        } else {
            throw new BizException("审核结论不合法");
        }
        compensationRepo.save(c);

        // 同步到费用分摊单（若已生成）
        Incident inc = incidentService.getOrThrow(c.getIncidentId());
        costRepo.findByIncidentId(inc.getId()).ifPresent(ca -> {
            if (Constants.CostStatus.PENDING.equals(ca.getStatus())) {
                ca.setCompensationAmount(c.getApprovedAmount());
                ca.setTotalAmount(ca.getEmergencyCost().add(ca.getRepairCost()).add(c.getApprovedAmount()));
                costRepo.save(ca);
            }
        });
        notificationService.notify(inc.getReporterId(), inc.getId(),
                "赔付案件已审核 " + inc.getCode(),
                "APPROVED".equals(result) ? ("核定赔付 ¥" + c.getApprovedAmount()) : "赔付申请被驳回");
        return c;
    }

    /** 标记赔付完成 */
    @Transactional
    public CompensationCase pay(Long id) {
        AuthContext.requireRole(Constants.Role.COMPENSATOR);
        CompensationCase c = getOrThrow(id);
        if (!Constants.CompStatus.APPROVED.equals(c.getStatus())) {
            throw new BizException("只有已核定的案件才能赔付");
        }
        c.setStatus(Constants.CompStatus.PAID);
        c.setPaidAt(LocalDateTime.now());
        return compensationRepo.save(c);
    }

    private CompensationCase getOrThrow(Long id) {
        return compensationRepo.findById(id).orElseThrow(() -> new BizException("赔付案件不存在"));
    }
}
