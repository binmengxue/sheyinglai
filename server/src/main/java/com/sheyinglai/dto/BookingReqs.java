package com.sheyinglai.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

public final class BookingReqs {

    private BookingReqs() {
    }

    public record CreateReq(
            @NotNull Long photographerId,
            @NotNull(message = "请选择日期") LocalDate shootDate,
            @NotNull(message = "请选择开始时间") LocalTime startTime,
            @NotNull @Min(1) @Max(12) Integer hours,
            @NotNull @Min(1) @Max(20) Integer people,
            @Size(max = 200) String address,
            @Size(max = 32) String style,
            @Size(max = 500) String remark,
            @NotBlank(message = "请留下联系方式") @Size(max = 64) String contact) {
    }

    public record ReviewReq(
            @NotNull @Min(1) @Max(5) Integer score,
            @Size(max = 500) String content) {
    }

    /** acceptPenalty：要收违约金时，客户需要确认后才能取消 */
    public record CancelReq(@Size(max = 200) String reason, boolean acceptPenalty) {
        public static final CancelReq EMPTY = new CancelReq(null, false);
    }

    public record RescheduleReq(
            @NotNull(message = "请选择日期") LocalDate shootDate,
            @NotNull(message = "请选择开始时间") LocalTime startTime,
            @NotNull @Min(1) @Max(12) Integer hours) {
    }

    public record ReportReq(
            @NotBlank String targetType,
            @NotNull Long targetId,
            @NotBlank(message = "请选择举报原因") @Size(max = 32) String reason,
            @Size(max = 500) String content) {
    }

    public record OffReq(@NotNull LocalDate date, boolean off) {
    }
}
