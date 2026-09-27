package com.city.manhole.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 应急处置记录：街道值班员派单、到场、临时围护（警示锥/临时盖板/夜间反光标识）。
 * 只考核现场风险与到场时效，与权属追责线分离。
 */
@Data
@Entity
@Table(name = "emergency_measure")
public class EmergencyMeasure {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long incidentId;

    /** 处置的街道值班员 */
    private Long officerId;

    /** 派单时间 */
    private LocalDateTime dispatchedAt;

    /** 到场时间 */
    private LocalDateTime arrivedAt;

    /** 完成围护时间 */
    private LocalDateTime securedAt;

    /** 采取的措施，逗号分隔：警示锥,临时盖板,夜间反光标识 */
    @Column(length = 256)
    private String measures;

    /** 应急处置费用（街道先行垫付） */
    @Column(precision = 12, scale = 2)
    private BigDecimal cost = BigDecimal.ZERO;

    @Column(columnDefinition = "TEXT")
    private String note;

    private LocalDateTime createdAt = LocalDateTime.now();
}
