package com.sheyinglai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

/** 接口测试基类：内存 H2 + MockMvc，通过 HTTP 接口走完整流程 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:apitest;MODE=MySQL;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1",
        "app.storage.local.dir=target/test-uploads",
})
@AutoConfigureMockMvc
public abstract class ApiTestSupport {

    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired
    protected MockMvc mvc;
    @Autowired
    protected ObjectMapper json;

    /** 发请求，返回整个 Result（code/msg/data） */
    protected JsonNode call(String method, String path, Object body, String token) throws Exception {
        int n = SEQ.incrementAndGet();
        MockHttpServletRequestBuilder req = request(HttpMethod.valueOf(method), path)
                // 每个请求用不同的 IP，避免测试之间互相触发限流
                .header("X-Real-IP", "10.0." + (n / 250 % 250) + "." + (n % 250));
        if (token != null) req.header("Authorization", "Bearer " + token);
        if (body != null) req.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(body));
        String text = mvc.perform(req).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return json.readTree(text);
    }

    protected JsonNode ok(String method, String path, Object body, String token) throws Exception {
        JsonNode r = call(method, path, body, token);
        if (r.get("code").asInt() != 0) throw new AssertionError(path + " -> " + r);
        return r.get("data");
    }

    /** 注册一个新用户，返回 [token, userId] */
    protected String[] register(String nickname) throws Exception {
        String phone = "13" + String.format("%09d", SEQ.incrementAndGet() * 7919L % 1_000_000_000L);
        JsonNode d = ok("POST", "/auth/register",
                Map.of("phone", phone, "password", "test1234", "nickname", nickname, "agree", true), null);
        return new String[]{d.get("token").asText(), d.at("/me/user/id").asText()};
    }

    /** 注册并设置价格，成为可预约的摄影师（全天接单） */
    protected String[] photographer(String nickname) throws Exception {
        String[] p = register(nickname);
        ok("PUT", "/photographers/me", Map.of("pricePerHour", 200, "minHours", 1, "workDays", "ALL",
                "workStart", "00:00", "workEnd", "23:30"), p[0]);
        return p;
    }

    protected long book(String customerToken, String photographerId, LocalDate date, String start) throws Exception {
        return ok("POST", "/bookings", Map.of("photographerId", Long.valueOf(photographerId), "shootDate", date.toString(),
                "startTime", start, "hours", 1, "people", 1, "contact", "wx_test"), customerToken).get("id").asLong();
    }
}
