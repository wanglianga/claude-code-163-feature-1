package com.city.manhole.controller;

import com.city.manhole.common.ApiResponse;
import com.city.manhole.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {
    private final StatsService statsService;

    @GetMapping("/overview")
    public ApiResponse<Map<String, Object>> overview() {
        return ApiResponse.ok(statsService.overview());
    }

    @GetMapping("/hotspots")
    public ApiResponse<List<Map<String, Object>>> hotspots() {
        return ApiResponse.ok(statsService.hotspots());
    }

    @GetMapping("/disputes")
    public ApiResponse<Map<String, Object>> disputes() {
        return ApiResponse.ok(statsService.disputes());
    }

    @GetMapping("/repair-timeouts")
    public ApiResponse<Map<String, Object>> repairTimeouts() {
        return ApiResponse.ok(statsService.repairTimeouts());
    }

    @GetMapping("/compensation")
    public ApiResponse<Map<String, Object>> compensation() {
        return ApiResponse.ok(statsService.compensation());
    }

    @GetMapping("/emergency")
    public ApiResponse<List<Map<String, Object>>> emergency() {
        return ApiResponse.ok(statsService.emergency());
    }
}
