package com.city.manhole.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 夜间临时围护失效处理记录。
 *
 * 串起：首次上报 → 围护照片 → 夜间巡查到场 → 警示锥被撞移位时间 → 二次事故（骑车人摔伤），
 * 判定失效原因（围护方式不足 / 巡查间隔过长 / 外力破坏），并记录加固处置决策
 * （加派反光围挡 / 临时封道 / 专人值守）。
 * 二次事故发生在学校门口或公交站附近时，记录人流高峰、照明条件、周边监控编号，
 * 值班员选择是否临时安排绕行提示或请求交警协助。
 */
@Data
@Entity
@Table(name = "night_failure")
public class NightFailure {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 事件编号，如 NF20261002-0001 */
    @Column(nullable = false, unique = true, length = 32)
    private String code;

    @Column(nullable = false)
    private Long incidentId;

    /** 登记人（街道值班员） */
    @Column(nullable = false)
    private Long officerId;

    /** 最近一次夜间巡查到场时间（来自巡查记录或手动补录） */
    private LocalDateTime lastPatrolAt;

    /** 警示锥移位/被撞倒时间 */
    @Column(nullable = false)
    private LocalDateTime coneMovedAt;

    /** 发现人/来源：巡查发现、市民二次上报、交警转递等 */
    @Column(length = 64)
    private String reportedBy;

    /** 失效现场照片 URL 的 JSON 数组（移位的警示锥、损坏的反光标识等） */
    @Column(columnDefinition = "TEXT")
    private String photos;

    // ---------------- 二次事故 ----------------

    /** 是否发生二次事故 */
    @Column(nullable = false)
    private Boolean hasSecondAccident = false;

    /** 二次事故发生时间 */
    private LocalDateTime secondAccidentAt;

    /** 第二名受伤骑车人姓名 */
    @Column(length = 64)
    private String injuredName;

    /** 受伤骑车人联系方式 */
    @Column(length = 32)
    private String injuredPhone;

    /** 伤情描述 */
    @Column(columnDefinition = "TEXT")
    private String injuryDesc;

    /** 二次事故说明（车辆、经过、处置） */
    @Column(columnDefinition = "TEXT")
    private String accidentDesc;

    /** 二次事故现场照片 URL 的 JSON 数组 */
    @Column(columnDefinition = "TEXT")
    private String accidentPhotos;

    /** 二次事故生成的赔付案件 id（自动立案） */
    private Long compensationId;

    // ---------------- 原因判定 ----------------

    /** 失效原因，见 Constants.FailureCause */
    @Column(length = 32)
    private String cause;

    /** 判定依据/分析：围护方式、巡查间隔（分钟）、外力痕迹等 */
    @Column(columnDefinition = "TEXT")
    private String causeAnalysis;

    /** 失效距上次巡查到场的间隔（分钟），辅助判断巡查间隔是否过长 */
    private Long patrolGapMinutes;

    // ---------------- 加固处置决策 ----------------

    /** 决定采取的加固措施，逗号分隔，见 Constants.ReinforceMeasure */
    @Column(length = 128)
    private String reinforceMeasures;

    /** 处置决策时间 */
    private LocalDateTime decisionAt;

    /** 处置决策人 */
    private Long decisionOfficerId;

    /** 处置说明 */
    @Column(columnDefinition = "TEXT")
    private String decisionNote;

    /** 加固处置是否已落实 */
    @Column(nullable = false)
    private Boolean reinforced = false;

    /** 加固落实时间 */
    private LocalDateTime reinforcedAt;

    // ---------------- 学校门口/公交站敏感地点 ----------------

    /** 是否位于学校门口或公交站附近 */
    @Column(nullable = false)
    private Boolean sensitiveLocation = false;

    /** 地点类型：学校门口 / 公交站 */
    @Column(length = 32)
    private String locationType;

    /** 关联场所名称，如 第二实验小学（东门）/ 人民影院公交站 */
    @Column(length = 128)
    private String locationName;

    /** 人流高峰时段，如 07:00-08:00、16:30-18:00 */
    @Column(length = 128)
    private String peakHours;

    /** 照明条件：良好 / 一般 / 昏暗无路灯 */
    @Column(length = 32)
    private String lighting;

    /** 周边监控编号，逗号分隔，如 JY-0112,JY-0113 */
    @Column(length = 256)
    private String cameraIds;

    /** 是否临时安排绕行提示 */
    @Column(nullable = false)
    private Boolean detourNotice = false;

    /** 绕行提示落实说明（牌数、位置） */
    @Column(columnDefinition = "TEXT")
    private String detourNote;

    /** 是否请求交警协助 */
    @Column(nullable = false)
    private Boolean trafficPoliceAssist = false;

    /** 交警协助情况（来人时间、警力编号） */
    @Column(columnDefinition = "TEXT")
    private String trafficPoliceNote;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();
}
