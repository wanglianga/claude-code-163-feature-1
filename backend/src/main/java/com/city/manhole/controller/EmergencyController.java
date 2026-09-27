package com.city.manhole.controller;

import com.city.manhole.common.ApiResponse;
import com.city.manhole.dto.Req;
import com.city.manhole.model.EmergencyMeasure;
import com.city.manhole.service.EmergencyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/incidents/{incidentId}/emergency")
@RequiredArgsConstructor
public class EmergencyController {
    private final EmergencyService emergencyService;

    @PostMapping("/dispatch")
    public ApiResponse<EmergencyMeasure> dispatch(@PathVariable Long incidentId) {
        return ApiResponse.ok(emergencyService.dispatch(incidentId));
    }

    @PostMapping("/secure")
    public ApiResponse<EmergencyMeasure> secure(@PathVariable Long incidentId,
                                                @RequestBody Req.SecureReq req) {
        return ApiResponse.ok(emergencyService.secure(incidentId, req.measures(), req.cost(), req.note()));
    }
}
