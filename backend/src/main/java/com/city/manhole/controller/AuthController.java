package com.city.manhole.controller;

import com.city.manhole.common.ApiResponse;
import com.city.manhole.common.AuthContext;
import com.city.manhole.dto.Req;
import com.city.manhole.model.User;
import com.city.manhole.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public ApiResponse<User> register(@RequestBody Req.RegisterReq req) {
        return ApiResponse.ok(authService.register(req.username(), req.password(), req.name(), req.phone()));
    }

    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@RequestBody Req.LoginReq req) {
        return ApiResponse.ok(authService.login(req.username(), req.password()));
    }

    @GetMapping("/me")
    public ApiResponse<User> me() {
        User u = AuthContext.requireLogin();
        u.setPassword(null);
        return ApiResponse.ok(u);
    }
}
