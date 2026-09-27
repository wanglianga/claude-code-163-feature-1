package com.city.manhole.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/** 站内通知 */
@Data
@Entity
@Table(name = "notification")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    private Long incidentId;

    @Column(nullable = false, length = 128)
    private String title;

    @Column(length = 512)
    private String content;

    @Column(nullable = false)
    private Boolean readFlag = false;

    private LocalDateTime createdAt = LocalDateTime.now();
}
