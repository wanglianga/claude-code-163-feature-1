package com.city.manhole.service;

import com.city.manhole.common.Constants;
import com.city.manhole.model.User;
import com.city.manhole.repo.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 下拉选项等元数据 */
@Service
@RequiredArgsConstructor
public class MetaService {
    private final UserRepo userRepo;

    @Value("${app.night-patrol-limit-minutes:180}")
    private long nightPatrolLimitMinutes;

    public Map<String, Object> meta() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("issueTypes", IncidentService.ISSUE_TYPES);
        m.put("roadTypes", IncidentService.ROAD_TYPES);
        m.put("dangerLevels", IncidentService.DANGER_LEVELS);
        m.put("measureTypes", EmergencyService.MEASURE_TYPES);
        m.put("failureCauses", List.of(
                Map.of("value", Constants.FailureCause.INADEQUATE, "label", "围护方式不足"),
                Map.of("value", Constants.FailureCause.INTERVAL_TOO_LONG, "label", "巡查间隔过长"),
                Map.of("value", Constants.FailureCause.EXTERNAL_DAMAGE, "label", "外力破坏")));
        m.put("reinforceMeasures", List.of(
                Map.of("value", Constants.ReinforceMeasure.REFLECTIVE_BARRIER, "label", "加派反光围挡"),
                Map.of("value", Constants.ReinforceMeasure.TEMP_CLOSURE, "label", "临时封道"),
                Map.of("value", Constants.ReinforceMeasure.GUARD_DUTY, "label", "安排专人值守")));
        m.put("sensitiveLocationTypes", NightFailureService.LOCATION_TYPES);
        m.put("lightingOptions", NightFailureService.LIGHTINGS);
        m.put("nightPatrolLimitMinutes", nightPatrolLimitMinutes);
        m.put("coordUnits", List.of("燃气", "电力", "通信"));
        m.put("damageTypes", List.of("人身伤害", "车辆损失", "财产损失"));
        m.put("ownerUnits", userRepo.findByRole(Constants.Role.OWNER_UNIT).stream()
                .map(this::toOption).toList());
        m.put("repairTeams", userRepo.findByRole(Constants.Role.REPAIR_TEAM).stream()
                .map(this::toOption).toList());
        return m;
    }

    private Map<String, Object> toOption(User u) {
        Map<String, Object> o = new LinkedHashMap<>();
        o.put("id", u.getId());
        o.put("name", u.getName());
        o.put("orgName", u.getOrgName());
        return o;
    }
}
