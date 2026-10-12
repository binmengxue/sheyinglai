package com.sheyinglai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sheyinglai.common.Auth;
import com.sheyinglai.common.BizException;
import com.sheyinglai.dto.Views;
import com.sheyinglai.entity.Booking;
import com.sheyinglai.entity.Photographer;
import com.sheyinglai.entity.ScheduleOff;
import com.sheyinglai.mapper.BookingMapper;
import com.sheyinglai.mapper.ScheduleOffMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 档期：每天的可约时段 = 接单时间段（work_start~work_end）减去已确认的预约。
 * 不接单的星期、手动设置的休息日整天不可约。
 */
@Service
@RequiredArgsConstructor
public class ScheduleService {

    public static final String EXPIRED = "EXPIRED";
    public static final String OFF = "OFF";
    public static final String FULL = "FULL";
    public static final String AVAILABLE = "AVAILABLE";

    /** 最多可以提前多少天预约 */
    private static final int MAX_DAYS_AHEAD = 90;

    private final ScheduleOffMapper offMapper;
    private final BookingMapper bookingMapper;
    private final PhotographerService photographerService;

    public Views.ScheduleView month(Long photographerId, YearMonth month) {
        Photographer p = photographerService.get(photographerId);
        LocalDate from = month.atDay(1);
        LocalDate to = month.atEndOfMonth();
        Set<LocalDate> offs = offMapper.selectList(new LambdaQueryWrapper<ScheduleOff>()
                        .eq(ScheduleOff::getPhotographerId, photographerId).between(ScheduleOff::getOffDate, from, to))
                .stream().map(ScheduleOff::getOffDate).collect(Collectors.toSet());
        Map<LocalDate, List<Booking>> booked = confirmed(photographerId, from, to).stream()
                .collect(Collectors.groupingBy(Booking::getShootDate));

        List<Views.DayInfo> days = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            days.add(day(p, d, offs.contains(d), booked.getOrDefault(d, List.of())));
        }
        return new Views.ScheduleView(p.getMinHours(), p.getWorkStart(), p.getWorkEnd(), days);
    }

    /** 摄影师切换某天休息 */
    public void setOff(LocalDate date, boolean off) {
        Long uid = Auth.uid();
        if (off) {
            ScheduleOff s = new ScheduleOff();
            s.setPhotographerId(uid);
            s.setOffDate(date);
            try {
                offMapper.insert(s);
            } catch (DuplicateKeyException ignored) {
                // 已经是休息日
            }
        } else {
            offMapper.delete(new LambdaQueryWrapper<ScheduleOff>()
                    .eq(ScheduleOff::getPhotographerId, uid).eq(ScheduleOff::getOffDate, date));
        }
    }

    /** 校验这个时间段能约，不能约直接抛出原因 */
    public void checkBookable(Photographer p, LocalDate date, LocalTime start, int hours) {
        if (date.isAfter(LocalDate.now().plusDays(MAX_DAYS_AHEAD))) {
            throw new BizException("最多只能预约 " + MAX_DAYS_AHEAD + " 天内的档期");
        }
        if (hours < p.getMinHours()) {
            throw new BizException(p.getMinHours() + " 小时起拍");
        }
        if (start.getMinute() != 0 && start.getMinute() != 30 || start.getSecond() != 0) {
            throw new BizException("开始时间只能是整点或半点");
        }
        boolean off = offMapper.exists(new LambdaQueryWrapper<ScheduleOff>()
                .eq(ScheduleOff::getPhotographerId, p.getUserId()).eq(ScheduleOff::getOffDate, date));
        Views.DayInfo day = day(p, date, off, confirmed(p.getUserId(), date, date));
        switch (day.status()) {
            case EXPIRED -> throw new BizException("这一天已经过去了");
            case OFF -> throw new BizException("摄影师这天休息");
            case FULL -> throw new BizException("这一天已约满");
            default -> {
            }
        }
        // 跨过午夜（如 22:00 开始拍 4 小时）时 plusHours 会回绕成 02:00，单独判断
        LocalTime end = start.plusHours(hours);
        if (!end.isAfter(start)) {
            throw new BizException("拍摄时间不能跨天");
        }
        boolean fits = day.free().stream().anyMatch(s -> !start.isBefore(s.start()) && !end.isAfter(s.end()));
        if (!fits) {
            throw new BizException("该时段不可约，请重新选择");
        }
    }

    private Views.DayInfo day(Photographer p, LocalDate d, boolean manuallyOff, List<Booking> bookings) {
        LocalDate today = LocalDate.now();
        if (d.isBefore(today)) {
            return new Views.DayInfo(d, EXPIRED, List.of());
        }
        if (manuallyOff || !worksOn(p.getWorkDays(), d.getDayOfWeek())) {
            return new Views.DayInfo(d, OFF, List.of());
        }
        LocalTime begin = p.getWorkStart();
        if (d.equals(today)) {
            // 今天只能约一小时以后、整点或半点开始的时段
            LocalTime earliest = roundUpHalfHour(LocalDateTime.now().plusHours(1));
            if (earliest == null) {
                return new Views.DayInfo(d, FULL, List.of());
            }
            if (earliest.isAfter(begin)) begin = earliest;
        }
        List<Views.Slot> free = new ArrayList<>();
        LocalTime cursor = begin;
        for (Booking b : bookings.stream().sorted(Comparator.comparing(Booking::getStartTime)).toList()) {
            if (b.getStartTime().isAfter(cursor)) {
                free.add(new Views.Slot(cursor, min(b.getStartTime(), p.getWorkEnd())));
            }
            if (b.endTime().isAfter(cursor)) cursor = b.endTime();
        }
        if (cursor.isBefore(p.getWorkEnd())) {
            free.add(new Views.Slot(cursor, p.getWorkEnd()));
        }
        long minMinutes = p.getMinHours() * 60L;
        free = free.stream().filter(s -> s.start().isBefore(s.end())
                && ChronoUnit.MINUTES.between(s.start(), s.end()) >= minMinutes).toList();
        return new Views.DayInfo(d, free.isEmpty() ? FULL : AVAILABLE, free);
    }

    private List<Booking> confirmed(Long photographerId, LocalDate from, LocalDate to) {
        return bookingMapper.selectList(new LambdaQueryWrapper<Booking>()
                .eq(Booking::getPhotographerId, photographerId)
                .eq(Booking::getStatus, Booking.CONFIRMED)
                .between(Booking::getShootDate, from, to));
    }

    private static boolean worksOn(String workDays, DayOfWeek dow) {
        boolean weekend = dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY;
        return switch (workDays == null ? "ALL" : workDays) {
            case "WEEKDAY" -> !weekend;
            case "WEEKEND" -> weekend;
            default -> true;
        };
    }

    /** 向上取整到半点；已经到第二天了返回 null */
    private static LocalTime roundUpHalfHour(LocalDateTime t) {
        LocalDateTime r = t.truncatedTo(ChronoUnit.HOURS);
        if (t.isAfter(r)) r = r.plusMinutes(30);
        if (t.isAfter(r)) r = r.plusMinutes(30);
        return r.toLocalDate().isAfter(LocalDate.now()) ? null : r.toLocalTime();
    }

    private static LocalTime min(LocalTime a, LocalTime b) {
        return a.isBefore(b) ? a : b;
    }
}
