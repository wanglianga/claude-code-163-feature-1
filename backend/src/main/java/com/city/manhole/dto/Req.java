package com.city.manhole.dto;

import java.math.BigDecimal;
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

    /** 夜间巡查到场记录 */
    public record NightPatrolReq(String roundName, java.time.LocalDateTime arrivedAt, Boolean intact,
                                 String siteStatus, List<String> photos,
                                 java.time.LocalDateTime coneMovedAt, String note) {}

    /** 夜间围护失效 + 二次事故登记 */
    public record NightFailureReq(
            java.time.LocalDateTime lastPatrolAt,
            java.time.LocalDateTime coneMovedAt,
            String reportedBy,
            List<String> photos,
            Boolean hasSecondAccident,
            java.time.LocalDateTime secondAccidentAt,
            String injuredName,
            String injuredPhone,
            String injuryDesc,
            String accidentDesc,
            List<String> accidentPhotos,
            String cause,
            String causeAnalysis,
            List<String> reinforceMeasures,
            String decisionNote,
            Boolean sensitiveLocation,
            String locationType,
            String locationName,
            String peakHours,
            String lighting,
            String cameraIds,
            Boolean detourNotice,
            String detourNote,
            Boolean trafficPoliceAssist,
            String trafficPoliceNote) {}

    /** 仅原因判定与加固决策 */
    public record NightFailureDecisionReq(String cause, String causeAnalysis,
                                          List<String> reinforceMeasures, String decisionNote,
                                          Boolean detourNotice, String detourNote,
                                          Boolean trafficPoliceAssist, String trafficPoliceNote) {}

    /** 加固措施落实确认 */
    public record NightFailureReinforceReq(String note) {}

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
