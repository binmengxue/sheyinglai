package com.sheyinglai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;

/** 摄影师每日经营数据：曝光（出现在首页列表）、主页浏览、咨询（发起预约） */
@Data
@TableName("stat_daily")
public class StatDaily {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long photographerId;
    private LocalDate statDate;
    private Integer exposure;
    private Integer profileView;
    private Integer consult;
}
