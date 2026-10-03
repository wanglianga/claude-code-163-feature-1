package com.city.manhole.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 赔付案件：市民或车辆受损时，赔付专员依据上报时间、临时围护、权属确认、维修时长、事故证明定责。
 */
@Data
@Entity
@Table(name = "compensation_case")
public class CompensationCase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 案件类型：PRIMARY 首次上报时损伤 / SECOND 夜间围护失效二次事故 */
    @Column(nullable = false, length = 16)
    private String caseType = "PRIMARY";

    /** SECOND 类型案件关联的夜间围护失效记录 id */
    private Long nightFailureId;

    @Column(nullable = false)
    private Long incidentId;

    /** 经办的赔付专员 */
    private Long specialistId;

    @Column(length = 64)
    private String claimantName;

    @Column(length = 32)
    private String claimantPhone;

    /** 损失类型：人身伤害/车辆损失/财产损失 */
    @Column(length = 32)
    private String damageType;

    /** 索赔金额 */
    @Column(precision = 12, scale = 2)
    private BigDecimal claimAmount = BigDecimal.ZERO;

    /** 事故证明（材料说明） */
    @Column(columnDefinition = "TEXT")
    private String accidentProof;

    /** 责任分析（上报时间、围护时效、权属确认、维修时长、事故证明综合判断） */
    @Column(columnDefinition = "TEXT")
    private String analysis;

    /** 责任认定，见 Constants.Responsibility */
    @Column(length = 32)
    private String responsibility;

    /** 权属单位承担比例 % */
    private Integer ownerRatio = 0;

    /** 街道承担比例 % */
    private Integer streetRatio = 0;

    /** 核定赔付金额 */
    @Column(precision = 12, scale = 2)
    private BigDecimal approvedAmount = BigDecimal.ZERO;

    /** 见 Constants.CompStatus */
    @Column(nullable = false, length = 16)
    private String status;

    private LocalDateTime decidedAt;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt = LocalDateTime.now();
}
