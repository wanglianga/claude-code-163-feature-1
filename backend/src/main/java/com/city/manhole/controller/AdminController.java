package com.city.manhole.controller;

import com.city.manhole.common.ApiResponse;
import com.city.manhole.dto.Req;
import com.city.manhole.model.HandoverRule;
import com.city.manhole.model.InspectionRoute;
import com.city.manhole.model.User;
import com.city.manhole.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
    private final AdminService adminService;

    @GetMapping("/users")
    public ApiResponse<List<User>> users() {
        return ApiResponse.ok(adminService.users());
    }

    @PostMapping("/users")
    public ApiResponse<User> createUser(@RequestBody Req.UserCreateReq req) {
        return ApiResponse.ok(adminService.createUser(req.username(), req.password(),
                req.name(), req.role(), req.orgName(), req.phone()));
    }

    @GetMapping("/routes")
    public ApiResponse<List<InspectionRoute>> routes() {
        return ApiResponse.ok(adminService.routes());
    }

    @PostMapping("/routes")
    public ApiResponse<InspectionRoute> saveRoute(@RequestBody Req.RouteReq req) {
        return ApiResponse.ok(adminService.saveRoute(req.id(), req.name(), req.sections(),
                req.frequency(), req.note()));
    }

    @DeleteMapping("/routes/{id}")
    public ApiResponse<Void> deleteRoute(@PathVariable Long id) {
        adminService.deleteRoute(id);
        return ApiResponse.ok();
    }

    @GetMapping("/rules")
    public ApiResponse<List<HandoverRule>> rules() {
        return ApiResponse.ok(adminService.rules());
    }

    @PostMapping("/rules")
    public ApiResponse<HandoverRule> saveRule(@RequestBody Req.RuleReq req) {
        return ApiResponse.ok(adminService.saveRule(req.id(), req.title(), req.content()));
    }

    @DeleteMapping("/rules/{id}")
    public ApiResponse<Void> deleteRule(@PathVariable Long id) {
        adminService.deleteRule(id);
        return ApiResponse.ok();
    }
}
