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
    /** 新密码：8~32 位，同时包含字母和数字（登录不校验，老用户的短密码仍能登录） */
    public static final String PASSWORD_REGEX = "^(?=.*[A-Za-z])(?=.*\\d).{8,32}$";
    public static final String PASSWORD_MSG = "密码 8~32 位，需同时包含字母和数字";

    public record LoginReq(
            @NotBlank @Pattern(regexp = PHONE_REGEX, message = "手机号格式不正确") String phone,
            @NotBlank String password) {
    }

    public record RegisterReq(
            @NotBlank @Pattern(regexp = PHONE_REGEX, message = "手机号格式不正确") String phone,
            @NotBlank @Pattern(regexp = PASSWORD_REGEX, message = PASSWORD_MSG) String password,
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

    public record DeleteAccountReq(String password) {
    }

    public record PasswordReq(
            @NotBlank String oldPassword,
            @NotBlank @Pattern(regexp = PASSWORD_REGEX, message = PASSWORD_MSG) String newPassword) {
    }
}
