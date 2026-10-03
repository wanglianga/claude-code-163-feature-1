package com.city.manhole.controller;

import com.city.manhole.common.ApiResponse;
import com.city.manhole.dto.Req;
import com.city.manhole.model.NightFailure;
import com.city.manhole.model.NightPatrol;
import com.city.manhole.service.NightFailureService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 夜间临时围护失效处理：夜间巡查到场、围护失效与二次事故登记、原因判定与加固决策、加固落实确认。
 */
@RestController
@RequiredArgsConstructor
public class NightFailureController {
    private final NightFailureService service;

    /** 夜间巡查到场 */
    @PostMapping("/api/incidents/{incidentId}/night-patrols")
    public ApiResponse<NightPatrol> patrol(@PathVariable Long incidentId,
                                           @RequestBody Req.NightPatrolReq req) {
        return ApiResponse.ok(service.patrol(incidentId, req.roundName(), req.arrivedAt(), req.intact(),
                req.siteStatus(), req.photos(), req.coneMovedAt(), req.note()));
    }

    /** 登记围护失效（含二次事故、原因初判、敏感地点信息、处置决策） */
    @PostMapping("/api/incidents/{incidentId}/night-failures")
    public ApiResponse<NightFailure> report(@PathVariable Long incidentId,
                                            @RequestBody Req.NightFailureReq req) {
        return ApiResponse.ok(service.reportFailure(incidentId, new NightFailureService.ReqFields(
                req.lastPatrolAt(), req.coneMovedAt(), req.reportedBy(), req.photos(),
                req.hasSecondAccident(), req.secondAccidentAt(), req.injuredName(), req.injuredPhone(),
                req.injuryDesc(), req.accidentDesc(), req.accidentPhotos(), req.cause(), req.causeAnalysis(),
                req.reinforceMeasures(), req.decisionNote(), req.sensitiveLocation(), req.locationType(),
                req.locationName(), req.peakHours(), req.lighting(), req.cameraIds(),
                req.detourNotice(), req.detourNote(), req.trafficPoliceAssist(), req.trafficPoliceNote())));
    }

    /** 值班员研判：补充原因判定与加固处置决策 */
    @PostMapping("/api/night-failures/{id}/decision")
    public ApiResponse<NightFailure> decision(@PathVariable Long id,
                                              @RequestBody Req.NightFailureDecisionReq req) {
        return ApiResponse.ok(service.decide(id, req.cause(), req.causeAnalysis(), req.reinforceMeasures(),
                req.decisionNote(), req.detourNotice(), req.detourNote(),
                req.trafficPoliceAssist(), req.trafficPoliceNote()));
    }

    /** 确认加固处置已落实（反光围挡到场/封道完成/值守就位） */
    @PostMapping("/api/night-failures/{id}/reinforce")
    public ApiResponse<NightFailure> reinforce(@PathVariable Long id,
                                               @RequestBody(required = false) Req.NightFailureReinforceReq req) {
        return ApiResponse.ok(service.confirmReinforced(id, req == null ? null : req.note()));
    }

    /** 全部夜间围护失效记录 */
    @GetMapping("/api/night-failures")
    public ApiResponse<List<NightFailure>> list() {
        return ApiResponse.ok(service.listAll());
    }
}
