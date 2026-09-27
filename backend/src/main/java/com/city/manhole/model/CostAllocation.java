package com.city.manhole.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 费用分摊：应急处置费 + 维修费 + 赔付金，验收通过后汇总，复核后分摊到权属单位/街道。
 * 街道先行垫付的费用在复核后按结果分摊。
 */
@Data
@Entity
@Table(name = "cost_allocation")
public class CostAllocation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long incidentId;

    @Column(precision = 12, scale = 2)
    private BigDecimal emergencyCost = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    private BigDecimal repairCost = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    private BigDecimal compensationAmount = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    /** 分摊明细 JSON：[{"party":"权属单位-城市燃气集团","amount":1200,"reason":"权属确认后承担全部"}] */
    @Column(columnDefinition = "TEXT")
    private String allocations;

    @Column(columnDefinition = "TEXT")
    private String reviewNote;

    /** 见 Constants.CostStatus */
    @Column(nullable = false, length = 16)
    private String status;

    private Long reviewedBy;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt = LocalDateTime.now();
}
