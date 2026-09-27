package com.city.manhole.service;

import com.city.manhole.model.Notification;
import com.city.manhole.model.User;
import com.city.manhole.repo.NotificationRepo;
import com.city.manhole.repo.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepo notificationRepo;
    private final UserRepo userRepo;

    @Transactional
    public void notify(Long userId, Long incidentId, String title, String content) {
        Notification n = new Notification();
        n.setUserId(userId);
        n.setIncidentId(incidentId);
        n.setTitle(title);
        n.setContent(content);
        notificationRepo.save(n);
    }

    /** 按角色广播（如：通知全部街道值班员） */
    @Transactional
    public void notifyRole(String role, Long incidentId, String title, String content) {
        List<User> users = userRepo.findByRole(role);
        for (User u : users) {
            notify(u.getId(), incidentId, title, content);
        }
    }

    public List<Notification> mine(Long userId) {
        List<Notification> list = notificationRepo.findByUserIdOrderByCreatedAtDesc(userId);
        return list.size() > 50 ? list.subList(0, 50) : list;
    }

    public long unread(Long userId) {
        return notificationRepo.countByUserIdAndReadFlagFalse(userId);
    }

    @Transactional
    public void markRead(Long id, Long userId) {
        notificationRepo.findById(id).ifPresent(n -> {
            if (n.getUserId().equals(userId)) {
                n.setReadFlag(true);
                notificationRepo.save(n);
            }
        });
    }

    @Transactional
    public void markAllRead(Long userId) {
        List<Notification> list = notificationRepo.findByUserIdOrderByCreatedAtDesc(userId);
        list.forEach(n -> n.setReadFlag(true));
        notificationRepo.saveAll(list);
    }
}
