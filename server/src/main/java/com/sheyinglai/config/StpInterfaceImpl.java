package com.sheyinglai.config;

import cn.dev33.satoken.stp.StpInterface;
import com.sheyinglai.common.Auth;
import com.sheyinglai.entity.User;
import com.sheyinglai.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

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

    /** 一次请求里可能多次判断角色（canManage、isAdmin 等），结果缓存在请求属性里，只查一次库 */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        String key = "roles:" + loginId;
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs != null && attrs.getAttribute(key, RequestAttributes.SCOPE_REQUEST) instanceof List<?> cached) {
            @SuppressWarnings("unchecked") List<String> roles = (List<String>) cached;
            return roles;
        }
        Long id = Auth.parse(loginId);
        User user = id == null ? null : userMapper.selectById(id);
        List<String> roles = user == null ? List.of() : List.of(user.getRole());
        if (attrs != null) attrs.setAttribute(key, roles, RequestAttributes.SCOPE_REQUEST);
        return roles;
    }
}
