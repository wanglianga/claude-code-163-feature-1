package com.city.manhole.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "sys_user")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String username;

    @Column(nullable = false)
    private String password;

    /** 姓名/显示名 */
    @Column(nullable = false, length = 64)
    private String name;

    /** 角色，见 Constants.Role */
    @Column(nullable = false, length = 32)
    private String role;

    /** 所属单位（权属单位/维修队/部门名称） */
    @Column(length = 128)
    private String orgName;

    @Column(length = 32)
    private String phone;

    private LocalDateTime createdAt = LocalDateTime.now();
}
