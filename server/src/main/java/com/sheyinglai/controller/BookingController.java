package com.sheyinglai.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.sheyinglai.common.PageResult;
import com.sheyinglai.common.Result;
import com.sheyinglai.dto.BookingReqs;
import com.sheyinglai.dto.Views;
import com.sheyinglai.entity.Booking;
import com.sheyinglai.entity.Review;
import com.sheyinglai.service.BookingService;
import com.sheyinglai.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@SaCheckLogin
@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final ReviewService reviewService;

    @PostMapping
    public Result<Booking> create(@Valid @RequestBody BookingReqs.CreateReq req) {
        return Result.ok(bookingService.create(req));
    }

    /** role：customer 我约的 / photographer 约我的；status 可选 */
    @GetMapping
    public Result<PageResult<Views.BookingView>> list(@RequestParam(defaultValue = "customer") String role,
                                                      @RequestParam(required = false) String status,
                                                      @RequestParam(defaultValue = "1") long page,
                                                      @RequestParam(defaultValue = "20") long size) {
        return Result.ok(bookingService.list(role, status, page, size));
    }

    @PostMapping("/{id}/confirm")
    public Result<Views.BookingView> confirm(@PathVariable Long id) {
        return Result.ok(bookingService.confirm(id));
    }

    @PostMapping("/{id}/reject")
    public Result<Views.BookingView> reject(@PathVariable Long id) {
        return Result.ok(bookingService.reject(id));
    }

    @PostMapping("/{id}/cancel")
    public Result<Views.BookingView> cancel(@PathVariable Long id) {
        return Result.ok(bookingService.cancel(id));
    }

    @PostMapping("/{id}/complete")
    public Result<Views.BookingView> complete(@PathVariable Long id) {
        return Result.ok(bookingService.complete(id));
    }

    @PostMapping("/{id}/review")
    public Result<Review> review(@PathVariable Long id, @Valid @RequestBody BookingReqs.ReviewReq req) {
        return Result.ok(reviewService.create(id, req));
    }
}
