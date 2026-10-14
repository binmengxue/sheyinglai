package com.sheyinglai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 站内消息，userId 是接收人 */
@Data
@TableName("notification")
public class Notification {
    public static final String TYPE_BOOKING = "BOOKING";
    public static final String TYPE_REVIEW = "REVIEW";
    public static final String TYPE_FOLLOW = "FOLLOW";

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String type;
    private String title;
    private String content;
    /** 点击后跳转的前端路由 */
    private String link;
    private Boolean isRead;
    private LocalDateTime createdAt;
}
