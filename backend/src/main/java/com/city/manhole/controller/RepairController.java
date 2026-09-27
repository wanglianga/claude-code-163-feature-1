package com.city.manhole.controller;

import com.city.manhole.common.ApiResponse;
import com.city.manhole.dto.Req;
import com.city.manhole.model.RepairOrder;
import com.city.manhole.service.RepairService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class RepairController {
    private final RepairService repairService;

    @PostMapping("/api/incidents/{incidentId}/repair-order")
    public ApiResponse<RepairOrder> create(@PathVariable Long incidentId,
                                           @RequestBody Req.RepairOrderReq req) {
        return ApiResponse.ok(repairService.createOrder(incidentId, req.teamId()));
    }

    @PostMapping("/api/repair-orders/{orderId}/accept")
    public ApiResponse<RepairOrder> accept(@PathVariable Long orderId) {
        return ApiResponse.ok(repairService.accept(orderId));
    }

    @PostMapping("/api/repair-orders/{orderId}/finish")
    public ApiResponse<RepairOrder> finish(@PathVariable Long orderId,
                                           @RequestBody Req.RepairFinishReq req) {
        return ApiResponse.ok(repairService.finish(orderId, req.chamberCondition(), req.coverSpec(),
                req.needRoadClosure(), req.needCoord(), req.coordUnits(), req.repairNote(), req.repairCost()));
    }

    @GetMapping("/api/repair-orders")
    public ApiResponse<List<RepairOrder>> myOrders() {
        return ApiResponse.ok(repairService.myOrders());
    }
}
