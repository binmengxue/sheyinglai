package com.sheyinglai.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.sheyinglai.common.PageResult;
import com.sheyinglai.common.Result;
import com.sheyinglai.dto.Views;
import com.sheyinglai.entity.User;
import com.sheyinglai.service.AdminService;
import com.sheyinglai.service.ReportService;
import com.sheyinglai.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** 管理员：用户列表、封禁、隐藏摄影师、删除评价。删除客片直接用 DELETE /albums/{id}（管理员不受归属限制） */
@SaCheckRole(User.ROLE_ADMIN)
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final ReviewService reviewService;
    private final ReportService reportService;

    @GetMapping("/users")
    public Result<PageResult<Views.AdminUserView>> users(@RequestParam(required = false) String keyword,
                                                         @RequestParam(defaultValue = "1") long page,
                                                         @RequestParam(defaultValue = "20") long size) {
        return Result.ok(adminService.users(keyword, page, size));
    }

    @PutMapping("/users/{id}/banned")
    public Result<Void> ban(@PathVariable Long id, @RequestParam boolean value) {
        adminService.setBanned(id, value);
        return Result.ok();
    }

    @PutMapping("/photographers/{id}/visible")
    public Result<Void> visible(@PathVariable Long id, @RequestParam boolean value) {
        adminService.setVisible(id, value);
        return Result.ok();
    }

    /** 举报列表，status 可选：PENDING / HANDLED / IGNORED */
    @GetMapping("/reports")
    public Result<PageResult<Views.ReportView>> reports(@RequestParam(required = false) String status,
                                                        @RequestParam(defaultValue = "1") long page,
                                                        @RequestParam(defaultValue = "20") long size) {
        return Result.ok(reportService.list(status, page, size));
    }

    @PostMapping("/reports/{id}/handle")
    public Result<Void> handleReport(@PathVariable Long id, @RequestBody Map<String, String> body) {
        reportService.handle(id, body.get("status"), body.get("note"));
        return Result.ok();
    }

    @DeleteMapping("/reviews/{id}")
    public Result<Void> deleteReview(@PathVariable Long id) {
        reviewService.delete(id);
        return Result.ok();
    }
}
