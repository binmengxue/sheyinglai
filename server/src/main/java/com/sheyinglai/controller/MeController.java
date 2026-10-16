package com.sheyinglai.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.sheyinglai.common.Auth;
import com.sheyinglai.common.BizException;
import com.sheyinglai.common.PageResult;
import com.sheyinglai.common.Result;
import com.sheyinglai.dto.Views;
import com.sheyinglai.entity.Album;
import com.sheyinglai.entity.User;
import com.sheyinglai.service.ReviewService;
import com.sheyinglai.service.SocialService;
import com.sheyinglai.service.StatService;
import com.sheyinglai.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;

/** 「我的」页面：喜欢的客片、我写的点评、经营数据 */
@SaCheckLogin
@RestController
@RequestMapping("/me")
@RequiredArgsConstructor
public class MeController {

    /** 简历只给这个手机号的账号看 */
    private static final String RESUME_PHONE = "18701309020";

    private final SocialService socialService;
    private final ReviewService reviewService;
    private final StatService statService;
    private final UserService userService;

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

    @GetMapping("/resume")
    public ResponseEntity<Resource> resume() {
        User user = userService.get(Auth.uid());
        if (user == null || !RESUME_PHONE.equals(user.getPhone())) {
            throw new BizException(403, "无权查看");
        }
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename("刘雪琴-高级前端工程师-简历.pdf", StandardCharsets.UTF_8).build().toString())
                .body(new ClassPathResource("private/resume.pdf"));
    }
}
