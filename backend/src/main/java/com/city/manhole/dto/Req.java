package com.city.manhole.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** 全部请求体定义 */
public final class Req {
    private Req() {}

    public record LoginReq(String username, String password) {}
    public record RegisterReq(String username, String password, String name, String phone) {}

    public record IncidentCreateReq(String issueType, Double longitude, Double latitude,
                                    String address, String roadType, List<String> photos,
                                    Boolean hasDamage, String damageDesc, String dangerLevel) {}

    public record SecureReq(List<String> measures, BigDecimal cost, String note) {}

    /** 登记夜间围护失效与二次事故 */
    public record NightFailureRegisterReq(LocalDateTime displacedAt, LocalDateTime secondAccidentAt,
                                          String secondAccidentDesc, Boolean hasInjury, String injuryDesc,
                                          List<String> protectionPhotos, String sensitiveLocation,
                                          String peakHours, String lighting, String cameraIds,
                                          Boolean detourNotice, String detourNote,
                                          Boolean trafficPoliceAssist, String trafficPoliceNote,
                                          LocalDateTime patrolArrivedAt) {}

    /** 巡查到场核查（补充围护照片） */
    public record NightPatrolReq(List<String> protectionPhotos, String note) {}

    /** 认定失效原因 */
    public record NightAssessReq(String cause, String causeAnalysis) {}

    /** 加派措施处置 */
    public record NightHandleReq(List<String> decisions, String handleNote,
                                 Boolean detourNotice, String detourNote,
                                 Boolean trafficPoliceAssist, String trafficPoliceNote,
                                 BigDecimal addCost) {}

    public record InvestigateReq(String manholeMarking, String pipelineInfo, String constructionHistory,
                                 String merchantInfo, String cityLedger, String conclusion,
                                 Long confirmedOwnerId, String note) {}

    public record ClaimReq(String note) {}

    public record RepairOrderReq(Long teamId) {}
    public record RepairFinishReq(String chamberCondition, String coverSpec, Boolean needRoadClosure,
                                  Boolean needCoord, String coordUnits, String repairNote,
                                  BigDecimal repairCost) {}

    public record AcceptanceReq(Boolean photoOk, Boolean locationOk, Boolean flatnessOk,
                                Boolean rollingTestOk, String result, String note) {}

    public record CompUpdateReq(String damageType, BigDecimal claimAmount, String accidentProof) {}
    public record CompDecideReq(String result, String responsibility, Integer ownerRatio,
                                Integer streetRatio, BigDecimal approvedAmount, String analysis) {}

    public record CostReviewReq(List<Map<String, Object>> allocations, String reviewNote) {}

    public record UserCreateReq(String username, String password, String name,
                                String role, String orgName, String phone) {}
    public record RouteReq(Long id, String name, String sections, String frequency, String note) {}
    public record RuleReq(Long id, String title, String content) {}
}
