package com.sheyinglai.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.sheyinglai.common.Result;
import com.sheyinglai.dto.AuthReqs;
import com.sheyinglai.entity.User;
import com.sheyinglai.service.AccountService;
import com.sheyinglai.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@SaCheckLogin
@RestController
@RequestMapping("/users/me")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final AccountService accountService;

    @PutMapping
    public Result<User> updateProfile(@Valid @RequestBody AuthReqs.ProfileReq req) {
        return Result.ok(userService.updateProfile(req));
    }

    @PutMapping("/password")
    public Result<Void> changePassword(@Valid @RequestBody AuthReqs.PasswordReq req) {
        userService.changePassword(req);
        return Result.ok();
    }

    /** 注销账号，需要输入密码确认 */
    @PostMapping("/delete")
    public Result<Void> delete(@RequestBody AuthReqs.DeleteAccountReq req) {
        accountService.delete(req.password());
        return Result.ok();
    }

    @PostMapping("/avatar")
    public Result<User> avatar(@RequestParam("file") MultipartFile file) throws IOException {
        return Result.ok(userService.uploadAvatar(file));
    }
}
