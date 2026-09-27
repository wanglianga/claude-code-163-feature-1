package com.city.manhole.controller;

import com.city.manhole.common.ApiResponse;
import com.city.manhole.dto.Req;
import com.city.manhole.model.Incident;
import com.city.manhole.service.IncidentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/incidents")
@RequiredArgsConstructor
public class IncidentController {
    private final IncidentService incidentService;

    @PostMapping
    public ApiResponse<Incident> create(@RequestBody Req.IncidentCreateReq req) {
        return ApiResponse.ok(incidentService.create(req.issueType(), req.longitude(), req.latitude(),
                req.address(), req.roadType(), req.photos(), req.hasDamage(),
                req.damageDesc(), req.dangerLevel()));
    }

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list() {
        return ApiResponse.ok(incidentService.listForMe());
    }

    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long id) {
        return ApiResponse.ok(incidentService.detail(id));
    }
}
