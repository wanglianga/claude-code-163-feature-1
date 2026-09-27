package com.city.manhole.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 权属排查记录：依据井盖标识、地下管线、历史施工、周边商户、城管台账五类证据排查权属单位。
 */
@Data
@Entity
@Table(name = "ownership_investigation")
public class OwnershipInvestigation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long incidentId;

    /** 排查人（街道值班员） */
    @Column(nullable = false)
    private Long investigatorId;

    /** 井盖标识（铸字、编号、权属标记） */
    @Column(columnDefinition = "TEXT")
    private String manholeMarking;

    /** 地下管线资料 */
    @Column(columnDefinition = "TEXT")
    private String pipelineInfo;

    /** 历史施工记录 */
    @Column(columnDefinition = "TEXT")
    private String constructionHistory;

    /** 周边商户走访 */
    @Column(columnDefinition = "TEXT")
    private String merchantInfo;

    /** 城管台账 */
    @Column(columnDefinition = "TEXT")
    private String cityLedger;

    /** 结论：CONFIRMED 确认权属 / PENDING 无法确认（责任待定） */
    @Column(nullable = false, length = 16)
    private String conclusion;

    /** 确认的权属单位用户 id（结论为 CONFIRMED 时必填） */
    private Long confirmedOwnerId;

    @Column(columnDefinition = "TEXT")
    private String note;

    private LocalDateTime createdAt = LocalDateTime.now();
}
