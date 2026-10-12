package com.sheyinglai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("review")
public class Review {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long bookingId;
    private Long customerId;
    private Long photographerId;
    /** 1~5 分 */
    private Integer score;
    private String content;
    private LocalDateTime createdAt;
}
