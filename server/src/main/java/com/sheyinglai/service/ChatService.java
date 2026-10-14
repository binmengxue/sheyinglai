package com.sheyinglai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.sheyinglai.chat.ChatHub;
import com.sheyinglai.common.Auth;
import com.sheyinglai.common.BizException;
import com.sheyinglai.common.RateLimiter;
import com.sheyinglai.dto.Views;
import com.sheyinglai.entity.ChatConversation;
import com.sheyinglai.entity.ChatMessage;
import com.sheyinglai.entity.User;
import com.sheyinglai.mapper.ChatConversationMapper;
import com.sheyinglai.mapper.ChatMessageMapper;
import com.sheyinglai.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** 私信（聊一聊）：消息先存库，再通过 WebSocket 推给双方在线的页面 */
@Service
@RequiredArgsConstructor
public class ChatService {

    public static final int MAX_TEXT = 500;
    private static final int PREVIEW_LEN = 60;

    private final ChatConversationMapper conversationMapper;
    private final ChatMessageMapper messageMapper;
    private final UserService userService;
    private final StorageService storage;
    private final RateLimiter rateLimiter;
    private final ChatHub hub;

    /** 服务端推给前端的新消息 */
    public record Push(String type, Long conversationId, ChatMessage message, Views.UserBrief sender) {
    }

    /** 找到或创建和 peerId 的会话 */
    public Views.ChatConversationView open(Long peerId) {
        Long uid = Auth.uid();
        if (uid.equals(peerId)) {
            throw new BizException("不能和自己聊天");
        }
        checkPeer(peerId);
        long a = Math.min(uid, peerId);
        long b = Math.max(uid, peerId);
        ChatConversation c = findPair(a, b);
        if (c == null) {
            c = new ChatConversation();
            c.setUserA(a);
            c.setUserB(b);
            c.setUnreadA(0);
            c.setUnreadB(0);
            c.setCreatedAt(LocalDateTime.now());
            try {
                conversationMapper.insert(c);
            } catch (DuplicateKeyException e) {
                // 双方同时点「聊一聊」
                c = findPair(a, b);
            }
        }
        return view(c, uid);
    }

    public Views.ChatConversationView get(Long id) {
        Long uid = Auth.uid();
        return view(mine(id, uid), uid);
    }

    /** 我的会话，最近有消息的在前；还没发过消息的空会话不显示 */
    public List<Views.ChatConversationView> list() {
        Long uid = Auth.uid();
        List<ChatConversation> list = conversationMapper.selectList(new LambdaQueryWrapper<ChatConversation>()
                .and(w -> w.eq(ChatConversation::getUserA, uid).or().eq(ChatConversation::getUserB, uid))
                .isNotNull(ChatConversation::getLastAt)
                .orderByDesc(ChatConversation::getLastAt)
                .last("LIMIT 200"));
        Map<Long, Views.UserBrief> users = userService.briefs(list.stream().map(c -> c.peerOf(uid)).toList());
        return list.stream().map(c -> new Views.ChatConversationView(c.getId(),
                users.getOrDefault(c.peerOf(uid), Views.UserBrief.of(null)),
                c.getLastMsg(), c.getLastAt(), c.unreadOf(uid))).toList();
    }

