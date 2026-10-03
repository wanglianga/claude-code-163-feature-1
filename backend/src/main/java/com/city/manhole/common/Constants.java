package com.city.manhole.common;

/** 平台全部状态与角色常量 */
public final class Constants {
    private Constants() {}

    /** 角色 */
    public static final class Role {
        public static final String CITIZEN = "CITIZEN";           // 市民
        public static final String STREET_DUTY = "STREET_DUTY";   // 街道值班员
        public static final String OWNER_UNIT = "OWNER_UNIT";     // 权属单位
        public static final String REPAIR_TEAM = "REPAIR_TEAM";   // 维修队
        public static final String INSPECTOR = "INSPECTOR";       // 城管巡查员
        public static final String COMPENSATOR = "COMPENSATOR";   // 赔付专员
        public static final String ADMIN = "ADMIN";               // 城管管理员
    }

    /** 事件主流程状态 */
    public static final class IncidentStatus {
        public static final String REPORTED = "REPORTED";               // 已上报，待应急处置
        public static final String SECURED = "SECURED";                 // 已临时围护，待权属确认
        public static final String OWNER_CONFIRMED = "OWNER_CONFIRMED"; // 权属已确认，待派单维修
        public static final String REPAIRING = "REPAIRING";             // 维修中
        public static final String REPAIRED = "REPAIRED";               // 维修完成，待验收
        public static final String ACCEPTED = "ACCEPTED";               // 验收通过，待费用复核
        public static final String CLOSED = "CLOSED";                   // 已结案
    }

    /** 应急处置线状态（只看现场风险与到场时效） */
    public static final class EmergencyStatus {
        public static final String PENDING = "PENDING";       // 待派单
        public static final String DISPATCHED = "DISPATCHED"; // 已派单，待到场
        public static final String SECURED = "SECURED";       // 已完成临时围护
    }

    /** 权属追责线状态 */
    public static final class OwnerStatus {
        public static final String UNCONFIRMED = "UNCONFIRMED"; // 未确认
        public static final String INVESTIGATING = "INVESTIGATING"; // 排查中
        public static final String CONFIRMED = "CONFIRMED";     // 已确认/认领
        public static final String PENDING = "PENDING";         // 责任待定（街道先行处置）
    }

    /** 维修工单状态 */
    public static final class RepairStatus {
        public static final String ASSIGNED = "ASSIGNED";   // 已派发，待接单
        public static final String ACCEPTED = "ACCEPTED";   // 已接单，维修中
        public static final String FINISHED = "FINISHED";   // 维修完成，待验收
        public static final String RETURNED = "RETURNED";   // 验收不通过，退回重修
    }

    /** 赔付案件状态 */
    public static final class CompStatus {
        public static final String FILED = "FILED";         // 已立案，待审核
        public static final String APPROVED = "APPROVED";   // 已核定，待赔付
        public static final String REJECTED = "REJECTED";   // 已驳回
        public static final String PAID = "PAID";           // 已赔付
    }

    /** 费用分摊状态 */
    public static final class CostStatus {
        public static final String PENDING = "PENDING";       // 待复核分摊
        public static final String ALLOCATED = "ALLOCATED";   // 已分摊结案
    }

    /** 夜间临时围护失效处置状态 */
    public static final class NightFailureStatus {
        public static final String REPORTED = "REPORTED";   // 已登记失效/二次事故，待巡查到场核查
        public static final String VERIFIED = "VERIFIED";   // 巡查已到场核查，待原因认定
        public static final String ASSESSED = "ASSESSED";   // 已认定失效原因，待处置
        public static final String HANDLED = "HANDLED";     // 已加派措施处置完毕
    }

    /** 围护失效原因认定 */
    public static final class NightFailureCause {
        public static final String INADEQUATE_PROTECTION = "INADEQUATE_PROTECTION"; // 围护方式不足
        public static final String PATROL_GAP = "PATROL_GAP";                       // 巡查间隔过长
        public static final String EXTERNAL_DAMAGE = "EXTERNAL_DAMAGE";             // 外力破坏
    }

    /** 失效后加派措施 */
    public static final class NightDecision {
        public static final String REFLECTIVE_BARRIER = "REFLECTIVE_BARRIER"; // 加派反光围挡
        public static final String ROAD_CLOSURE = "ROAD_CLOSURE";             // 临时封道
        public static final String GUARD_DUTY = "GUARD_DUTY";                 // 安排专人值守
    }

    /** 二次事故敏感场所 */
    public static final class SensitiveLocation {
        public static final String NONE = "NONE";                 // 非敏感场所
        public static final String SCHOOL_GATE = "SCHOOL_GATE";   // 学校门口
        public static final String BUS_STOP = "BUS_STOP";         // 公交站附近
    }

    /** 赔付责任认定 */
    public static final class Responsibility {
        public static final String OWNER = "OWNER";             // 权属单位全责
        public static final String STREET = "STREET";           // 街道（围护不及时）责任
        public static final String SHARED = "SHARED";           // 多方分摊
        public static final String UNDETERMINED = "UNDETERMINED"; // 无法认定
    }
}
