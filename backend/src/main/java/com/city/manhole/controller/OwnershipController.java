package com.city.manhole.controller;

import com.city.manhole.common.ApiResponse;
import com.city.manhole.dto.Req;
import com.city.manhole.model.OwnerClaim;
import com.city.manhole.model.OwnershipInvestigation;
import com.city.manhole.service.OwnershipService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/incidents/{incidentId}")
@RequiredArgsConstructor
public class OwnershipController {
    private final OwnershipService ownershipService;

    @PostMapping("/investigation")
    public ApiResponse<OwnershipInvestigation> investigate(@PathVariable Long incidentId,
                                                           @RequestBody Req.InvestigateReq req) {
        return ApiResponse.ok(ownershipService.investigate(incidentId, req.manholeMarking(),
                req.pipelineInfo(), req.constructionHistory(), req.merchantInfo(), req.cityLedger(),
                req.conclusion(), req.confirmedOwnerId(), req.note()));
    }

    @PostMapping("/claim")
    public ApiResponse<OwnerClaim> claim(@PathVariable Long incidentId, @RequestBody Req.ClaimReq req) {
        return ApiResponse.ok(ownershipService.claim(incidentId, req.note()));
    }
}
