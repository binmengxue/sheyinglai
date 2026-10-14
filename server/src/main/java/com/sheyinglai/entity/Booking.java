package com.sheyinglai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** 预约单：不在线付款，摄影师确认后双方线下联系 */
@Data
@TableName("booking")
public class Booking {
    public static final String PENDING = "PENDING";
    public static final String CONFIRMED = "CONFIRMED";
    public static final String REJECTED = "REJECTED";
    public static final String CANCELLED = "CANCELLED";
    public static final String COMPLETED = "COMPLETED";
    /** 摄影师在拍摄日前一直没处理，系统自动关闭 */
    public static final String EXPIRED = "EXPIRED";

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long customerId;
    private Long photographerId;
    private LocalDate shootDate;
    private LocalTime startTime;
    private Integer hours;
    private Integer people;
    private String address;
    private String style;
    private String remark;
    private String customerContact;
    private Integer priceEstimate;
    private String status;
    private Long cancelledBy;
    private Boolean reviewed;
    /** 摄影师确认或拒绝的时间，用来算回复时长 */
    private LocalDateTime respondedAt;
    private LocalDateTime createdAt;
    private String cancelReason;
    /** 按取消规则算出的违约金（元），线下结算 */
    private Integer penaltyAmount;
    /** 已发过拍摄前一天的提醒 */
    private Boolean reminded;
    private Integer rescheduleCount;

    public LocalTime endTime() {
        return startTime.plusHours(hours);
    }

    public LocalDateTime startAt() {
        return shootDate.atTime(startTime);
    }
}