    /** 历史消息：beforeId 之前的 size 条，按时间正序返回 */
    public List<ChatMessage> history(Long id, Long beforeId, int size) {
        mine(id, Auth.uid());
        List<ChatMessage> list = new ArrayList<>(messageMapper.selectList(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getConversationId, id)
                .lt(beforeId != null, ChatMessage::getId, beforeId)
                .orderByDesc(ChatMessage::getId)
                .last("LIMIT " + Math.max(1, Math.min(size, 50)))));
        Collections.reverse(list);
        return list;
    }

    @Transactional
    public ChatMessage sendText(Long id, String text) {
        String content = text == null ? "" : text.strip();
        if (content.isEmpty()) {
            throw new BizException("消息不能为空");
        }
        if (content.length() > MAX_TEXT) {
            throw new BizException("消息不能超过 " + MAX_TEXT + " 字");
        }
        return send(id, ChatMessage.TYPE_TEXT, content);
    }

    @Transactional
    public ChatMessage sendImage(Long id, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new BizException("请选择图片");
        }
        // 先校验会话和频率，不合法的请求不落文件
        Long uid = Auth.uid();
        checkPeer(mine(id, uid).peerOf(uid));
        limit(uid);
        return send(id, ChatMessage.TYPE_IMAGE, storage.store(file).url());
    }

    public void read(Long id) {
        Long uid = Auth.uid();
        conversationMapper.update(null, new LambdaUpdateWrapper<ChatConversation>()
                .set(ChatConversation::getUnreadA, 0).eq(ChatConversation::getId, id).eq(ChatConversation::getUserA, uid));
        conversationMapper.update(null, new LambdaUpdateWrapper<ChatConversation>()
                .set(ChatConversation::getUnreadB, 0).eq(ChatConversation::getId, id).eq(ChatConversation::getUserB, uid));
    }

    public long unreadCount() {
        return conversationMapper.unreadTotal(Auth.uid());
    }

    private ChatMessage send(Long id, String type, String content) {
        Long uid = Auth.uid();
        ChatConversation c = mine(id, uid);
        Long peerId = c.peerOf(uid);
        checkPeer(peerId);
        if (ChatMessage.TYPE_TEXT.equals(type)) limit(uid);

        ChatMessage m = new ChatMessage();
        m.setConversationId(id);
        m.setSenderId(uid);
        m.setType(type);
        m.setContent(content);
        m.setCreatedAt(LocalDateTime.now());
        messageMapper.insert(m);

        String preview = ChatMessage.TYPE_IMAGE.equals(type) ? "[图片]"
                : content.length() > PREVIEW_LEN ? content.substring(0, PREVIEW_LEN) + "…" : content;
        boolean peerIsA = peerId.equals(c.getUserA());
        conversationMapper.update(null, new LambdaUpdateWrapper<ChatConversation>()
                .set(ChatConversation::getLastMsg, preview)
                .set(ChatConversation::getLastAt, m.getCreatedAt())
                .setSql(peerIsA ? "unread_a = unread_a + 1" : "unread_b = unread_b + 1")
                .eq(ChatConversation::getId, id));

        // 事务提交后再推送，避免对方收到推送去查库时还查不到
        Push push = new Push("message", id, m, Views.UserBrief.of(userService.get(uid)));
        afterCommit(() -> {
            hub.push(peerId, push);
            hub.push(uid, push); // 自己的其它页面（手机和电脑同时登录）也同步
        });
        return m;
    }

    private void limit(Long uid) {
        rateLimiter.check("chat:" + uid, 30, Duration.ofMinutes(1), "发送太频繁，请稍后再试");
    }

    /** 对方账号被封禁或已注销时不能再发消息 */
    private void checkPeer(Long peerId) {
        User peer = userService.get(peerId);
        if (peer == null || !Objects.equals(User.STATUS_NORMAL, peer.getStatus())) {
            throw new BizException("对方账号不可用，无法发送消息");
        }
    }

    private ChatConversation findPair(long a, long b) {
        return conversationMapper.selectOne(new LambdaQueryWrapper<ChatConversation>()
                .eq(ChatConversation::getUserA, a).eq(ChatConversation::getUserB, b));
    }

    /** 只能看自己参与的会话；别人的会话也报「不存在」，不暴露会话 id 是否有效 */
    private ChatConversation mine(Long id, Long uid) {
        ChatConversation c = id == null ? null : conversationMapper.selectById(id);
        if (c == null || !c.has(uid)) {
            throw new BizException(404, "会话不存在");
        }
        return c;
    }

    private Views.ChatConversationView view(ChatConversation c, Long uid) {
        User peer = userService.get(c.peerOf(uid));
        return new Views.ChatConversationView(c.getId(), Views.UserBrief.of(peer), c.getLastMsg(), c.getLastAt(),
                c.unreadOf(uid));
    }

    private static void afterCommit(Runnable r) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            r.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                r.run();
            }
        });
    }
}
