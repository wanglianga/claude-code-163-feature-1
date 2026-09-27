package com.city.manhole.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 维修工单：权属单位认领后派给维修队。
 */
@Data
@Entity
@Table(name = "repair_order")
public class RepairOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long incidentId;

    /** 派单的权属单位 */
    @Column(nullable = false)
    private Long ownerId;

    /** 承接的维修队 */
    @Column(nullable = false)
    private Long teamId;

    private LocalDateTime assignedAt = LocalDateTime.now();

    /** 维修队接单时间 */
    private LocalDateTime acceptedAt;

    /** 到场时间 */
    private LocalDateTime arrivedAt;

    /** 井室状况 */
    @Column(length = 256)
    private String chamberCondition;

    /** 井盖规格 */
    @Column(length = 128)
    private String coverSpec;

    /** 是否需要封路 */
    private Boolean needRoadClosure = false;

    /** 是否涉及燃气/电力/通信等专业单位协同 */
    private Boolean needCoord = false;

    /** 协同单位，逗号分隔：燃气,电力,通信 */
    @Column(length = 128)
    private String coordUnits;

    @Column(columnDefinition = "TEXT")
    private String repairNote;

    /** 维修费用 */
    @Column(precision = 12, scale = 2)
    private BigDecimal repairCost = BigDecimal.ZERO;

    private LocalDateTime finishedAt;

    /** 见 Constants.RepairStatus */
    @Column(nullable = false, length = 16)
    private String status;
}
