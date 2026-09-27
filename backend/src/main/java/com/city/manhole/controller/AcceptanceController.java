package com.city.manhole.controller;

import com.city.manhole.common.ApiResponse;
import com.city.manhole.dto.Req;
import com.city.manhole.model.Acceptance;
import com.city.manhole.service.AcceptanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/incidents/{incidentId}/acceptance")
@RequiredArgsConstructor
public class AcceptanceController {
    private final AcceptanceService acceptanceService;

    @PostMapping
    public ApiResponse<Acceptance> submit(@PathVariable Long incidentId,
                                          @RequestBody Req.AcceptanceReq req) {
        return ApiResponse.ok(acceptanceService.submit(incidentId, req.photoOk(), req.locationOk(),
                req.flatnessOk(), req.rollingTestOk(), req.result(), req.note()));
    }
}
