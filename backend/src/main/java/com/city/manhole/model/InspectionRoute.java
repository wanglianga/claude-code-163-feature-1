package com.city.manhole.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 日常巡检路线：依据高发路段统计动态调整。
 */
@Data
@Entity
@Table(name = "inspection_route")
public class InspectionRoute {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 128)
    private String name;

    /** 覆盖路段，逗号分隔 */
    @Column(columnDefinition = "TEXT")
    private String sections;

    /** 巡检频次：每日/隔日/每周 */
    @Column(length = 16)
    private String frequency;

    /** 调整依据/备注 */
    @Column(columnDefinition = "TEXT")
    private String note;

    private LocalDateTime updatedAt = LocalDateTime.now();
}
