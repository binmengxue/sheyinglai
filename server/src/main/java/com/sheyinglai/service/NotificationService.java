package com.sheyinglai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sheyinglai.common.Auth;
import com.sheyinglai.common.PageResult;
import com.sheyinglai.entity.Notification;
import com.sheyinglai.entity.User;
import com.sheyinglai.mapper.NotificationMapper;
import com.sheyinglai.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/** 站内消息。只依赖 mapper，避免和 BookingService 等互相依赖 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationMapper notificationMapper;
    private final UserMapper userMapper;

    /** 发给 userId；接收人为空或就是当前操作人时不发 */
    public void send(Long userId, String type, String title, String content, String link) {
        if (userId == null || userId.equals(Auth.uidOrNull())) return;
        Notification n = new Notification();
        n.setUserId(userId);
        n.setType(type);
        n.setTitle(title);
        n.setContent(content);
        n.setLink(link);
        n.setIsRead(false);
        n.setCreatedAt(LocalDateTime.now());
        notificationMapper.insert(n);
    }

    /** 拼消息文案用 */
    public String nickname(Long userId) {
        User u = userId == null ? null : userMapper.selectById(userId);
        return u == null ? "有用户" : u.getNickname();
    }

    public PageResult<Notification> list(long page, long size) {
        Page<Notification> p = notificationMapper.selectPage(Page.of(Math.max(1, page), Math.max(1, Math.min(size, 50))),
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getUserId, Auth.uid())
                        .orderByDesc(Notification::getId));
        return PageResult.of(p);
    }

    public long unreadCount() {
        return notificationMapper.selectCount(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, Auth.uid()).eq(Notification::getIsRead, false));
    }

    public void read(Long id) {
        notificationMapper.update(null, new LambdaUpdateWrapper<Notification>()
                .set(Notification::getIsRead, true)
                .eq(Notification::getId, id).eq(Notification::getUserId, Auth.uid()));
    }

    public void readAll() {
        notificationMapper.update(null, new LambdaUpdateWrapper<Notification>()
                .set(Notification::getIsRead, true)
                .eq(Notification::getUserId, Auth.uid()).eq(Notification::getIsRead, false));
    }
}
