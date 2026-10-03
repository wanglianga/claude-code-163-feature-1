package com.city.manhole.controller;

import com.city.manhole.common.ApiResponse;
import com.city.manhole.dto.Req;
import com.city.manhole.model.NightProtectionFailure;
import com.city.manhole.service.NightFailureService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 夜间临时围护失效与二次事故处理。
 */
@RestController
@RequestMapping("/api/incidents/{incidentId}/night-failure")
@RequiredArgsConstructor
public class NightFailureController {
    private final NightFailureService nightService;

    /** 登记夜间围护失效与二次事故 */
    @PostMapping
    public ApiResponse<NightProtectionFailure> register(@PathVariable Long incidentId,
                                                        @RequestBody Req.NightFailureRegisterReq req) {
        return ApiResponse.ok(nightService.register(incidentId, new NightFailureService.ReqData(
                req.displacedAt(), req.secondAccidentAt(), req.secondAccidentDesc(),
                req.hasInjury(), req.injuryDesc(), req.protectionPhotos(), req.sensitiveLocation(),
                req.peakHours(), req.lighting(), req.cameraIds(), req.detourNotice(), req.detourNote(),
                req.trafficPoliceAssist(), req.trafficPoliceNote(), req.patrolArrivedAt())));
    }

    /** 巡查到场核查（补充围护/移位现场照片） */
    @PostMapping("/patrol")
    public ApiResponse<NightProtectionFailure> patrol(@PathVariable Long incidentId,
                                                      @RequestBody Req.NightPatrolReq req) {
        return ApiResponse.ok(nightService.patrol(incidentId, req.protectionPhotos(), req.note()));
    }

    /** 认定失效原因 */
    @PostMapping("/assess")
    public ApiResponse<NightProtectionFailure> assess(@PathVariable Long incidentId,
                                                      @RequestBody Req.NightAssessReq req) {
        return ApiResponse.ok(nightService.assess(incidentId, req.cause(), req.causeAnalysis()));
    }

    /** 加派措施处置（反光围挡/临时封道/专人值守，可追加应急垫付费用） */
    @PostMapping("/handle")
    public ApiResponse<NightProtectionFailure> handle(@PathVariable Long incidentId,
                                                      @RequestBody Req.NightHandleReq req) {
        return ApiResponse.ok(nightService.handle(incidentId, req.decisions(), req.handleNote(),
                req.detourNotice(), req.detourNote(), req.trafficPoliceAssist(), req.trafficPoliceNote(),
                req.addCost()));
    }

    /** 获取处置建议（依据原因/危险等级/敏感场所/照明） */
    @GetMapping("/suggest")
    public ApiResponse<Map<String, Object>> suggest(@PathVariable Long incidentId) {
        return ApiResponse.ok(nightService.suggest(incidentId));
    }
}
