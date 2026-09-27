package com.city.manhole.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 验收记录：城管巡查员按照片、定位、路面平整度、车辆碾压测试验收。
 */
@Data
@Entity
@Table(name = "acceptance")
public class Acceptance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long incidentId;

    @Column(nullable = false)
    private Long inspectorId;

    /** 照片比对通过 */
    private Boolean photoOk = false;

    /** 定位一致 */
    private Boolean locationOk = false;

    /** 路面平整度合格 */
    private Boolean flatnessOk = false;

    /** 车辆碾压测试通过 */
    private Boolean rollingTestOk = false;

    /** PASSED 通过 / FAILED 不通过 */
    @Column(nullable = false, length = 16)
    private String result;

    @Column(columnDefinition = "TEXT")
    private String note;

    private LocalDateTime inspectedAt = LocalDateTime.now();
}
