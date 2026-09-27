package com.city.manhole.controller;

import com.city.manhole.common.ApiResponse;
import com.city.manhole.service.MetaService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/meta")
@RequiredArgsConstructor
public class MetaController {
    private final MetaService metaService;

    @GetMapping
    public ApiResponse<Map<String, Object>> meta() {
        return ApiResponse.ok(metaService.meta());
    }
}
