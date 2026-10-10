package com.sheyinglai.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.sheyinglai.common.BizException;
import com.sheyinglai.common.Result;
import com.sheyinglai.config.AppProperties;
import com.sheyinglai.dto.LoginReq;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AppProperties props;

    @PostMapping("/login")
    public Result<Map<String, String>> login(@Valid @RequestBody LoginReq req) {
        AppProperties.Admin admin = props.getAdmin();
        if (!same(req.username(), admin.getUsername()) || !same(req.password(), admin.getPassword())) {
            throw new BizException("账号或密码错误");
        }
        StpUtil.login(admin.getUsername());
        return Result.ok(Map.of("token", StpUtil.getTokenValue(), "username", admin.getUsername()));
    }

    @GetMapping("/me")
    public Result<Map<String, Object>> me() {
        boolean login = StpUtil.isLogin();
        return Result.ok(Map.of("loggedIn", login, "username", login ? StpUtil.getLoginIdAsString() : ""));
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        StpUtil.logout();
        return Result.ok();
    }

    private static boolean same(String a, String b) {
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
