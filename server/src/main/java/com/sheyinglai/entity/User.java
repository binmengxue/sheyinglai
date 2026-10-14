package com.sheyinglai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;

/** 平台用户：注册即自动成为摄影师（photographer 表同 id） */
@Data
@TableName("app_user")
public class User {
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_USER = "USER";
    public static final String STATUS_NORMAL = "NORMAL";
    public static final String STATUS_BANNED = "BANNED";
    /** 已注销：个人信息已清除，不能再登录 */
    public static final String STATUS_DELETED = "DELETED";

    @TableId(type = IdType.AUTO)
    private Long id;
    private String phone;
    @JsonIgnore
    private String passwordHash;
    private String nickname;
    private String avatarUrl;
    /** 0 未知，1 男，2 女 */
    private Integer gender;
    private String bio;
    private String city;
    /** 微信号：只给本人和已确认预约的客户看 */
    private String wechat;
    private String role;
    private String status;
    private LocalDateTime createdAt;
}
