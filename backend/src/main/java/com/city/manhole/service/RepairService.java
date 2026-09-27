package com.city.manhole.service;

import com.city.manhole.common.AuthContext;
import com.city.manhole.common.BizException;
import com.city.manhole.common.Constants;
import com.city.manhole.model.Incident;
import com.city.manhole.model.RepairOrder;
import com.city.manhole.model.User;
import com.city.manhole.repo.RepairOrderRepo;
import com.city.manhole.repo.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** 维修工单：权属单位派单 → 维修队接单 → 填写维修信息并完成。 */
@Service
@RequiredArgsConstructor
public class RepairService {
    private final RepairOrderRepo repairOrderRepo;
    private final UserRepo userRepo;
    private final IncidentService incidentService;
    private final NotificationService notificationService;

    /** 权属单位派发维修工单 */
    @Transactional
    public RepairOrder createOrder(Long incidentId, Long teamId) {
        User me = AuthContext.requireRole(Constants.Role.OWNER_UNIT);
        Incident inc = incidentService.getOrThrow(incidentId);
        if (!Constants.OwnerStatus.CONFIRMED.equals(inc.getOwnerStatus()) || !me.getId().equals(inc.getOwnerId())) {
            throw new BizException("只有已确认权属的单位才能派单");
        }
        if (repairOrderRepo.findByIncidentId(incidentId).isPresent()) {
            throw new BizException("该事件已存在维修工单");
        }
        User team = userRepo.findById(teamId == null ? -1 : teamId)
                .filter(u -> Constants.Role.REPAIR_TEAM.equals(u.getRole()))
                .orElseThrow(() -> new BizException("维修队不存在"));

        RepairOrder o = new RepairOrder();
        o.setIncidentId(incidentId);
        o.setOwnerId(me.getId());
        o.setTeamId(team.getId());
        o.setStatus(Constants.RepairStatus.ASSIGNED);
        repairOrderRepo.save(o);

        inc.setStatus(Constants.IncidentStatus.REPAIRING);
        incidentService.save(inc);

        notificationService.notify(team.getId(), inc.getId(),
                "新维修工单 " + inc.getCode(), inc.getAddress() + " 井盖" + inc.getIssueType() + "，请尽快接单维修。");
        return o;
    }

    /** 维修队接单 */
    @Transactional
    public RepairOrder accept(Long orderId) {
        User me = AuthContext.requireRole(Constants.Role.REPAIR_TEAM);
        RepairOrder o = myOrder(orderId, me);
        if (!Constants.RepairStatus.ASSIGNED.equals(o.getStatus())
                && !Constants.RepairStatus.RETURNED.equals(o.getStatus())) {
            throw new BizException("工单当前状态不能接单");
        }
        o.setStatus(Constants.RepairStatus.ACCEPTED);
        o.setAcceptedAt(LocalDateTime.now());
        return repairOrderRepo.save(o);
    }

    /** 维修队补充维修信息并完成 */
    @Transactional
    public RepairOrder finish(Long orderId, String chamberCondition, String coverSpec,
                              Boolean needRoadClosure, Boolean needCoord, String coordUnits,
                              String repairNote, BigDecimal repairCost) {
        User me = AuthContext.requireRole(Constants.Role.REPAIR_TEAM);
        RepairOrder o = myOrder(orderId, me);
        if (!Constants.RepairStatus.ACCEPTED.equals(o.getStatus())) {
            throw new BizException("请先接单再完成维修");
        }
        if (chamberCondition == null || chamberCondition.isBlank()) throw new BizException("请填写井室状况");
        if (coverSpec == null || coverSpec.isBlank()) throw new BizException("请填写井盖规格");

        o.setArrivedAt(LocalDateTime.now());
        o.setChamberCondition(chamberCondition);
        o.setCoverSpec(coverSpec);
        o.setNeedRoadClosure(Boolean.TRUE.equals(needRoadClosure));
        o.setNeedCoord(Boolean.TRUE.equals(needCoord));
        o.setCoordUnits(Boolean.TRUE.equals(needCoord) ? coordUnits : null);
        o.setRepairNote(repairNote);
        o.setRepairCost(repairCost == null ? BigDecimal.ZERO : repairCost);
        o.setFinishedAt(LocalDateTime.now());
        o.setStatus(Constants.RepairStatus.FINISHED);
        repairOrderRepo.save(o);

        Incident inc = incidentService.getOrThrow(o.getIncidentId());
        inc.setStatus(Constants.IncidentStatus.REPAIRED);
        incidentService.save(inc);

        notificationService.notifyRole(Constants.Role.INSPECTOR, inc.getId(),
                "维修完成待验收 " + inc.getCode(), inc.getAddress() + " 井盖维修已完成，请安排验收。");
        return o;
    }

    public List<RepairOrder> myOrders() {
        User me = AuthContext.requireRole(Constants.Role.REPAIR_TEAM, Constants.Role.OWNER_UNIT);
        if (Constants.Role.REPAIR_TEAM.equals(me.getRole())) {
            return repairOrderRepo.findByTeamIdOrderByAssignedAtDesc(me.getId());
        }
        return repairOrderRepo.findByOwnerIdOrderByAssignedAtDesc(me.getId());
    }

    private RepairOrder myOrder(Long orderId, User me) {
        RepairOrder o = repairOrderRepo.findById(orderId).orElseThrow(() -> new BizException("工单不存在"));
        if (!o.getTeamId().equals(me.getId())) throw new BizException("只能操作本队工单");
        return o;
    }
}
