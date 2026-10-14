package com.sheyinglai.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sheyinglai.common.Auth;
import com.sheyinglai.common.BizException;
import com.sheyinglai.common.PageResult;
import com.sheyinglai.dto.Views;
import com.sheyinglai.entity.Photographer;
import com.sheyinglai.entity.User;
import com.sheyinglai.mapper.PhotographerMapper;
import com.sheyinglai.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 入驻不审核，管理员事后处置：封禁账号、隐藏摄影师；删除客片和评价复用原有接口 */
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserMapper userMapper;
    private final PhotographerMapper photographerMapper;

    public PageResult<Views.AdminUserView> users(String keyword, long page, long size) {
        Page<User> p = userMapper.selectPage(Page.of(Math.max(1, page), Math.max(1, Math.min(size, 100))),
                new LambdaQueryWrapper<User>()
                        .and(StringUtils.hasText(keyword), w -> w.like(User::getPhone, keyword)
                                .or().like(User::getNickname, keyword))
                        .orderByDesc(User::getId));
        List<Long> ids = p.getRecords().stream().map(User::getId).toList();
        Map<Long, Photographer> ps = ids.isEmpty() ? Map.of() : photographerMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(Photographer::getUserId, Function.identity()));
        List<Views.AdminUserView> views = p.getRecords().stream()
                .map(u -> new Views.AdminUserView(u, ps.get(u.getId()))).toList();
        return new PageResult<>(views, p.getTotal(), p.getCurrent(), p.getSize());
    }

    public void setBanned(Long userId, boolean banned) {
        if (userId.equals(Auth.uid())) {
            throw new BizException("不能封禁自己");
        }
        User u = userMapper.selectById(userId);
        if (u != null && User.STATUS_DELETED.equals(u.getStatus())) {
            throw new BizException("该账号已注销");
        }
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .set(User::getStatus, banned ? User.STATUS_BANNED : User.STATUS_NORMAL)
                .eq(User::getId, userId));
        if (banned) {
            StpUtil.logout(userId);
        }
    }

    public void setVisible(Long photographerId, boolean visible) {
        photographerMapper.update(null, new LambdaUpdateWrapper<Photographer>()
                .set(Photographer::getVisible, visible).eq(Photographer::getUserId, photographerId));
    }
}
