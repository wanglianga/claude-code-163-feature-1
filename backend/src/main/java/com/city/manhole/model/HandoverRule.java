package com.city.manhole.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 施工移交规则：依据权属争议与维修超时沉淀的管理规则。
 */
@Data
@Entity
@Table(name = "handover_rule")
public class HandoverRule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 128)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    private LocalDateTime updatedAt = LocalDateTime.now();
}
