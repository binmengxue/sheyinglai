package com.sheyinglai.chat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 在线的私信连接：userId -> 该用户所有打开的页面（手机、电脑可同时在线）。
 * 连接放在内存里，单实例部署够用；以后多实例时改成 Redis 发布订阅。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatHub {

    private final ObjectMapper objectMapper;
    private final Map<Long, Set<WebSocketSession>> online = new ConcurrentHashMap<>();

    public void add(Long uid, WebSocketSession s) {
        online.computeIfAbsent(uid, k -> ConcurrentHashMap.newKeySet()).add(s);
    }

    public void remove(Long uid, WebSocketSession s) {
        online.computeIfPresent(uid, (k, set) -> {
            set.remove(s);
            return set.isEmpty() ? null : set;
        });
    }

    /** 推给某人的所有在线页面；不在线就算了，消息已经存库，下次打开时能看到 */
    public void push(Long uid, Object payload) {
        Set<WebSocketSession> sessions = online.get(uid);
        if (sessions == null || sessions.isEmpty()) return;
        TextMessage msg;
        try {
            msg = new TextMessage(objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException e) {
            log.warn("私信推送序列化失败", e);
            return;
        }
        for (WebSocketSession s : sessions) {
            try {
                if (s.isOpen()) s.sendMessage(msg);
            } catch (IOException | IllegalStateException e) {
                // 连接已断开，等 afterConnectionClosed 清理
                log.debug("私信推送失败 uid={}", uid, e);
            }
        }
    }
}
