package com.sheyinglai.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.sheyinglai.common.PageResult;
import com.sheyinglai.common.Result;
import com.sheyinglai.dto.Views;
import com.sheyinglai.entity.Album;
import com.sheyinglai.service.ReviewService;
import com.sheyinglai.service.SocialService;
import com.sheyinglai.service.StatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 「我的」页面：喜欢的客片、我写的点评、经营数据 */
@SaCheckLogin
@RestController
@RequestMapping("/me")
@RequiredArgsConstructor
public class MeController {

    private final SocialService socialService;
    private final ReviewService reviewService;
    private final StatService statService;

    @GetMapping("/likes")
    public Result<List<Album>> likes() {
        return Result.ok(socialService.myLikes());
    }

    @GetMapping("/reviews")
    public Result<PageResult<Views.ReviewView>> reviews(@RequestParam(defaultValue = "1") long page,
                                                        @RequestParam(defaultValue = "20") long size) {
        return Result.ok(reviewService.mine(page, size));
    }

    @GetMapping("/stats")
    public Result<Views.StatsView> stats() {
        return Result.ok(statService.mine());
    }

    @PostMapping("/likes/{albumId}")
    public Result<Void> like(@PathVariable Long albumId) {
        socialService.like(albumId);
        return Result.ok();
    }

    @DeleteMapping("/likes/{albumId}")
    public Result<Void> unlike(@PathVariable Long albumId) {
        socialService.unlike(albumId);
        return Result.ok();
    }
}
