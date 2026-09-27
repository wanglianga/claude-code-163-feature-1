package com.city.manhole.controller;

import com.city.manhole.common.ApiResponse;
import com.city.manhole.common.AuthContext;
import com.city.manhole.model.Notification;
import com.city.manhole.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;

    @GetMapping
    public ApiResponse<Map<String, Object>> mine() {
        Long uid = AuthContext.requireLogin().getId();
        List<Notification> list = notificationService.mine(uid);
        Map<String, Object> m = new HashMap<>();
        m.put("list", list);
        m.put("unread", notificationService.unread(uid));
        return ApiResponse.ok(m);
    }

    @PostMapping("/{id}/read")
    public ApiResponse<Void> read(@PathVariable Long id) {
        notificationService.markRead(id, AuthContext.requireLogin().getId());
        return ApiResponse.ok();
    }

    @PostMapping("/read-all")
    public ApiResponse<Void> readAll() {
        notificationService.markAllRead(AuthContext.requireLogin().getId());
        return ApiResponse.ok();
    }
}
