package com.sheyinglai.config;

import com.sheyinglai.chat.ChatSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketMessage;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** 私信推送：/api/ws/chat（加上 context-path） */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    /** 单条推送最多等 10 秒、积压 512KB，超过就断开这个慢连接，避免拖住发消息的请求 */
    private static final int SEND_TIME_LIMIT_MS = 10_000;
    private static final int BUFFER_LIMIT = 512 * 1024;

    private final ChatSocketHandler chatSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 鉴权靠连接后的第一条 auth 消息，不依赖 Cookie，所以不限制来源
        registry.addHandler(new ConcurrentSend(chatSocketHandler), "/ws/chat").setAllowedOriginPatterns("*");
    }

    /**
     * WebSocketSession.sendMessage 不能多线程同时调用（两个人同时给一个人发消息时会出现），
     * 这里把每个连接包一层 ConcurrentWebSocketSessionDecorator，后面的代码拿到的都是线程安全的连接。
     */
    private static class ConcurrentSend extends WebSocketHandlerDecorator {
        private final Map<String, WebSocketSession> wrapped = new ConcurrentHashMap<>();

        ConcurrentSend(WebSocketHandler delegate) {
            super(delegate);
        }

        private WebSocketSession wrap(WebSocketSession s) {
            return wrapped.computeIfAbsent(s.getId(),
                    k -> new ConcurrentWebSocketSessionDecorator(s, SEND_TIME_LIMIT_MS, BUFFER_LIMIT));
        }

        @Override
        public void afterConnectionEstablished(WebSocketSession session) throws Exception {
            super.afterConnectionEstablished(wrap(session));
        }

        @Override
        public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
            super.handleMessage(wrap(session), message);
        }

        @Override
        public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
            super.handleTransportError(wrap(session), exception);
        }

        @Override
        public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
            WebSocketSession s = wrapped.remove(session.getId());
            super.afterConnectionClosed(s != null ? s : session, closeStatus);
        }
    }
}
