package com.sheyinglai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.sheyinglai.PhotoServerApplication;
import com.sheyinglai.entity.Booking;
import com.sheyinglai.entity.Notification;
import com.sheyinglai.entity.Photographer;
import com.sheyinglai.mapper.BookingMapper;
import com.sheyinglai.mapper.PhotographerMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 订单自动流转：
 * - 每小时：拍摄时间已到还没确认的预约自动过期；拍摄日 3 天后仍未标记完成的自动完成（客户才能评价）
 * - 每天 9 点：提醒明天有拍摄的双方
 * 每条记录单独更新，带上原状态做条件，和用户操作并发时不会互相覆盖。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookingJobs {

    /** 拍摄日之后多少天自动完成 */
    static final int AUTO_COMPLETE_DAYS = 3;
    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("M月d日 HH:mm");

    private final BookingMapper bookingMapper;
    private final PhotographerMapper photographerMapper;
    private final NotificationService notificationService;
    private final TransactionTemplate tx;

    @Scheduled(cron = "0 5 * * * *", zone = PhotoServerApplication.ZONE)
    public void hourly() {
        expirePending();
        autoComplete();
    }

    @Scheduled(cron = "0 0 9 * * *", zone = PhotoServerApplication.ZONE)
    public void daily() {
        remindTomorrow();
    }

    /** 拍摄开始时间已过、摄影师一直没处理的预约 → 已过期 */
    public int expirePending() {
        LocalDateTime now = LocalDateTime.now();
        List<Booking> list = bookingMapper.selectList(new LambdaQueryWrapper<Booking>()
                .eq(Booking::getStatus, Booking.PENDING).le(Booking::getShootDate, now.toLocalDate()));
        int n = 0;
        for (Booking b : list) {
            if (b.startAt().isAfter(now)) continue;
            boolean done = Boolean.TRUE.equals(tx.execute(s -> {
                if (!changeStatus(b, Booking.EXPIRED)) return false;
                notify(b, b.getCustomerId(), "预约已过期", "你预约的 " + when(b) + " 的拍摄摄影师没有及时确认，预约已自动关闭，可以看看其他摄影师");
                notify(b, b.getPhotographerId(), "预约已过期", when(b) + " 的预约你没有及时处理，已自动关闭");
                return true;
            }));
            if (done) n++;
        }
        if (n > 0) log.info("自动关闭过期预约 {} 个", n);
        return n;
    }

    /** 拍摄日过去 3 天仍未标记完成的已确认预约 → 已完成，并提醒客户评价 */
    public int autoComplete() {
        LocalDate before = LocalDate.now().minusDays(AUTO_COMPLETE_DAYS);
        List<Booking> list = bookingMapper.selectList(new LambdaQueryWrapper<Booking>()
                .eq(Booking::getStatus, Booking.CONFIRMED).le(Booking::getShootDate, before));
        int n = 0;
        for (Booking b : list) {
            boolean done = Boolean.TRUE.equals(tx.execute(s -> {
                if (!changeStatus(b, Booking.COMPLETED)) return false;
                photographerMapper.update(null, new LambdaUpdateWrapper<Photographer>()
                        .setSql("deal_count = deal_count + 1").eq(Photographer::getUserId, b.getPhotographerId()));
                notify(b, b.getCustomerId(), "拍摄已完成", when(b) + " 的拍摄已自动标记完成，快去给摄影师写个评价吧");
                return true;
            }));
            if (done) n++;
        }
        if (n > 0) log.info("自动完成预约 {} 个", n);
        return n;
    }

    /** 明天有拍摄的已确认预约，提醒双方（每个预约只提醒一次） */
    public int remindTomorrow() {
        List<Booking> list = bookingMapper.selectList(new LambdaQueryWrapper<Booking>()
                .eq(Booking::getStatus, Booking.CONFIRMED).eq(Booking::getShootDate, LocalDate.now().plusDays(1))
                .eq(Booking::getReminded, false));
        int n = 0;
        for (Booking b : list) {
            boolean done = Boolean.TRUE.equals(tx.execute(s -> {
                int updated = bookingMapper.update(null, new LambdaUpdateWrapper<Booking>()
                        .set(Booking::getReminded, true)
                        .eq(Booking::getId, b.getId()).eq(Booking::getReminded, false));
                if (updated == 0) return false;
                String place = b.getAddress() == null ? "" : "，地点：" + b.getAddress();
                notify(b, b.getCustomerId(), "明天有拍摄", "别忘了 " + when(b) + " 的拍摄" + place);
                notify(b, b.getPhotographerId(), "明天有拍摄", "别忘了 " + when(b) + " 的拍摄" + place);
                return true;
            }));
            if (done) n++;
        }
        if (n > 0) log.info("发送拍摄提醒 {} 个", n);
        return n;
    }

    private boolean changeStatus(Booking b, String to) {
        return bookingMapper.update(null, new LambdaUpdateWrapper<Booking>()
                .set(Booking::getStatus, to)
                .eq(Booking::getId, b.getId()).eq(Booking::getStatus, b.getStatus())) > 0;
    }

    private void notify(Booking b, Long userId, String title, String content) {
        notificationService.send(userId, Notification.TYPE_BOOKING, title, content, "/orders/" + b.getId());
    }

    private static String when(Booking b) {
        return b.startAt().format(WHEN);
    }
}
