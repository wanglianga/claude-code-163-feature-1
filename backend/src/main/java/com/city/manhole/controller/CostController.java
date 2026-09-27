package com.city.manhole.controller;

import com.city.manhole.common.ApiResponse;
import com.city.manhole.dto.Req;
import com.city.manhole.model.CostAllocation;
import com.city.manhole.service.CostService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cost-allocations")
@RequiredArgsConstructor
public class CostController {
    private final CostService costService;

    @GetMapping
    public ApiResponse<List<CostAllocation>> list() {
        return ApiResponse.ok(costService.listAll());
    }

    @PostMapping("/{id}/review")
    public ApiResponse<CostAllocation> review(@PathVariable Long id,
                                              @RequestBody Req.CostReviewReq req) {
        return ApiResponse.ok(costService.review(id, req.allocations(), req.reviewNote()));
    }
}
