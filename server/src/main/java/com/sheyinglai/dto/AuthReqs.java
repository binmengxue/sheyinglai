package com.sheyinglai.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 登录、注册、个人资料的请求体 */
public final class AuthReqs {

    private AuthReqs() {
    }

    public static final String PHONE_REGEX = "^1\\d{10}$";

    public record LoginReq(
            @NotBlank @Pattern(regexp = PHONE_REGEX, message = "手机号格式不正确") String phone,
            @NotBlank String password) {
    }

    public record RegisterReq(
            @NotBlank @Pattern(regexp = PHONE_REGEX, message = "手机号格式不正确") String phone,
            @NotBlank @Size(min = 6, max = 32, message = "密码长度 6~32 位") String password,
            @Size(max = 32) String nickname,
            @AssertTrue(message = "请先阅读并同意用户协议和隐私政策") boolean agree) {
    }

    public record ProfileReq(
            @NotBlank @Size(max = 32) String nickname,
            @Min(0) @Max(2) Integer gender,
            @Size(max = 500) String bio,
            @Size(max = 32) String city,
            @Size(max = 64) String wechat) {
    }

    public record PasswordReq(
            @NotBlank String oldPassword,
            @NotBlank @Size(min = 6, max = 32, message = "密码长度 6~32 位") String newPassword) {
    }
}
