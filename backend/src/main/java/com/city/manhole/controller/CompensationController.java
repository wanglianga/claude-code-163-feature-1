package com.city.manhole.controller;

import com.city.manhole.common.ApiResponse;
import com.city.manhole.dto.Req;
import com.city.manhole.model.CompensationCase;
import com.city.manhole.service.CompensationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/compensations")
@RequiredArgsConstructor
public class CompensationController {
    private final CompensationService compensationService;

    @GetMapping
    public ApiResponse<List<CompensationCase>> list() {
        return ApiResponse.ok(compensationService.listAll());
    }

    @PutMapping("/{id}")
    public ApiResponse<CompensationCase> update(@PathVariable Long id,
                                                @RequestBody Req.CompUpdateReq req) {
        return ApiResponse.ok(compensationService.updateCase(id, req.damageType(),
                req.claimAmount(), req.accidentProof()));
    }

    @PostMapping("/{id}/decide")
    public ApiResponse<CompensationCase> decide(@PathVariable Long id,
                                                @RequestBody Req.CompDecideReq req) {
        return ApiResponse.ok(compensationService.decide(id, req.result(), req.responsibility(),
                req.ownerRatio(), req.streetRatio(), req.approvedAmount(), req.analysis()));
    }

    @PostMapping("/{id}/pay")
    public ApiResponse<CompensationCase> pay(@PathVariable Long id) {
        return ApiResponse.ok(compensationService.pay(id));
    }
}
