package com.sheyinglai.common;

import cn.dev33.satoken.stp.StpUtil;
import com.sheyinglai.entity.User;

/** 当前登录用户相关的小工具，loginId 就是 user.id */
public final class Auth {

    private Auth() {
    }

    /** 当前用户 id，未登录时抛 NotLoginException（返回 401） */
    public static Long uid() {
        StpUtil.checkLogin();
        Long id = parse(StpUtil.getLoginId());
        if (id == null) {
            // 升级前的旧 token（loginId 是用户名），作废后让前端重新登录
            StpUtil.logout();
            StpUtil.checkLogin();
        }
        return id;
    }

    /** 当前用户 id，未登录返回 null */
    public static Long uidOrNull() {
        return StpUtil.isLogin() ? parse(StpUtil.getLoginId()) : null;
    }

    public static boolean isAdmin() {
        return uidOrNull() != null && StpUtil.hasRole(User.ROLE_ADMIN);
    }

    /** 是不是本人或管理员 */
    public static boolean canManage(Long ownerId) {
        Long uid = uidOrNull();
        return uid != null && (uid.equals(ownerId) || isAdmin());
    }

    public static void checkOwner(Long ownerId) {
        if (!canManage(ownerId)) {
            throw new BizException(403, "无权操作");
        }
    }

    public static Long parse(Object loginId) {
        try {
            return Long.valueOf(String.valueOf(loginId));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
