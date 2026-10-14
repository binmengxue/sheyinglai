package com.sheyinglai.chat;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sheyinglai.common.Auth;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;

/**
 * 私信推送通道（/api/ws/chat）。只负责服务端往下推，发消息走普通 HTTP 接口。
 * 连上后第一条消息必须是 {"type":"auth","token":"..."}：浏览器建 WebSocket 不能带请求头，
 * 又不想把 token 放进 URL（会出现在 Nginx 访问日志里）。
 * 客户端定时发 {"type":"ping"} 保活，服务端回 {"type":"pong"}。
 */
@Component
@RequiredArgsConstructor
public class ChatSocketHandler extends TextWebSocketHandler {

    private static final String UID = "uid";
    private static final TextMessage PONG = new TextMessage("{\"type\":\"pong\"}");
    private static final TextMessage AUTH_OK = new TextMessage("{\"type\":\"auth_ok\"}");

    private final ChatHub hub;
    private final ObjectMapper objectMapper;

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        JsonNode body;
        try {
            body = objectMapper.readTree(message.getPayload());
        } catch (IOException e) {
            session.close(CloseStatus.BAD_DATA);
            return;
        }
        String type = body.path("type").asText();
        Long uid = (Long) session.getAttributes().get(UID);
        if (uid == null) {
            auth(session, type, body.path("token").asText(null));
        } else if ("ping".equals(type)) {
            session.sendMessage(PONG);
        }
    }

    private void auth(WebSocketSession session, String type, String token) throws IOException {
        Long uid = "auth".equals(type) && token != null ? Auth.parse(StpUtil.getLoginIdByToken(token)) : null;
        if (uid == null) {
            // 4401：前端据此判断登录失效，不再重连
            session.close(new CloseStatus(4401, "unauthorized"));
            return;
        }
        Map<String, Object> attrs = session.getAttributes();
        attrs.put(UID, uid);
        hub.add(uid, session);
        session.sendMessage(AUTH_OK);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long uid = (Long) session.getAttributes().get(UID);
        if (uid != null) hub.remove(uid, session);
    }
}
