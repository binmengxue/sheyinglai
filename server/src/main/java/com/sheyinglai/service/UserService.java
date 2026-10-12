package com.sheyinglai.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.sheyinglai.common.Auth;
import com.sheyinglai.common.BizException;
import com.sheyinglai.dto.AuthReqs;
import com.sheyinglai.dto.Views;
import com.sheyinglai.entity.User;
import com.sheyinglai.mapper.UserMapper;
import com.sheyinglai.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    /** 头像上限和照片一致：1MB */
    private static final long MAX_AVATAR_BYTES = 1024 * 1024;

    private final UserMapper userMapper;
    private final PhotographerService photographerService;
    private final StorageService storage;
    private final PasswordEncoder encoder;

    /** 注册即成为摄影师（按需求不审核），同时完成登录 */
    @Transactional
    public Views.LoginResult register(AuthReqs.RegisterReq req) {
        if (userMapper.exists(new LambdaQueryWrapper<User>().eq(User::getPhone, req.phone()))) {
            throw new BizException("该手机号已注册，请直接登录");
        }
        User user = new User();
        user.setPhone(req.phone());
        user.setPasswordHash(encoder.encode(req.password()));
        user.setNickname(StringUtils.hasText(req.nickname()) ? req.nickname().trim()
                : "摄影来" + req.phone().substring(req.phone().length() - 4));
        user.setGender(0);
        user.setRole(User.ROLE_USER);
        user.setStatus(User.STATUS_NORMAL);
        user.setCreatedAt(LocalDateTime.now());
        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException e) {
            throw new BizException("该手机号已注册，请直接登录");
        }
        photographerService.ensure(user.getId());
        StpUtil.login(user.getId());
        return new Views.LoginResult(StpUtil.getTokenValue(), me());
    }

    public Views.LoginResult login(AuthReqs.LoginReq req) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getPhone, req.phone()));
        if (user == null || !encoder.matches(req.password(), user.getPasswordHash())) {
            throw new BizException("手机号或密码错误");
        }
        if (User.STATUS_BANNED.equals(user.getStatus())) {
            throw new BizException(403, "账号已被封禁，如有疑问请联系管理员");
        }
        StpUtil.login(user.getId());
        return new Views.LoginResult(StpUtil.getTokenValue(), me());
    }

    public Views.Me me() {
        Long uid = Auth.uidOrNull();
        User user = uid == null ? null : userMapper.selectById(uid);
        if (user == null || User.STATUS_BANNED.equals(user.getStatus())) {
            if (StpUtil.isLogin()) StpUtil.logout();
            return new Views.Me(false, null, null, false);
        }
        return new Views.Me(true, user, photographerService.ensure(user.getId()),
                User.ROLE_ADMIN.equals(user.getRole()));
    }

    public User updateProfile(AuthReqs.ProfileReq req) {
        Long uid = Auth.uid();
        // 用 UpdateWrapper 才能把选填项清空（updateById 会忽略 null）
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .set(User::getNickname, req.nickname().trim())
                .set(User::getGender, req.gender() == null ? 0 : req.gender())
                .set(User::getBio, blankToNull(req.bio()))
                .set(User::getCity, blankToNull(req.city()))
                .set(User::getWechat, blankToNull(req.wechat()))
                .eq(User::getId, uid));
        return userMapper.selectById(uid);
    }

    public void changePassword(AuthReqs.PasswordReq req) {
        User user = userMapper.selectById(Auth.uid());
        if (!encoder.matches(req.oldPassword(), user.getPasswordHash())) {
            throw new BizException("原密码不正确");
        }
        user.setPasswordHash(encoder.encode(req.newPassword()));
        userMapper.updateById(user);
    }

    /** 旧头像文件不删除：头像没记录存储 key，且量很小 */
    public User uploadAvatar(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new BizException("请选择图片");
        }
        if (file.getSize() > MAX_AVATAR_BYTES) {
            throw new BizException("图片不可以超过 1M");
        }
        Long uid = Auth.uid();
        User user = userMapper.selectById(uid);
        user.setAvatarUrl(storage.store(file).thumbUrl());
        userMapper.updateById(user);
        return user;
    }

    public User get(Long id) {
        return userMapper.selectById(id);
    }

    public Map<Long, User> mapByIds(Collection<Long> ids) {
        if (ids.isEmpty()) return Map.of();
        return userMapper.selectBatchIds(ids).stream().collect(Collectors.toMap(User::getId, Function.identity()));
    }

    public Map<Long, Views.UserBrief> briefs(Collection<Long> ids) {
        return mapByIds(ids).values().stream().collect(Collectors.toMap(User::getId, Views.UserBrief::of));
    }

    /** 给客户看的摄影师联系方式：优先微信号，没填就给手机号 */
    public static String contactOf(User u) {
        if (u == null) return null;
        return StringUtils.hasText(u.getWechat()) ? "微信：" + u.getWechat() : "手机：" + u.getPhone();
    }

    private static String blankToNull(String s) {
        return StringUtils.hasText(s) ? s.trim() : null;
    }
}
