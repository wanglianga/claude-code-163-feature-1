package com.city.manhole.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/** 权属认领记录（主动认领 / 排查指定） */
@Data
@Entity
@Table(name = "owner_claim")
public class OwnerClaim {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long incidentId;

    @Column(nullable = false)
    private Long ownerId;

    /** 主动认领 / 排查指定 */
    @Column(nullable = false, length = 16)
    private String claimType;

    @Column(columnDefinition = "TEXT")
    private String note;

    private LocalDateTime createdAt = LocalDateTime.now();
}
