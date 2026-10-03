package com.city.manhole.config;

import com.city.manhole.common.Constants;
import com.city.manhole.model.*;
import com.city.manhole.repo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 首次启动时初始化演示数据：各角色账号 + 覆盖全流程各状态的事件。
 */
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {
    private final UserRepo userRepo;
    private final IncidentRepo incidentRepo;
    private final EmergencyMeasureRepo emergencyRepo;
    private final InvestigationRepo investigationRepo;
    private final OwnerClaimRepo claimRepo;
    private final RepairOrderRepo repairOrderRepo;
    private final AcceptanceRepo acceptanceRepo;
    private final CompensationRepo compensationRepo;
    private final CostAllocationRepo costRepo;
    private final InspectionRouteRepo routeRepo;
    private final HandoverRuleRepo ruleRepo;
    private final NightPatrolRepo nightPatrolRepo;
    private final NightFailureRepo nightFailureRepo;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    private User admin, street, citizen, citizen2, gas, power, telecom, municipal, repair1, repair2, inspector, comp;

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepo.count() > 0) return;
        seedUsers();
        seedIncidents();
        seedRoutesAndRules();
        System.out.println("===== 演示数据初始化完成 =====");
    }

    private void seedUsers() {
        admin = user("admin", "admin123", "城管管理员", Constants.Role.ADMIN, "城市管理局", "0311-12345");
        street = user("street", "street123", "值班员-王强", Constants.Role.STREET_DUTY, "朝阳街道值班室", "13800000001");
        citizen = user("citizen", "citizen123", "市民张三", Constants.Role.CITIZEN, "市民", "13800000002");
        citizen2 = user("citizen2", "citizen123", "市民李四", Constants.Role.CITIZEN, "市民", "13800000003");
        gas = user("gas", "owner123", "燃气权属专员", Constants.Role.OWNER_UNIT, "城市燃气集团", "13800000004");
        power = user("power", "owner123", "电力权属专员", Constants.Role.OWNER_UNIT, "国网电力公司", "13800000005");
        telecom = user("telecom", "owner123", "通信权属专员", Constants.Role.OWNER_UNIT, "联合通信公司", "13800000006");
        municipal = user("municipal", "owner123", "市政权属专员", Constants.Role.OWNER_UNIT, "市政工程公司", "13800000007");
        repair1 = user("repair1", "repair123", "维修一队长", Constants.Role.REPAIR_TEAM, "市政维修一队", "13800000008");
        repair2 = user("repair2", "repair123", "维修二队长", Constants.Role.REPAIR_TEAM, "燃气专业维修队", "13800000009");
        inspector = user("inspector", "inspect123", "巡查员-赵敏", Constants.Role.INSPECTOR, "城管执法局巡查队", "13800000010");
        comp = user("comp", "comp123", "赔付专员-钱芳", Constants.Role.COMPENSATOR, "保险理赔中心", "13800000011");
    }

    private User user(String username, String password, String name, String role, String org, String phone) {
        User u = new User();
        u.setUsername(username);
        u.setPassword(encoder.encode(password));
        u.setName(name);
        u.setRole(role);
        u.setOrgName(org);
        u.setPhone(phone);
        return userRepo.save(u);
    }

    // ------------------------------------------------------------------

    private void seedIncidents() {
        // 1. 全流程结案：燃气井盖缺失，车辆受损，赔付已支付，费用已分摊
        Incident i1 = base("MH20260708-0001", citizen, "缺失", "人民路与建设大街交口", "主干道", "高",
                true, "私家车前轮陷入井口，轮毂变形", 80);
        i1.setStatus(Constants.IncidentStatus.CLOSED);
        i1.setEmergencyStatus(Constants.EmergencyStatus.SECURED);
        i1.setOwnerStatus(Constants.OwnerStatus.CONFIRMED);
        i1.setOwnerId(gas.getId());
        i1.setClosedAt(i1.getCreatedAt().plusDays(6));
        incidentRepo.save(i1);
        emergency(i1, 15, 70, 100, "警示锥,临时盖板,夜间反光标识", "800");
        investigation(i1, "井圈铸有\"燃\"字及编号RQ-2209", "燃气中压管线从此经过", "2024年燃气改造施工记录",
                "商户反映上周燃气公司巡检", "台账登记为燃气集团设施", "CONFIRMED", gas, 79);
        claim(i1, gas, "排查指定", 79);
        RepairOrder o1 = order(i1, gas, repair2, 79, 30, "井室完好，井壁无渗漏", "Φ700 重型球墨铸铁井盖",
                false, false, null, "更换新井盖并加固井圈", "4500");
        acceptance(i1, true, true, true, true, "PASSED", "修复质量合格", 77);
        compensation(i1, "车辆损失", "12000", "交警事故认定书、维修发票",
                "上报后 70 分钟完成围护，权属 1 日内确认，维修 30 小时完成；事故发生于围护前，权属单位承担全责。",
                Constants.Responsibility.OWNER, 100, 0, "8600", Constants.CompStatus.PAID, 78);
        cost(i1, "800", "4500", "8600", Constants.CostStatus.ALLOCATED,
                "[{\"party\":\"权属单位-城市燃气集团\",\"amount\":13900,\"reason\":\"权属确认，承担应急、维修及赔付全部费用\"}]",
                "权属清晰，全部由燃气集团承担，街道垫付应急费予以返还。", 76);

        // 2. 验收通过待费用复核：市政井盖松动
        Incident i2 = base("MH20260906-0001", citizen2, "松动", "和平路商业街", "次干道", "中",
                false, null, 20);
        i2.setStatus(Constants.IncidentStatus.ACCEPTED);
        i2.setEmergencyStatus(Constants.EmergencyStatus.SECURED);
        i2.setOwnerStatus(Constants.OwnerStatus.CONFIRMED);
        i2.setOwnerId(municipal.getId());
        incidentRepo.save(i2);
        emergency(i2, 20, 150, 180, "警示锥,临时盖板", "300");
        investigation(i2, "井盖铸字\"市政\"", "雨水管线", "无近期施工", "商户未注意", "台账属市政公司",
                "CONFIRMED", municipal, 19);
        claim(i2, municipal, "排查指定", 19);
        order(i2, municipal, repair1, 19, 20, "井室轻微沉降", "Φ600 普通铸铁井盖",
                false, false, null, "更换密封胶条，复平井盖", "2800");
        acceptance(i2, true, true, true, true, "PASSED", "平整度合格", 18);
        cost(i2, "300", "2800", "0", Constants.CostStatus.PENDING,
                "[{\"party\":\"权属单位-市政工程公司\",\"amount\":3100,\"reason\":\"权属已确认，由权属单位承担全部费用\"}]",
                null, 0);

        // 3. 维修完成待验收：电力井盖塌陷
        Incident i3 = base("MH20260920-0001", citizen, "塌陷", "解放大道辅路", "主干道", "高",
                false, null, 6);
        i3.setStatus(Constants.IncidentStatus.REPAIRED);
        i3.setEmergencyStatus(Constants.EmergencyStatus.SECURED);
        i3.setOwnerStatus(Constants.OwnerStatus.CONFIRMED);
        i3.setOwnerId(power.getId());
        incidentRepo.save(i3);
        emergency(i3, 10, 90, 120, "警示锥,临时盖板,夜间反光标识", "600");
        investigation(i3, "井盖标有\"电\"字", "电缆沟并行", "上月电力增容施工", "商户看到电力作业车", "台账属电力公司",
                "CONFIRMED", power, 5);
        claim(i3, power, "排查指定", 5);
        order(i3, power, repair1, 5, 26, "井室坍塌需重砌", "Φ800 重型防盗井盖",
                true, true, "电力", "重砌井室、更换井盖，已恢复路面", "6000");

        // 4. 维修中：通信井盖异响
        Incident i4 = base("MH20260922-0001", citizen2, "异响", "文化巷", "街巷", "低",
                false, null, 4);
        i4.setStatus(Constants.IncidentStatus.REPAIRING);
        i4.setEmergencyStatus(Constants.EmergencyStatus.SECURED);
        i4.setOwnerStatus(Constants.OwnerStatus.CONFIRMED);
        i4.setOwnerId(telecom.getId());
        incidentRepo.save(i4);
        emergency(i4, 30, 300, 330, "警示锥", "100");
        investigation(i4, "井盖铸\"通信\"", "通信光缆井", "无", "商户反映夜间异响明显", "台账属通信公司",
                "CONFIRMED", telecom, 3);
        claim(i4, telecom, "排查指定", 3);
        RepairOrder o4 = new RepairOrder();
        o4.setIncidentId(i4.getId());
        o4.setOwnerId(telecom.getId());
        o4.setTeamId(repair1.getId());
        o4.setAssignedAt(i4.getCreatedAt().plusDays(1));
        o4.setAcceptedAt(i4.getCreatedAt().plusDays(1).plusHours(3));
        o4.setStatus(Constants.RepairStatus.ACCEPTED);
        repairOrderRepo.save(o4);

        // 5. 责任待定：滨河路井盖缺失，行人摔伤，街道先行处置垫付
        Incident i5 = base("MH20260923-0001", citizen, "缺失", "滨河路", "次干道", "高",
                true, "行人夜间跌入井口，腿部骨折送医", 3);
        i5.setStatus(Constants.IncidentStatus.SECURED);
        i5.setEmergencyStatus(Constants.EmergencyStatus.SECURED);
        i5.setOwnerStatus(Constants.OwnerStatus.PENDING);
        i5.setResponsibilityPending(true);
        i5.setEverPending(true);
        incidentRepo.save(i5);
        emergency(i5, 8, 55, 80, "警示锥,临时盖板,夜间反光标识", "900");
        investigation(i5, "井口无盖，井圈标识磨损无法辨认", "管线图显示燃气与通信管线交汇",
                "近三年无施工登记", "商户称井盖已缺失两周", "台账无该井登记",
                "PENDING", null, 2);
        CompensationCase c5 = new CompensationCase();
        c5.setIncidentId(i5.getId());
        c5.setClaimantName("市民张三");
        c5.setClaimantPhone("13800000002");
        c5.setDamageType("人身伤害");
        c5.setClaimAmount(new BigDecimal("50000"));
        c5.setAccidentProof("医院诊断证明、急诊病历、现场照片");
        c5.setStatus(Constants.CompStatus.FILED);
        c5.setCreatedAt(i5.getCreatedAt().plusHours(2));
        compensationRepo.save(c5);

        // 6. 应急完成排查中：中山路井盖松动
        Incident i6 = base("MH20260924-0001", citizen2, "松动", "中山路", "主干道", "中",
                false, null, 2);
        i6.setStatus(Constants.IncidentStatus.SECURED);
        i6.setEmergencyStatus(Constants.EmergencyStatus.SECURED);
        i6.setOwnerStatus(Constants.OwnerStatus.INVESTIGATING);
        incidentRepo.save(i6);
        emergency(i6, 12, 100, 130, "警示锥,临时盖板", "350");

        // 7. 刚上报待应急：胜利街井盖塌陷
        Incident i7 = base("MH20260926-0001", citizen, "塌陷", "胜利街", "支路", "高",
                false, null, 0);
        i7.setStatus(Constants.IncidentStatus.REPORTED);
        i7.setEmergencyStatus(Constants.EmergencyStatus.PENDING);
        i7.setOwnerStatus(Constants.OwnerStatus.UNCONFIRMED);
        i7.setCreatedAt(LocalDateTime.now().minusHours(5));
        i7.setUpdatedAt(i7.getCreatedAt());
        incidentRepo.save(i7);

        // 8. 曾责任待定后被通信认领并结案（权属争议样例）
        Incident i8 = base("MH20260728-0001", citizen2, "缺失", "建设大街北段", "主干道", "中",
                true, "电动车避让不及侧翻，骑手擦伤", 60);
        i8.setStatus(Constants.IncidentStatus.CLOSED);
        i8.setEmergencyStatus(Constants.EmergencyStatus.SECURED);
        i8.setOwnerStatus(Constants.OwnerStatus.CONFIRMED);
        i8.setOwnerId(telecom.getId());
        i8.setEverPending(true);
        i8.setClosedAt(i8.getCreatedAt().plusDays(9));
        incidentRepo.save(i8);
        emergency(i8, 25, 130, 160, "警示锥,临时盖板", "450");
        investigation(i8, "井圈无标识", "通信与雨水管线交汇", "移交记录缺失", "商户说法不一", "台账未登记",
                "PENDING", null, 59);
        investigation(i8, "复核井室内部发现通信光缆标识牌", "确认为通信管井", "2019 年通信扩容施工",
                "老商户确认为通信井", "补充台账", "CONFIRMED", telecom, 55);
        claim(i8, telecom, "主动认领", 56);
        order(i8, telecom, repair1, 55, 40, "井室完好", "Φ650 复合井盖",
                false, true, "通信", "更换井盖", "2200");
        acceptance(i8, true, true, true, true, "PASSED", "合格", 52);
        compensation(i8, "人身伤害", "6000", "门诊病历、误工证明",
                "围护 130 分钟到场略超时限；权属排查因移交记录缺失一度待定，通信公司认领后维修及时；双方均有责任。",
                Constants.Responsibility.SHARED, 70, 30, "3200", Constants.CompStatus.APPROVED, 51);
        cost(i8, "450", "2200", "3200", Constants.CostStatus.ALLOCATED,
                "[{\"party\":\"权属单位-联合通信公司\",\"amount\":4090,\"reason\":\"承担 70% 赔付及全部维修费\"},{\"party\":\"街道\",\"amount\":1760,\"reason\":\"围护略超时，承担 30% 赔付及应急费\"}]",
                "按 7:3 分摊赔付金，维修费由通信公司承担。", 50);

        // 9. 维修超时结案：市政井盖塌陷 72 小时才修完
        Incident i9 = base("MH20260812-0001", citizen, "塌陷", "和平路东段", "次干道", "高",
                false, null, 45);
        i9.setStatus(Constants.IncidentStatus.CLOSED);
        i9.setEmergencyStatus(Constants.EmergencyStatus.SECURED);
        i9.setOwnerStatus(Constants.OwnerStatus.CONFIRMED);
        i9.setOwnerId(municipal.getId());
        i9.setClosedAt(i9.getCreatedAt().plusDays(5));
        incidentRepo.save(i9);
        emergency(i9, 40, 200, 230, "警示锥,临时盖板,夜间反光标识", "500");
        investigation(i9, "铸字\"市政\"", "雨水主管", "道路大修后移交", "商户反映多次报修", "台账属市政",
                "CONFIRMED", municipal, 44);
        claim(i9, municipal, "排查指定", 44);
        order(i9, municipal, repair1, 44, 72, "井室破损严重", "Φ700 重型井盖",
                true, false, null, "封路重砌井室，耗时较长", "6000");
        acceptance(i9, true, true, true, true, "PASSED", "修复合格但维修超时", 41);
        cost(i9, "500", "6000", "0", Constants.CostStatus.ALLOCATED,
                "[{\"party\":\"权属单位-市政工程公司\",\"amount\":6500,\"reason\":\"权属确认且维修超时，承担全部费用\"}]",
                "维修超时 24 小时，纳入市政公司考核。", 40);

        // 10. 验收通过待复核：人民路西段井盖异响
        Incident i10 = base("MH20260911-0001", citizen2, "异响", "人民路西段", "支路", "低",
                false, null, 15);
        i10.setStatus(Constants.IncidentStatus.ACCEPTED);
        i10.setEmergencyStatus(Constants.EmergencyStatus.SECURED);
        i10.setOwnerStatus(Constants.OwnerStatus.CONFIRMED);
        i10.setOwnerId(municipal.getId());
        incidentRepo.save(i10);
        emergency(i10, 35, 260, 290, "警示锥", "150");
        investigation(i10, "铸字\"市政\"", "污水管线", "无", "商户反映车辆碾压异响", "台账属市政",
                "CONFIRMED", municipal, 14);
        claim(i10, municipal, "排查指定", 14);
        order(i10, municipal, repair1, 14, 12, "井室完好，胶条老化", "Φ600 铸铁井盖",
                false, false, null, "更换减震胶条", "900");
        acceptance(i10, true, true, true, true, "PASSED", "异响消除", 13);
        cost(i10, "150", "900", "0", Constants.CostStatus.PENDING,
                "[{\"party\":\"权属单位-市政工程公司\",\"amount\":1050,\"reason\":\"权属已确认，由权属单位承担全部费用\"}]",
                null, 0);

        seedNightFailures();
    }

    /** 11/12. 夜间临时围护失效：学校门口二次事故（外力破坏）、公交站围护方式不足待处置 */
    private void seedNightFailures() {
        // 11. 学校门口：夜间警示锥被车辆撞倒，清晨第二名骑车人摔伤
        LocalDateTime t11 = LocalDateTime.now().minusDays(1).withHour(18).withMinute(40).withSecond(0).withNano(0);
        Incident i11 = base("MH20261001-0001", citizen2, "缺失", "育才街第二实验小学东门对面", "次干道", "高",
                false, null, 0);
        i11.setCreatedAt(t11);
        i11.setUpdatedAt(t11);
        i11.setStatus(Constants.IncidentStatus.SECURED);
        i11.setEmergencyStatus(Constants.EmergencyStatus.SECURED);
        i11.setOwnerStatus(Constants.OwnerStatus.INVESTIGATING);
        incidentRepo.save(i11);
        EmergencyMeasure e11 = new EmergencyMeasure();
        e11.setIncidentId(i11.getId());
        e11.setOfficerId(street.getId());
        e11.setDispatchedAt(t11.plusMinutes(5));
        e11.setArrivedAt(t11.plusMinutes(25));
        e11.setSecuredAt(t11.plusMinutes(35));
        e11.setMeasures("警示锥,夜间反光标识");
        e11.setCost(new BigDecimal("200"));
        e11.setNote("放学高峰后到场，沿井口摆放警示锥 6 个并贴反光条");
        e11.setCreatedAt(e11.getDispatchedAt());
        emergencyRepo.save(e11);

        // 两轮夜间巡查：22:00 完好；01:30 到场发现锥桶 01:10 已被撞散
        nightPatrol(i11, "22:00 第一轮", t11.plusHours(3).plusMinutes(20), true,
                "6 个警示锥在位，反光条完好，校门口路灯一侧不亮", null, null);
        nightPatrol(i11, "01:30 第二轮", t11.plusHours(6).plusMinutes(50), false,
                "警示锥被撞散 3 个倒伏在路边，反光标识脱落，路面有车辆碎片",
                t11.plusHours(6).plusMinutes(30), "疑似社会车辆冲入围护带后逃逸");

        NightFailure nf11 = new NightFailure();
        nf11.setCode("NF" + t11.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")) + "-0001");
        nf11.setIncidentId(i11.getId());
        nf11.setOfficerId(street.getId());
        nf11.setLastPatrolAt(t11.plusHours(3).plusMinutes(20));
        nf11.setConeMovedAt(t11.plusHours(6).plusMinutes(30));
        nf11.setPatrolGapMinutes(190L);
        nf11.setReportedBy("夜间巡查发现，交警转递车辆碎片");
        nf11.setPhotos("[]");
        nf11.setHasSecondAccident(true);
        nf11.setSecondAccidentAt(t11.plusHours(12).plusMinutes(15));
        nf11.setInjuredName("骑车人-陈晨");
        nf11.setInjuredPhone("13900000012");
        nf11.setInjuryDesc("清晨骑行上学途中碾到移位警示锥后摔倒，右膝挫伤、右手擦伤，已由 120 送医");
        nf11.setAccidentDesc("06:55 天未全亮，伤者未看见井口，连人带车摔倒；书包挡在井口前，路人报警");
        nf11.setAccidentPhotos("[]");
        nf11.setCause(Constants.FailureCause.EXTERNAL_DAMAGE);
        nf11.setCauseAnalysis("时间链：18:40 上报，19:15 仅以警示锥+反光条围护；22:00 巡查完好；01:10 警示锥被车辆撞散（距上次巡查 190 分钟，略超 180 分钟时限），现场留有车辆碎片并逃逸，属外力破坏，但巡查间隔偏长也导致未能及时复围，清晨能见度低时引发二次事故。");
        nf11.setReinforceMeasures(Constants.ReinforceMeasure.REFLECTIVE_BARRIER + ","
                + Constants.ReinforceMeasure.GUARD_DUTY);
        nf11.setDecisionAt(t11.plusHours(7).plusMinutes(20));
        nf11.setDecisionOfficerId(street.getId());
        nf11.setDecisionNote("02:00 加派带灯反光围挡 8 米封闭井口区域；05:30 起安排 2 名值守人员至早高峰结束");
        nf11.setReinforced(true);
        nf11.setReinforcedAt(t11.plusHours(10).plusMinutes(50));
        nf11.setSensitiveLocation(true);
        nf11.setLocationType("学校门口");
        nf11.setLocationName("第二实验小学（东门）");
        nf11.setPeakHours("07:00-08:00、16:30-18:00");
        nf11.setLighting("昏暗无路灯");
        nf11.setCameraIds("JY-0112,JY-0113");
        nf11.setDetourNotice(true);
        nf11.setDetourNote("校门两侧 50 米各放 2 块绕行提示牌，引导非机动车辆走对面慢行道");
        nf11.setTrafficPoliceAssist(true);
        nf11.setTrafficPoliceNote("已请求辖区交警中队 06:40 到场护学，警力编号 JJ3207；同步调取 JY-0112 监控追查肇事车辆");
        nf11.setCreatedAt(t11.plusHours(6).plusMinutes(50));
        nf11.setUpdatedAt(nf11.getCreatedAt());
        nightFailureRepo.save(nf11);

        // 二次事故赔付案件
        CompensationCase c11 = new CompensationCase();
        c11.setIncidentId(i11.getId());
        c11.setCaseType("SECOND");
        c11.setNightFailureId(nf11.getId());
        c11.setClaimantName("骑车人-陈晨");
        c11.setClaimantPhone("13900000012");
        c11.setDamageType("人身伤害");
        c11.setAccidentProof("夜间围护失效二次事故：右膝挫伤、右手擦伤；120 出诊记录、急诊病历、现场照片");
        c11.setStatus(Constants.CompStatus.FILED);
        c11.setCreatedAt(nf11.getSecondAccidentAt().plusMinutes(20));
        compensationRepo.save(c11);
        nf11.setCompensationId(c11.getId());
        nightFailureRepo.save(nf11);

        // 12. 公交站旁：仅设警示锥，深夜被风吹倒/带倒，围护方式不足，处置决策待落实
        LocalDateTime t12 = LocalDateTime.now().minusHours(10).withMinute(0).withSecond(0).withNano(0);
        Incident i12 = base("MH20261002-0001", citizen, "松动", "中山广场公交站北侧 30 米", "支路", "中",
                false, null, 0);
        i12.setCreatedAt(t12);
        i12.setUpdatedAt(t12);
        i12.setStatus(Constants.IncidentStatus.SECURED);
        i12.setEmergencyStatus(Constants.EmergencyStatus.SECURED);
        i12.setOwnerStatus(Constants.OwnerStatus.INVESTIGATING);
        incidentRepo.save(i12);
        EmergencyMeasure e12 = new EmergencyMeasure();
        e12.setIncidentId(i12.getId());
        e12.setOfficerId(street.getId());
        e12.setDispatchedAt(t12.plusMinutes(8));
        e12.setArrivedAt(t12.plusMinutes(40));
        e12.setSecuredAt(t12.plusMinutes(50));
        e12.setMeasures("警示锥");
        e12.setCost(new BigDecimal("80"));
        e12.setNote("夜间只用 4 个警示锥围在井盖四周，未放反光围挡");
        e12.setCreatedAt(e12.getDispatchedAt());
        emergencyRepo.save(e12);

        NightFailure nf12 = new NightFailure();
        nf12.setCode("NF" + t12.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")) + "-0001");
        nf12.setIncidentId(i12.getId());
        nf12.setOfficerId(street.getId());
        nf12.setLastPatrolAt(e12.getSecuredAt());
        nf12.setConeMovedAt(t12.plusHours(3).plusMinutes(10));
        nf12.setPatrolGapMinutes(200L);
        nf12.setReportedBy("公交末班司机反映");
        nf12.setPhotos("[]");
        nf12.setHasSecondAccident(false);
        nf12.setCause(Constants.FailureCause.INADEQUATE);
        nf12.setCauseAnalysis("围护仅使用 4 个普通警示锥，无反光围挡与临时盖板，深夜可视距离不足，且 200 分钟无人复核，属围护方式不足（同时巡查间隔偏长）；公交站旁非机动车借道时极易剐蹭。");
        nf12.setReinforceMeasures(Constants.ReinforceMeasure.REFLECTIVE_BARRIER + ","
                + Constants.ReinforceMeasure.TEMP_CLOSURE);
        nf12.setDecisionAt(t12.plusHours(3).plusMinutes(30));
        nf12.setDecisionOfficerId(street.getId());
        nf12.setDecisionNote("决定加派带灯反光围挡并临时封闭公交站北侧非机动车道，待凌晨施工窗修复");
        nf12.setReinforced(false);
        nf12.setSensitiveLocation(true);
        nf12.setLocationType("公交站");
        nf12.setLocationName("中山广场公交站");
        nf12.setPeakHours("07:00-09:00、17:00-19:00");
        nf12.setLighting("一般");
        nf12.setCameraIds("BUS-ZS-0312");
        nf12.setDetourNotice(false);
        nf12.setTrafficPoliceAssist(false);
        nf12.setCreatedAt(t12.plusHours(3).plusMinutes(20));
        nf12.setUpdatedAt(nf12.getCreatedAt());
        nightFailureRepo.save(nf12);
    }

    private void nightPatrol(Incident inc, String round, LocalDateTime at, boolean intact,
                             String siteStatus, LocalDateTime coneMovedAt, String note) {
        NightPatrol p = new NightPatrol();
        p.setIncidentId(inc.getId());
        p.setOfficerId(street.getId());
        p.setRoundName(round);
        p.setArrivedAt(at);
        p.setIntact(intact);
        p.setSiteStatus(siteStatus);
        p.setPhotos("[]");
        p.setConeMovedAt(coneMovedAt);
        p.setNote(note);
        p.setCreatedAt(at);
        nightPatrolRepo.save(p);
    }

    private Incident base(String code, User reporter, String issueType, String address, String roadType,
                          String danger, boolean hasDamage, String damageDesc, int daysAgo) {
        Incident inc = new Incident();
        inc.setCode(code);
        inc.setReporterId(reporter.getId());
        inc.setIssueType(issueType);
        inc.setAddress(address);
        inc.setRoadType(roadType);
        inc.setLongitude(114.48 + Math.random() * 0.1);
        inc.setLatitude(38.03 + Math.random() * 0.06);
        inc.setPhotos("[]");
        inc.setHasDamage(hasDamage);
        inc.setDamageDesc(damageDesc);
        inc.setDangerLevel(danger);
        inc.setCreatedAt(LocalDateTime.now().minusDays(daysAgo));
        inc.setUpdatedAt(inc.getCreatedAt());
        return inc;
    }

    private void emergency(Incident inc, int dispatchMin, int arriveMin, int secureMin,
                           String measures, String cost) {
        EmergencyMeasure em = new EmergencyMeasure();
        em.setIncidentId(inc.getId());
        em.setOfficerId(street.getId());
        em.setDispatchedAt(inc.getCreatedAt().plusMinutes(dispatchMin));
        em.setArrivedAt(inc.getCreatedAt().plusMinutes(arriveMin));
        em.setSecuredAt(inc.getCreatedAt().plusMinutes(secureMin));
        em.setMeasures(measures);
        em.setCost(new BigDecimal(cost));
        em.setNote("现场已完成临时围护");
        em.setCreatedAt(em.getDispatchedAt());
        emergencyRepo.save(em);
    }

    private void investigation(Incident inc, String marking, String pipeline, String construction,
                               String merchant, String ledger, String conclusion, User owner, int daysAgo) {
        OwnershipInvestigation inv = new OwnershipInvestigation();
        inv.setIncidentId(inc.getId());
        inv.setInvestigatorId(street.getId());
        inv.setManholeMarking(marking);
        inv.setPipelineInfo(pipeline);
        inv.setConstructionHistory(construction);
        inv.setMerchantInfo(merchant);
        inv.setCityLedger(ledger);
        inv.setConclusion(conclusion);
        inv.setConfirmedOwnerId(owner == null ? null : owner.getId());
        inv.setCreatedAt(LocalDateTime.now().minusDays(daysAgo));
        investigationRepo.save(inv);
    }

    private void claim(Incident inc, User owner, String type, int daysAgo) {
        OwnerClaim c = new OwnerClaim();
        c.setIncidentId(inc.getId());
        c.setOwnerId(owner.getId());
        c.setClaimType(type);
        c.setCreatedAt(LocalDateTime.now().minusDays(daysAgo));
        claimRepo.save(c);
    }

    private RepairOrder order(Incident inc, User owner, User team, int assignDaysAgo, long repairHours,
                              String chamber, String spec, boolean closure, boolean coord, String coordUnits,
                              String note, String cost) {
        RepairOrder o = new RepairOrder();
        o.setIncidentId(inc.getId());
        o.setOwnerId(owner.getId());
        o.setTeamId(team.getId());
        o.setAssignedAt(LocalDateTime.now().minusDays(assignDaysAgo));
        o.setAcceptedAt(o.getAssignedAt().plusHours(2));
        o.setArrivedAt(o.getAssignedAt().plusHours(5));
        o.setChamberCondition(chamber);
        o.setCoverSpec(spec);
        o.setNeedRoadClosure(closure);
        o.setNeedCoord(coord);
        o.setCoordUnits(coordUnits);
        o.setRepairNote(note);
        o.setRepairCost(new BigDecimal(cost));
        o.setFinishedAt(o.getAssignedAt().plusHours(repairHours));
        o.setStatus(Constants.RepairStatus.FINISHED);
        return repairOrderRepo.save(o);
    }

    private void acceptance(Incident inc, boolean photo, boolean loc, boolean flat, boolean roll,
                            String result, String note, int daysAgo) {
        Acceptance a = new Acceptance();
        a.setIncidentId(inc.getId());
        a.setInspectorId(inspector.getId());
        a.setPhotoOk(photo);
        a.setLocationOk(loc);
        a.setFlatnessOk(flat);
        a.setRollingTestOk(roll);
        a.setResult(result);
        a.setNote(note);
        a.setInspectedAt(LocalDateTime.now().minusDays(daysAgo));
        acceptanceRepo.save(a);
    }

    private void compensation(Incident inc, String damageType, String claimAmount, String proof,
                              String analysis, String responsibility, int ownerRatio, int streetRatio,
                              String approved, String status, int decidedDaysAgo) {
        CompensationCase c = new CompensationCase();
        c.setIncidentId(inc.getId());
        c.setSpecialistId(comp.getId());
        c.setClaimantName("市民");
        c.setClaimantPhone("13800000002");
        c.setDamageType(damageType);
        c.setClaimAmount(new BigDecimal(claimAmount));
        c.setAccidentProof(proof);
        c.setAnalysis(analysis);
        c.setResponsibility(responsibility);
        c.setOwnerRatio(ownerRatio);
        c.setStreetRatio(streetRatio);
        c.setApprovedAmount(new BigDecimal(approved));
        c.setStatus(status);
        c.setCreatedAt(inc.getCreatedAt().plusHours(3));
        c.setDecidedAt(LocalDateTime.now().minusDays(decidedDaysAgo));
        if (Constants.CompStatus.PAID.equals(status)) {
            c.setPaidAt(c.getDecidedAt().plusDays(1));
        }
        compensationRepo.save(c);
    }

    private void cost(Incident inc, String emergency, String repair, String compAmount,
                      String status, String allocations, String reviewNote, int reviewedDaysAgo) {
        CostAllocation ca = new CostAllocation();
        ca.setIncidentId(inc.getId());
        ca.setEmergencyCost(new BigDecimal(emergency));
        ca.setRepairCost(new BigDecimal(repair));
        ca.setCompensationAmount(new BigDecimal(compAmount));
        ca.setTotalAmount(ca.getEmergencyCost().add(ca.getRepairCost()).add(ca.getCompensationAmount()));
        ca.setAllocations(allocations);
        ca.setStatus(status);
        ca.setReviewNote(reviewNote);
        ca.setCreatedAt(inc.getCreatedAt().plusDays(2));
        if (Constants.CostStatus.ALLOCATED.equals(status)) {
            ca.setReviewedBy(admin.getId());
            ca.setReviewedAt(LocalDateTime.now().minusDays(reviewedDaysAgo));
        }
        costRepo.save(ca);
    }

    private void seedRoutesAndRules() {
        InspectionRoute r1 = new InspectionRoute();
        r1.setName("巡检路线 A（高发路段加密）");
        r1.setSections("人民路,建设大街,和平路");
        r1.setFrequency("每日");
        r1.setNote("近 90 天高发路段，加密为每日巡检");
        routeRepo.save(r1);

        InspectionRoute r2 = new InspectionRoute();
        r2.setName("巡检路线 B（商业区）");
        r2.setSections("中山路,解放大道,和平路商业街");
        r2.setFrequency("隔日");
        r2.setNote("人流量大，重点关注松动与异响");
        routeRepo.save(r2);

        InspectionRoute r3 = new InspectionRoute();
        r3.setName("巡检路线 C（背街小巷）");
        r3.setSections("文化巷,滨河路,胜利街");
        r3.setFrequency("每周");
        r3.setNote("夜间照明不足路段，重点检查反光标识");
        routeRepo.save(r3);

        HandoverRule rule1 = new HandoverRule();
        rule1.setTitle("新建道路井盖设施移交时限");
        rule1.setContent("新建、改建道路竣工验收后 15 日内，施工单位须将井盖设施台账（位置、规格、权属、管线资料）移交市政工程公司并报城管局备案；逾期未移交期间发生的井盖事件，由施工单位承担责任。");
        ruleRepo.save(rule1);

        HandoverRule rule2 = new HandoverRule();
        rule2.setTitle("专业管线井盖协同核验");
        rule2.setContent("涉及燃气、电力、通信等专业井盖的施工，开工前须通知权属单位现场核验，施工结束后由权属单位签字确认井盖复位质量；未履行协同程序的，发生问题由施工单位与权属单位按 5:5 分摊。");
        ruleRepo.save(rule2);

        HandoverRule rule3 = new HandoverRule();
        rule3.setTitle("无主井盖先行处置与费用回补");
        rule3.setContent("权属无法即时确认的井盖险情，街道先行设置警示锥、临时盖板、夜间反光标识并垫付费用；权属确认后 30 日内由权属单位回补街道垫付费用，复核分摊结果以城管部门费用复核单为准。");
        ruleRepo.save(rule3);
    }
}
