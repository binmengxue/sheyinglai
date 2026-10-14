package com.sheyinglai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 用户举报：摄影师、评价、客片 */
@Data
@TableName("report")
public class Report {
    public static final String TARGET_PHOTOGRAPHER = "PHOTOGRAPHER";
    public static final String TARGET_REVIEW = "REVIEW";
    public static final String TARGET_ALBUM = "ALBUM";

    public static final String PENDING = "PENDING";
    /** 已处理（删除、隐藏或封禁） */
    public static final String HANDLED = "HANDLED";
    /** 核实后不需要处理 */
    public static final String IGNORED = "IGNORED";

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long reporterId;
    private String targetType;
    private Long targetId;
    private String reason;
    private String content;
    private String status;
    private Long handledBy;
    private String handleNote;
    private LocalDateTime handledAt;
    private LocalDateTime createdAt;
}
