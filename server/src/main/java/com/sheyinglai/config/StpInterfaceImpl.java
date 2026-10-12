package com.sheyinglai.config;

import cn.dev33.satoken.stp.StpInterface;
import com.sheyinglai.common.Auth;
import com.sheyinglai.entity.User;
import com.sheyinglai.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/** 给 Sa-Token 提供角色，@SaCheckRole("ADMIN") 和 StpUtil.hasRole 会用到 */
@Component
@RequiredArgsConstructor
public class StpInterfaceImpl implements StpInterface {

    private final UserMapper userMapper;

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return List.of();
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        Long id = Auth.parse(loginId);
        User user = id == null ? null : userMapper.selectById(id);
        return user == null ? List.of() : List.of(user.getRole());
    }
}
