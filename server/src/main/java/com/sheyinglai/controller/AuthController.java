package com.sheyinglai.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.sheyinglai.common.Result;
import com.sheyinglai.dto.AuthReqs;
import com.sheyinglai.dto.Views;
import com.sheyinglai.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    /** 手机号 + 密码注册，注册后自动登录 */
    @PostMapping("/register")
    public Result<Views.LoginResult> register(@Valid @RequestBody AuthReqs.RegisterReq req) {
        return Result.ok(userService.register(req));
    }

    @PostMapping("/login")
    public Result<Views.LoginResult> login(@Valid @RequestBody AuthReqs.LoginReq req) {
        return Result.ok(userService.login(req));
    }

    /** 当前登录用户、摄影师资料、是否管理员 */
    @GetMapping("/me")
    public Result<Views.Me> me() {
        return Result.ok(userService.me());
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        StpUtil.logout();
        return Result.ok();
    }
}
