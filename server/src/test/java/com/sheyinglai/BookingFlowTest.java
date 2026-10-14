package com.sheyinglai;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.sheyinglai.entity.Booking;
import com.sheyinglai.mapper.BookingMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

class BookingFlowTest extends ApiTestSupport {

    private final LocalDate tomorrow = LocalDate.now().plusDays(1);

    @Autowired
    private BookingMapper bookingMapper;

    @Test
    void statusFlowAndNotifications() throws Exception {
        String[] c = register("客户");
        String[] p = photographer("摄影师");
        long id = book(c[0], p[1], tomorrow, "10:00");

        assertThat(unread(p[0])).isEqualTo(1);
        ok("POST", "/bookings/" + id + "/confirm", null, p[0]);
        // 拍摄日之后才能标记完成：直接把日期改到昨天
        bookingMapper.update(null, new LambdaUpdateWrapper<Booking>()
                .set(Booking::getShootDate, LocalDate.now().minusDays(1)).eq(Booking::getId, id));
        ok("POST", "/bookings/" + id + "/complete", null, p[0]);
        ok("POST", "/bookings/" + id + "/review", Map.of("score", 5), c[0]);
        // 客户：接单、完成两条；摄影师：新预约、新评价两条
        assertThat(unread(c[0])).isEqualTo(2);
        assertThat(unread(p[0])).isEqualTo(2);

        // 重复评价返回业务错误而不是 500
        JsonNode again = call("POST", "/bookings/" + id + "/review", Map.of("score", 4), c[0]);
        assertThat(again.get("code").asInt()).isEqualTo(400);
    }

    @Test
    void onlyPhotographerCanConfirm() throws Exception {
        String[] c = register("客户");
        String[] p = photographer("摄影师");
        long id = book(c[0], p[1], tomorrow, "10:00");
        assertThat(call("POST", "/bookings/" + id + "/confirm", null, c[0]).get("code").asInt()).isEqualTo(403);
    }

    @Test
    void concurrentConfirmOfOverlappingBookingsOnlyOneWins() throws Exception {
        String[] p = photographer("摄影师");
        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            ids.add(book(register("客户" + i)[0], p[1], tomorrow, "14:00"));
        }
        ExecutorService pool = Executors.newFixedThreadPool(ids.size());
        List<Future<Integer>> results = new ArrayList<>();
        for (long id : ids) {
            Callable<Integer> task = () -> call("POST", "/bookings/" + id + "/confirm", null, p[0]).get("code").asInt();
            results.add(pool.submit(task));
        }
        int success = 0;
        for (Future<Integer> f : results) {
            if (f.get() == 0) success++;
        }
        pool.shutdown();
        assertThat(success).isEqualTo(1);
    }

    @Test
    void cannotSetDayOffWhenConfirmedBookingExists() throws Exception {
        String[] c = register("客户");
        String[] p = photographer("摄影师");
        long id = book(c[0], p[1], tomorrow, "09:00");
        ok("POST", "/bookings/" + id + "/confirm", null, p[0]);
        JsonNode r = call("PUT", "/photographers/me/off", Map.of("date", tomorrow.toString(), "off", true), p[0]);
        assertThat(r.get("code").asInt()).isEqualTo(400);
    }

    private int unread(String token) throws Exception {
        return ok("GET", "/me/notifications/unread-count", null, token).asInt();
    }
}
