package com.sheyinglai.common;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 简单的固定窗口限流：key 在 window 时间内最多 max 次，超过抛 429。
 * 计数在内存里，单实例部署够用；以后多实例时换成 Redis。
 */
@Component
public class RateLimiter {

    private record Window(long start, int count) {
    }

    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    public void check(String key, int max, Duration window, String msg) {
        long now = System.currentTimeMillis();
        long ms = window.toMillis();
        Window w = windows.compute(key, (k, old) ->
                old == null || now - old.start() >= ms ? new Window(now, 1) : new Window(old.start(), old.count() + 1));
        if (windows.size() > 50_000) {
            // 防止被大量不同 key 撑大内存：清掉过期的窗口（按最长 1 天算）
            windows.entrySet().removeIf(e -> now - e.getValue().start() > Duration.ofDays(1).toMillis());
        }
        if (w.count() > max) {
            throw new BizException(429, msg);
        }
    }

    /** 客户端 IP：线上经过 Nginx，取它设置的 X-Real-IP */
    public static String clientIp() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs)) return "unknown";
        HttpServletRequest req = attrs.getRequest();
        String ip = req.getHeader("X-Real-IP");
        return ip != null && !ip.isBlank() ? ip : req.getRemoteAddr();
    }
}
