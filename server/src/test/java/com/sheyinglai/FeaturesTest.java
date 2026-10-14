package com.sheyinglai;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.sheyinglai.entity.Booking;
import com.sheyinglai.entity.Photographer;
import com.sheyinglai.mapper.BookingMapper;
import com.sheyinglai.mapper.PhotographerMapper;
import com.sheyinglai.service.BookingJobs;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** 订单自动流转、取消规则、改期、搜索、关注列表、注销、举报 */
class FeaturesTest extends ApiTestSupport {

    private final LocalDate tomorrow = LocalDate.now().plusDays(1);

    @Autowired
    private BookingMapper bookingMapper;
    @Autowired
    private PhotographerMapper photographerMapper;
    @Autowired
    private BookingJobs jobs;

    @Test
    void strictPolicyRequiresAcceptingPenalty() throws Exception {
        String[] c = register("客户");
        String[] p = photographer("摄影师");
        photographerMapper.update(null, new LambdaUpdateWrapper<Photographer>()
                .set(Photographer::getCancelPolicy, "STRICT").set(Photographer::getPenaltyPercent, 30)
                .eq(Photographer::getUserId, Long.valueOf(p[1])));
        long id = book(c[0], p[1], tomorrow, "10:00");
        ok("POST", "/bookings/" + id + "/confirm", null, p[0]);

        // 1 小时 × 200 元 × 30% = 60
        assertThat(ok("GET", "/bookings/" + id + "/cancel-preview", null, c[0]).get("penalty").asInt()).isEqualTo(60);
        assertThat(call("POST", "/bookings/" + id + "/cancel", Map.of("reason", "临时有事"), c[0]).get("code").asInt())
                .isEqualTo(409);
        JsonNode v = ok("POST", "/bookings/" + id + "/cancel", Map.of("reason", "临时有事", "acceptPenalty", true), c[0]);
        assertThat(v.at("/booking/status").asText()).isEqualTo("CANCELLED");
        assertThat(v.at("/booking/penaltyAmount").asInt()).isEqualTo(60);
        assertThat(v.at("/booking/cancelReason").asText()).isEqualTo("临时有事");
    }

    @Test
    void pendingCancelIsFreeEvenUnderStrictPolicy() throws Exception {
        String[] c = register("客户");
        String[] p = photographer("摄影师");
        photographerMapper.update(null, new LambdaUpdateWrapper<Photographer>()
                .set(Photographer::getCancelPolicy, "STRICT").set(Photographer::getPenaltyPercent, 30)
                .eq(Photographer::getUserId, Long.valueOf(p[1])));
        long id = book(c[0], p[1], tomorrow, "10:00");
        // 老版本前端不带请求体也能取消
        assertThat(ok("POST", "/bookings/" + id + "/cancel", null, c[0]).at("/booking/status").asText())
                .isEqualTo("CANCELLED");
    }

    @Test
    void rescheduleGoesBackToPendingAndOnlyCustomerCanDoIt() throws Exception {
        String[] c = register("客户");
        String[] p = photographer("摄影师");
        long id = book(c[0], p[1], tomorrow, "10:00");
        ok("POST", "/bookings/" + id + "/confirm", null, p[0]);

        Map<String, Object> req = Map.of("shootDate", tomorrow.plusDays(1).toString(), "startTime", "15:00", "hours", 2);
        assertThat(call("POST", "/bookings/" + id + "/reschedule", req, p[0]).get("code").asInt()).isEqualTo(403);
        JsonNode v = ok("POST", "/bookings/" + id + "/reschedule", req, c[0]);
        assertThat(v.at("/booking/status").asText()).isEqualTo("PENDING");
        assertThat(v.at("/booking/startTime").asText()).startsWith("15:00");
        assertThat(v.at("/booking/priceEstimate").asInt()).isEqualTo(400);
        assertThat(v.at("/booking/rescheduleCount").asInt()).isEqualTo(1);
    }

    @Test
    void jobsExpireCompleteAndRemind() throws Exception {
        String[] c = register("客户");
        String[] p = photographer("摄影师");
        long pending = book(c[0], p[1], tomorrow, "08:00");
        long confirmed = book(register("客户2")[0], p[1], tomorrow, "12:00");
        long remind = book(register("客户3")[0], p[1], tomorrow, "16:00");
        ok("POST", "/bookings/" + confirmed + "/confirm", null, p[0]);
        ok("POST", "/bookings/" + remind + "/confirm", null, p[0]);
        // 把前两个挪到过去：一个待确认已过拍摄时间，一个已确认且过了自动完成的天数
        moveTo(pending, LocalDate.now().minusDays(1));
        moveTo(confirmed, LocalDate.now().minusDays(5));

        jobs.expirePending();
        jobs.autoComplete();
        // 共用一个测试库，其它用例也可能有明天的预约，所以只断言「至少一个」和「不重复提醒」
        assertThat(jobs.remindTomorrow()).isPositive();
        assertThat(jobs.remindTomorrow()).isZero();

        assertThat(bookingMapper.selectById(pending).getStatus()).isEqualTo(Booking.EXPIRED);
        assertThat(bookingMapper.selectById(confirmed).getStatus()).isEqualTo(Booking.COMPLETED);
        assertThat(bookingMapper.selectById(remind).getReminded()).isTrue();
        // 详情页链接
        JsonNode msgs = ok("GET", "/me/notifications", null, c[0]).get("records");
        assertThat(msgs.get(0).get("link").asText()).isEqualTo("/orders/" + pending);
    }

