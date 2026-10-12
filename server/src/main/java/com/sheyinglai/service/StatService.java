package com.sheyinglai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sheyinglai.common.Auth;
import com.sheyinglai.dto.Views;
import com.sheyinglai.entity.Booking;
import com.sheyinglai.entity.StatDaily;
import com.sheyinglai.mapper.BookingMapper;
import com.sheyinglai.mapper.StatDailyMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

/** 经营数据：埋点计数 + 汇总 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StatService {

    public enum Kind {
        EXPOSURE("exposure"), PROFILE_VIEW("profile_view"), CONSULT("consult");

        private final String column;

        Kind(String column) {
            this.column = column;
        }
    }

    private final StatDailyMapper statMapper;
    private final BookingMapper bookingMapper;

    /** 计数失败只记日志，不影响正常业务 */
    public void incr(Long photographerId, Kind kind) {
        try {
            statMapper.incr(photographerId, LocalDate.now(), kind.column);
        } catch (Exception e) {
            log.warn("统计计数失败 {} {}", photographerId, kind, e);
        }
    }

    /** 最近 30 天每个摄影师的咨询量（首页卡片上的「近期 N 人咨询」） */
    public Map<Long, Integer> recentConsult(Collection<Long> photographerIds) {
        if (photographerIds.isEmpty()) return Map.of();
        return statMapper.selectList(new LambdaQueryWrapper<StatDaily>()
                        .in(StatDaily::getPhotographerId, photographerIds)
                        .ge(StatDaily::getStatDate, LocalDate.now().minusDays(29)))
                .stream().collect(Collectors.groupingBy(StatDaily::getPhotographerId,
                        Collectors.summingInt(StatDaily::getConsult)));
    }

    public Views.StatsView mine() {
        Long pid = Auth.uid();

        // 重要数据：从预约单聚合
        List<Booking> all = bookingMapper.selectList(new LambdaQueryWrapper<Booking>().eq(Booking::getPhotographerId, pid));
        List<Booking> done = all.stream().filter(b -> Booking.COMPLETED.equals(b.getStatus())).toList();
        List<Booking> responded = all.stream().filter(b -> b.getRespondedAt() != null).toList();
        long accepted = responded.stream().filter(b -> !Booking.REJECTED.equals(b.getStatus())).count();
        long cancelled = all.stream().filter(b -> Booking.CANCELLED.equals(b.getStatus())).count();

        Integer avgPrice = done.isEmpty() ? null
                : (int) Math.round(done.stream().mapToInt(Booking::getPriceEstimate).average().orElse(0));
        Integer maxPrice = done.stream().map(Booking::getPriceEstimate).max(Integer::compare).orElse(null);
        Long avgReply = responded.isEmpty() ? null : Math.round(responded.stream()
                .mapToLong(b -> Duration.between(b.getCreatedAt(), b.getRespondedAt()).toMinutes())
                .average().orElse(0));

        // 每日数据：取最近 61 天，够算昨日的日/周/月环比和 30 天汇总
        LocalDate today = LocalDate.now();
        Map<LocalDate, StatDaily> daily = statMapper.selectList(new LambdaQueryWrapper<StatDaily>()
                        .eq(StatDaily::getPhotographerId, pid)
                        .ge(StatDaily::getStatDate, today.minusDays(61)))
                .stream().collect(Collectors.toMap(StatDaily::getStatDate, Function.identity(), (a, b) -> a, HashMap::new));
        LocalDate y = today.minusDays(1);

        return new Views.StatsView(avgPrice, maxPrice,
                rate(done.size(), all.size()), rate(accepted, responded.size()), rate(cancelled, all.size()), avgReply,
                metric(daily, y, StatDaily::getExposure),
                metric(daily, y, StatDaily::getProfileView),
                metric(daily, y, StatDaily::getConsult),
                sum30(daily, today, StatDaily::getExposure),
                sum30(daily, today, StatDaily::getProfileView),
                sum30(daily, today, StatDaily::getConsult));
    }

    private static Views.Metric metric(Map<LocalDate, StatDaily> daily, LocalDate day, ToIntFunction<StatDaily> f) {
        long v = value(daily, day, f);
        return new Views.Metric(v,
                change(v, value(daily, day.minusDays(1), f)),
                change(v, value(daily, day.minusWeeks(1), f)),
                change(v, value(daily, day.minusMonths(1), f)));
    }

    private static long value(Map<LocalDate, StatDaily> daily, LocalDate day, ToIntFunction<StatDaily> f) {
        StatDaily s = daily.get(day);
        return s == null ? 0 : f.applyAsInt(s);
    }

    /** 最近 30 天（含今天） */
    private static long sum30(Map<LocalDate, StatDaily> daily, LocalDate today, ToIntFunction<StatDaily> f) {
        return daily.values().stream().filter(Objects::nonNull)
                .filter(s -> !s.getStatDate().isBefore(today.minusDays(29)))
                .mapToLong(f::applyAsInt).sum();
    }

    /** 环比百分比，基数为 0 时没有意义，返回 null */
    private static Double change(long now, long before) {
        return before == 0 ? null : Math.round((now - before) * 1000.0 / before) / 10.0;
    }

    /** 百分比，保留 1 位小数 */
    private static Double rate(long part, long total) {
        return total == 0 ? null : Math.round(part * 1000.0 / total) / 10.0;
    }
}
