package com.sheyinglai.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.sheyinglai.common.Result;
import com.sheyinglai.dto.BookingReqs;
import com.sheyinglai.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/** 举报摄影师、评价、客片 */
@SaCheckLogin
@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @PostMapping
    public Result<Void> create(@Valid @RequestBody BookingReqs.ReportReq req) {
        reportService.create(req.targetType(), req.targetId(), req.reason(), req.content());
        return Result.ok();
    }
}