    @Test
    void searchByKeywordAndPrice() throws Exception {
        String unique = "搜索测试" + System.nanoTime() % 100000;
        String[] p = photographer(unique);
        JsonNode r = ok("GET", "/photographers?keyword=" + unique, null, null);
        assertThat(r.get("total").asInt()).isEqualTo(1);
        assertThat(r.at("/records/0/id").asText()).isEqualTo(p[1]);
        JsonNode none = ok("GET", "/photographers?keyword=" + unique + "&maxPrice=100", null, null);
        assertThat(none.get("total").asInt()).isZero();
    }

    @Test
    void fansAndFollowingLists() throws Exception {
        String[] fan = register("粉丝");
        String[] p = photographer("摄影师");
        ok("POST", "/photographers/" + p[1] + "/follow", null, fan[0]);
        assertThat(ok("GET", "/photographers/" + p[1] + "/fans", null, null).at("/records/0/nickname").asText())
                .isEqualTo("粉丝");
        assertThat(ok("GET", "/photographers/" + fan[1] + "/following", null, null).at("/records/0/id").asText())
                .isEqualTo(p[1]);
    }

    @Test
    void deleteAccount() throws Exception {
        String[] c = register("要注销的人");
        String[] p = photographer("摄影师");
        long id = book(c[0], p[1], tomorrow, "10:00");
        // 有进行中的预约不能注销
        assertThat(call("POST", "/users/me/delete", Map.of("password", "test1234"), c[0]).get("code").asInt()).isEqualTo(400);
        ok("POST", "/bookings/" + id + "/cancel", null, c[0]);
        assertThat(call("POST", "/users/me/delete", Map.of("password", "wrong"), c[0]).get("code").asInt()).isEqualTo(400);
        ok("POST", "/users/me/delete", Map.of("password", "test1234"), c[0]);
        // token 失效，订单里显示为已注销用户
        assertThat(call("GET", "/me/notifications/unread-count", null, c[0]).get("code").asInt()).isEqualTo(401);
        assertThat(ok("GET", "/bookings/" + id, null, p[0]).at("/customer/nickname").asText()).isEqualTo("已注销用户");
    }

    @Test
    void reportAndHandle() throws Exception {
        String[] u = register("举报人");
        String[] p = photographer("被举报");
        ok("POST", "/reports", Map.of("targetType", "PHOTOGRAPHER", "targetId", Long.valueOf(p[1]), "reason", "虚假信息"), u[0]);
        assertThat(call("POST", "/reports", Map.of("targetType", "PHOTOGRAPHER", "targetId", Long.valueOf(p[1]),
                "reason", "虚假信息"), u[0]).get("code").asInt()).isEqualTo(400);

        String admin = ok("POST", "/auth/login", Map.of("phone", "13800000000", "password", "admin123"), null)
                .get("token").asText();
        JsonNode list = ok("GET", "/admin/reports?status=PENDING", null, admin);
        JsonNode mine = null;
        for (JsonNode r : list.get("records")) {
            if (r.at("/report/targetId").asText().equals(p[1])) mine = r;
        }
        assertThat(mine).isNotNull();
        assertThat(mine.at("/target/link").asText()).isEqualTo("/p/" + p[1]);
        ok("POST", "/admin/reports/" + mine.at("/report/id").asText() + "/handle", Map.of("status", "HANDLED"), admin);
        assertThat(ok("GET", "/me/notifications/unread-count", null, u[0]).asInt()).isEqualTo(1);
        // 普通用户不能看后台
        assertThat(call("GET", "/admin/reports", null, u[0]).get("code").asInt()).isEqualTo(403);
    }

    private void moveTo(long bookingId, LocalDate date) {
        bookingMapper.update(null, new LambdaUpdateWrapper<Booking>()
                .set(Booking::getShootDate, date).set(Booking::getStartTime, LocalTime.of(8, 0))
                .eq(Booking::getId, bookingId));
    }
}
