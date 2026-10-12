package com.sheyinglai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalTime;

/** 摄影师接单资料，主键就是 user.id；金额单位都是元 */
@Data
@TableName("photographer")
public class Photographer {
    @TableId(type = IdType.INPUT)
    private Long userId;
    /** 陪拍 / 专摄 */
    private String tag;
    /** 每小时价格；为空表示资料还没填好，不在首页展示 */
    private Integer pricePerHour;
    /** 加人费：元/人/小时 */
    private Integer extraPersonPrice;
    private Integer minHours;
    /** 多少公里内免交通费，超出按 kmFee 元/公里 */
    private Integer freeKm;
    private Integer kmFee;
    /** 每小时底片数、免费精修数、超出精修单价 */
    private Integer rawCount;
    private Integer retouchCount;
    private Integer retouchExtraPrice;
    private String retouchRule;
    private String makeupNote;
    private String equipment;
    /** 爽约规则：LOOSE 宽松 / NORMAL 一般 / STRICT 严格 */
    private String cancelPolicy;
    private Integer penaltyPercent;
    private Boolean refundPromise;
    /** 擅长类型，逗号分隔：随拍,人像写真,领证照 */
    private String styles;
    /** 可接单日：ALL 整周 / WEEKDAY 工作日 / WEEKEND 周末 */
    private String workDays;
    private LocalTime workStart;
    private LocalTime workEnd;
    /** 接单位置，只用来算距离，不直接返回给其他人 */
    @JsonIgnore
    private Double lat;
    @JsonIgnore
    private Double lng;
    private String address;
    private BigDecimal ratingAvg;
    private Integer ratingCount;
    private Integer dealCount;
    /** 管理员隐藏后不在首页出现，主页也打不开 */
    private Boolean visible;

    /** 是否设置过接单位置（坐标本身不返回） */
    public boolean isHasLocation() {
        return lat != null && lng != null;
    }

    /** 填了价格才算资料完整，才会出现在首页 */
    @JsonIgnore
    public boolean isReady() {
        return pricePerHour != null && pricePerHour > 0;
    }
}
