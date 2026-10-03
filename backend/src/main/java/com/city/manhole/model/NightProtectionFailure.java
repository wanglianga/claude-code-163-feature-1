package com.city.manhole.model;

import jakarta.persistence.*;
import lombok.Data;

import com.city.manhole.common.Constants;

import java.time.LocalDateTime;

/**
 * 夜间临时围护失效与二次事故记录。
 * 串联：首次上报 → 围护照片 → 巡查到场 → 警示锥移位时间 → 二次事故，
 * 认定失效原因（围护方式不足 / 巡查间隔过长 / 外力破坏），
 * 并记录加派措施（反光围挡 / 临时封道 / 专人值守）。
 * 发生在学校门口或公交站附近时，补充人流高峰、照明条件、周边监控编号，
 * 由值班员选择是否安排绕行提示或交警协助。
 */
@Data
@Entity
@Table(name = "night_protection_failure")
public class NightProtectionFailure {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long incidentId;

    /** 登记人（街道值班员） */
    private Long officerId;

    /** 现场巡查到场人（可为值班员本人） */
    private Long patrolId;

    // ---- 串联节点 ----

    /** 首次上报时间（取自事件，冗余便于串联展示） */
    private LocalDateTime firstReportAt;

    /** 围护照片 URL 的 JSON 数组（原围护状态取证） */
    @Column(columnDefinition = "TEXT")
    private String protectionPhotos;

    /** 巡查到场核查时间 */
    private LocalDateTime patrolArrivedAt;

    /** 警示锥（围护）移位/被撞离时间 */
    private LocalDateTime displacedAt;

    /** 二次事故发生时间 */
    private LocalDateTime secondAccidentAt;

    /** 二次事故描述（第二名骑车人摔伤等） */
    @Column(columnDefinition = "TEXT")
    private String secondAccidentDesc;

    /** 二次事故是否造成人车损伤 */
    @Column(nullable = false)
    private Boolean hasInjury = false;

    /** 二次事故伤者/损失说明 */
    @Column(columnDefinition = "TEXT")
    private String injuryDesc;

    // ---- 原因认定与处置 ----

    /** 处置状态，见 Constants.NightFailureStatus */
    @Column(nullable = false, length = 16)
    private String status;

    /** 失效原因认定，见 Constants.NightFailureCause */
    @Column(length = 32)
    private String cause;

    /** 原因认定说明（结合围护方式、巡查间隔、外力痕迹） */
    @Column(columnDefinition = "TEXT")
    private String causeAnalysis;

    /** 加派措施，逗号分隔，见 Constants.NightDecision */
    @Column(length = 128)
    private String decisions;

    /** 措施落实时间 */
    private LocalDateTime handledAt;

    /** 处置人 */
    private Long handledBy;

    /** 处置备注 */
    @Column(columnDefinition = "TEXT")
    private String handleNote;

    // ---- 学校门口 / 公交站敏感信息 ----

    /** 是否学校门口或公交站附近，见 Constants.SensitiveLocation */
    @Column(nullable = false, length = 16)
    private String sensitiveLocation = Constants.SensitiveLocation.NONE;

    /** 人流高峰时段，如 07:00-08:00、16:30-18:00 */
    @Column(length = 128)
    private String peakHours;

    /** 照明条件：充足/一般/昏暗/无照明 */
    @Column(length = 16)
    private String lighting;

    /** 周边监控编号，逗号或顿号分隔 */
    @Column(length = 256)
    private String cameraIds;

    /** 是否安排绕行提示 */
    @Column(nullable = false)
    private Boolean detourNotice = false;

    /** 绕行提示落实说明 */
    @Column(columnDefinition = "TEXT")
    private String detourNote;

    /** 是否请求交警协助 */
    @Column(nullable = false)
    private Boolean trafficPoliceAssist = false;

    /** 交警协助说明（出警人员/到场时间等） */
    @Column(columnDefinition = "TEXT")
    private String trafficPoliceNote;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();
}
