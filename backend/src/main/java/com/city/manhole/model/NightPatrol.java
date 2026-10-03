package com.city.manhole.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 夜间临时围护巡查记录：围护完成后街道值班员/巡查员夜间到场复核警示锥、反光标识状态。
 * 用于串起「围护完成 → 巡查到场 → 警示锥移位」的时间链，判定巡查间隔是否过长。
 */
@Data
@Entity
@Table(name = "night_patrol")
public class NightPatrol {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long incidentId;

    /** 巡查人（街道值班员/城管巡查员） */
    @Column(nullable = false)
    private Long officerId;

    /** 班次/轮次，如 22:00 第一轮 */
    @Column(length = 64)
    private String roundName;

    /** 巡查到场时间 */
    @Column(nullable = false)
    private LocalDateTime arrivedAt;

    /** 巡查时围护是否完好 */
    @Column(nullable = false)
    private Boolean intact = true;

    /** 现场状态说明：警示锥数量、反光标识、照明可见度等 */
    @Column(columnDefinition = "TEXT")
    private String siteStatus;

    /** 巡查照片 URL 的 JSON 数组 */
    @Column(columnDefinition = "TEXT")
    private String photos;

    /** 发现警示锥移位/倒伏的时间（巡查到场时已失效或到场后不久被撞） */
    private LocalDateTime coneMovedAt;

    /** 备注 */
    @Column(columnDefinition = "TEXT")
    private String note;

    private LocalDateTime createdAt = LocalDateTime.now();
}
