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

    public record OffReq(@NotNull LocalDate date, boolean off) {
    }
}
