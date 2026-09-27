package com.city.manhole.service;

import com.city.manhole.common.AuthContext;
import com.city.manhole.common.BizException;
import com.city.manhole.common.Constants;
import com.city.manhole.model.CostAllocation;
import com.city.manhole.model.Incident;
import com.city.manhole.model.User;
import com.city.manhole.repo.CostAllocationRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 费用分摊复核：应急处置费 + 维修费 + 赔付金。
 * 街道先行垫付的，复核后按权属追责结论分摊，分摊完成事件结案。
 */
@Service
@RequiredArgsConstructor
public class CostService {
    private final CostAllocationRepo costRepo;
    private final IncidentService incidentService;
    private final NotificationService notificationService;

    public List<CostAllocation> listAll() {
        AuthContext.requireRole(Constants.Role.ADMIN, Constants.Role.STREET_DUTY);
        return costRepo.findAllByOrderByCreatedAtDesc();
    }

    /**
     * 复核分摊：allocations = [{party, amount, reason}]，合计须等于总费用。
     */
    @Transactional
    public CostAllocation review(Long id, List<Map<String, Object>> allocations, String reviewNote) {
        User me = AuthContext.requireRole(Constants.Role.ADMIN);
        CostAllocation ca = costRepo.findById(id).orElseThrow(() -> new BizException("费用分摊单不存在"));
        if (!Constants.CostStatus.PENDING.equals(ca.getStatus())) {
            throw new BizException("该分摊单已复核");
        }
        if (allocations == null || allocations.isEmpty()) {
            throw new BizException("请填写分摊明细");
        }
        BigDecimal sum = BigDecimal.ZERO;
        for (Map<String, Object> a : allocations) {
            Object party = a.get("party");
            Object amount = a.get("amount");
            if (party == null || party.toString().isBlank()) throw new BizException("分摊方不能为空");
            if (amount == null) throw new BizException("分摊金额不能为空");
            BigDecimal amt = new BigDecimal(amount.toString());
            if (amt.compareTo(BigDecimal.ZERO) < 0) throw new BizException("分摊金额不能为负");
            sum = sum.add(amt);
        }
        if (sum.compareTo(ca.getTotalAmount()) != 0) {
            throw new BizException("分摊合计 ¥" + sum + " 与总费用 ¥" + ca.getTotalAmount() + " 不一致");
        }
        ca.setAllocations(incidentService.writeJson(allocations));
        ca.setReviewNote(reviewNote);
        ca.setStatus(Constants.CostStatus.ALLOCATED);
        ca.setReviewedBy(me.getId());
        ca.setReviewedAt(LocalDateTime.now());
        costRepo.save(ca);

        // 分摊完成，事件结案
        Incident inc = incidentService.getOrThrow(ca.getIncidentId());
        inc.setStatus(Constants.IncidentStatus.CLOSED);
        inc.setClosedAt(LocalDateTime.now());
        incidentService.save(inc);

        notificationService.notify(inc.getReporterId(), inc.getId(),
                "事件已结案 " + inc.getCode(), "费用分摊复核完成，感谢您对城市安全的关注。");
        notificationService.notifyRole(Constants.Role.STREET_DUTY, inc.getId(),
                "费用分摊已复核 " + inc.getCode(), "复核意见：" + (reviewNote == null ? "无" : reviewNote));
        return ca;
    }
}
