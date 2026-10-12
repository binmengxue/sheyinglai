package com.sheyinglai.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;
import java.util.List;

/** 摄影师编辑接单资料 */
public record PhotographerReq(
        @Pattern(regexp = "陪拍|专摄", message = "类型只能是陪拍或专摄") String tag,
        @NotNull(message = "请填写每小时价格") @Min(1) @Max(100000) Integer pricePerHour,
        @Min(0) @Max(100000) Integer extraPersonPrice,
        @NotNull @Min(1) @Max(12) Integer minHours,
        @Min(0) @Max(1000) Integer freeKm,
        @Min(0) @Max(1000) Integer kmFee,
        @Min(0) @Max(10000) Integer rawCount,
        @Min(0) @Max(1000) Integer retouchCount,
        @Min(0) @Max(10000) Integer retouchExtraPrice,
        @Size(max = 500) String retouchRule,
        @Size(max = 200) String makeupNote,
        @Size(max = 500) String equipment,
        @Pattern(regexp = "LOOSE|NORMAL|STRICT") String cancelPolicy,
        @Min(0) @Max(100) Integer penaltyPercent,
        Boolean refundPromise,
        @Size(max = 10) List<@Size(max = 16) String> styles,
        @NotNull @Pattern(regexp = "ALL|WEEKDAY|WEEKEND") String workDays,
        @NotNull LocalTime workStart,
        @NotNull LocalTime workEnd,
        @Min(-90) @Max(90) Double lat,
        @Min(-180) @Max(180) Double lng,
        @Size(max = 200) String address) {
}
