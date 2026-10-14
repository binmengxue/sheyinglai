package com.sheyinglai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sheyinglai.common.Auth;
import com.sheyinglai.common.BizException;
import com.sheyinglai.common.PageResult;
import com.sheyinglai.common.RateLimiter;
import com.sheyinglai.dto.BookingReqs;
import com.sheyinglai.dto.Views;
import com.sheyinglai.entity.Booking;
import com.sheyinglai.entity.Notification;
import com.sheyinglai.entity.Photographer;
import com.sheyinglai.entity.User;
import com.sheyinglai.mapper.BookingMapper;
import com.sheyinglai.mapper.PhotographerMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 预约单状态流转：
 * PENDING --摄影师确认--> CONFIRMED --摄影师标记完成--> COMPLETED（之后客户可评价）
 * PENDING --摄影师拒绝--> REJECTED
 * PENDING / CONFIRMED --任一方取消--> CANCELLED
 */
@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingMapper bookingMapper;
    private final PhotographerMapper photographerMapper;
    private final PhotographerService photographerService;
    private final ScheduleService scheduleService;
    private final UserService userService;
    private final StatService statService;
    private final NotificationService notificationService;
    private final RateLimiter rateLimiter;

    @Transactional
    public Booking create(BookingReqs.CreateReq req) {
        Long uid = Auth.uid();
        rateLimiter.check("booking:" + uid, 20, Duration.ofHours(1), "预约太频繁，请稍后再试");
        if (uid.equals(req.photographerId())) {
            throw new BizException("不能预约自己");
        }
        Photographer p = photographerService.getBookable(req.photographerId());
        scheduleService.checkBookable(p, req.shootDate(), req.startTime(), req.hours());
        boolean duplicated = bookingMapper.exists(new LambdaQueryWrapper<Booking>()
                .eq(Booking::getCustomerId, uid).eq(Booking::getPhotographerId, p.getUserId())
                .eq(Booking::getShootDate, req.shootDate()).eq(Booking::getStatus, Booking.PENDING));
        if (duplicated) {
            throw new BizException("这一天你已经有一个待确认的预约了，请等摄影师回复");
        }

        Booking b = new Booking();
        b.setCustomerId(uid);
        b.setPhotographerId(p.getUserId());
        b.setShootDate(req.shootDate());
        b.setStartTime(req.startTime());
        b.setHours(req.hours());
        b.setPeople(req.people());
        b.setAddress(trim(req.address()));
        b.setStyle(trim(req.style()));
        b.setRemark(trim(req.remark()));
        b.setCustomerContact(req.contact().trim());
        b.setPriceEstimate(estimate(p, req.hours(), req.people()));
        b.setStatus(Booking.PENDING);
        b.setReviewed(false);
        b.setReminded(false);
        b.setRescheduleCount(0);
        b.setCreatedAt(LocalDateTime.now());
        bookingMapper.insert(b);
        statService.incr(p.getUserId(), StatService.Kind.CONSULT);
        notify(b, b.getPhotographerId(), "新的预约",
                notificationService.nickname(uid) + " 预约了你 " + when(b) + " 的拍摄，请尽快确认");
        return b;
    }

    /** 预估价：单价 × 时长 + 加人费 × (人数 - 1) × 时长；交通费线下另算 */
    static int estimate(Photographer p, int hours, int people) {
        return p.getPricePerHour() * hours + p.getExtraPersonPrice() * Math.max(0, people - 1) * hours;
    }

    /** role：customer 我约的 / photographer 约我的 */
    public PageResult<Views.BookingView> list(String role, String status, long page, long size) {
        Long uid = Auth.uid();
        boolean asPhotographer = "photographer".equals(role);
        Page<Booking> p = bookingMapper.selectPage(Page.of(Math.max(1, page), Math.max(1, Math.min(size, 50))),
                new LambdaQueryWrapper<Booking>()
                        .eq(asPhotographer ? Booking::getPhotographerId : Booking::getCustomerId, uid)
                        .eq(StringUtils.hasText(status), Booking::getStatus, status)
                        .orderByDesc(Booking::getId));
        Set<Long> userIds = new HashSet<>();
        p.getRecords().forEach(b -> {
            userIds.add(b.getCustomerId());
            userIds.add(b.getPhotographerId());
        });
        Map<Long, User> users = userService.mapByIds(userIds);
        List<Views.BookingView> views = p.getRecords().stream().map(b -> view(b, users, uid)).toList();
        return new PageResult<>(views, p.getTotal(), p.getCurrent(), p.getSize());
    }

    @Transactional
    public Views.BookingView confirm(Long id) {
        Booking b = load(id);
        mustBePhotographer(b);
        mustBe(b, Booking.PENDING);
        if (b.getShootDate().isBefore(LocalDate.now())) {
            throw new BizException("拍摄日期已过，只能拒绝");
        }
        photographerService.lock(b.getPhotographerId());
        if (scheduleService.isOff(b.getPhotographerId(), b.getShootDate())) {
            throw new BizException("这天你设置了休息，请先在档期里取消休息再接单");
        }
        // 同一时段可能有多个待确认的预约，确认前检查不和已确认的冲突（已加锁，并发确认会排队）
        boolean overlap = bookingMapper.selectList(new LambdaQueryWrapper<Booking>()
                        .eq(Booking::getPhotographerId, b.getPhotographerId())
                        .eq(Booking::getShootDate, b.getShootDate())
                        .eq(Booking::getStatus, Booking.CONFIRMED))
                .stream().anyMatch(o -> o.getStartTime().isBefore(b.endTime()) && b.getStartTime().isBefore(o.endTime()));
        if (overlap) {
            throw new BizException("和已确认的预约时间冲突，请先拒绝或与客户协商改期");
        }
        Views.BookingView v = transit(b, Booking.CONFIRMED, w -> w.set(Booking::getRespondedAt, LocalDateTime.now()));
        notify(b, b.getCustomerId(), "摄影师已接单",
                "你预约的 " + when(b) + " 的拍摄已确认，可以在订单里查看摄影师联系方式");
        return v;
    }

    @Transactional
    public Views.BookingView reject(Long id) {
        Booking b = load(id);
        mustBePhotographer(b);
        mustBe(b, Booking.PENDING);
        Views.BookingView v = transit(b, Booking.REJECTED, w -> w.set(Booking::getRespondedAt, LocalDateTime.now()));
        notify(b, b.getCustomerId(), "预约未被接受",
                "你预约的 " + when(b) + " 的拍摄摄影师暂时无法接单，可以看看其他摄影师");
        return v;
    }

    /** 订单详情：只有双方和管理员能看 */
    public Views.BookingView detail(Long id) {
        Booking b = load(id);
        Long uid = Auth.uid();
        if (!uid.equals(b.getCustomerId()) && !uid.equals(b.getPhotographerId()) && !Auth.isAdmin()) {
            throw new BizException(403, "无权查看");
        }
        Map<Long, User> users = userService.mapByIds(List.of(b.getCustomerId(), b.getPhotographerId()));
        return view(b, users, uid);
    }

    /** 取消前预览：按摄影师的取消规则，现在取消要付多少违约金 */
    public Views.CancelPreview cancelPreview(Long id) {
        Booking b = load(id);
        Long uid = mustBeParticipant(b);
        mustBe(b, Booking.PENDING, Booking.CONFIRMED);
        boolean byCustomer = uid.equals(b.getCustomerId());
        checkCustomerCanCancel(b, byCustomer);
        Photographer p = photographerMapper.selectById(b.getPhotographerId());
        int penalty = byCustomer ? penalty(b, p) : 0;
        return new Views.CancelPreview(penalty, p == null ? null : p.getCancelPolicy(),
                p == null ? 0 : nz(p.getPenaltyPercent()));
    }

    @Transactional
    public Views.BookingView cancel(Long id, BookingReqs.CancelReq req) {
        Booking b = load(id);
        Long uid = mustBeParticipant(b);
        mustBe(b, Booking.PENDING, Booking.CONFIRMED);
        boolean byCustomer = uid.equals(b.getCustomerId());
        checkCustomerCanCancel(b, byCustomer);
        int penalty = byCustomer ? penalty(b, photographerMapper.selectById(b.getPhotographerId())) : 0;
        if (penalty > 0 && !req.acceptPenalty()) {
            throw new BizException(409, "按摄影师的取消规则，现在取消需支付违约金 ¥" + penalty + "，确认后再取消");
        }
        String reason = trim(req.reason());
        Views.BookingView v = transit(b, Booking.CANCELLED, w -> w.set(Booking::getCancelledBy, uid)
                .set(Booking::getCancelReason, reason)
                .set(Booking::getPenaltyAmount, penalty > 0 ? penalty : null));
        notify(b, byCustomer ? b.getPhotographerId() : b.getCustomerId(), "预约已取消",
                notificationService.nickname(uid) + " 取消了 " + when(b) + " 的拍摄预约"
                        + (reason == null ? "" : "，原因：" + reason)
                        + (penalty > 0 ? "（按取消规则需支付违约金 ¥" + penalty + "）" : ""));
        return v;
    }

    /**
     * 客户改期：待确认或已确认的预约都可以改，改完回到「待确认」，需要摄影师重新确认。
     * 最多改 3 次，拍摄开始后不能再改。
     */
    @Transactional
    public Views.BookingView reschedule(Long id, BookingReqs.RescheduleReq req) {
        Booking b = load(id);
        if (!Auth.uid().equals(b.getCustomerId())) {
            throw new BizException(403, "只有预约人可以改期");
        }
        mustBe(b, Booking.PENDING, Booking.CONFIRMED);
        if (!b.startAt().isAfter(LocalDateTime.now())) {
            throw new BizException("拍摄已经开始，不能改期");
        }
        if (nz(b.getRescheduleCount()) >= MAX_RESCHEDULE) {
            throw new BizException("每个预约最多改期 " + MAX_RESCHEDULE + " 次，请取消后重新预约");
        }
        if (req.shootDate().equals(b.getShootDate()) && req.startTime().equals(b.getStartTime())
                && req.hours().equals(b.getHours())) {
            throw new BizException("新的时间和原来一样");
        }
        Photographer p = photographerService.getBookable(b.getPhotographerId());
        photographerService.lock(p.getUserId());
        scheduleService.checkBookable(p, req.shootDate(), req.startTime(), req.hours(), b.getId());
        String oldWhen = when(b);
        Views.BookingView v = transit(b, Booking.PENDING, w -> w
                .set(Booking::getShootDate, req.shootDate())
                .set(Booking::getStartTime, req.startTime())
                .set(Booking::getHours, req.hours())
                .set(Booking::getPriceEstimate, estimate(p, req.hours(), b.getPeople()))
                .set(Booking::getRespondedAt, null)
                .set(Booking::getReminded, false)
                .set(Booking::getRescheduleCount, nz(b.getRescheduleCount()) + 1));
        notify(b, b.getPhotographerId(), "客户申请改期",
                notificationService.nickname(b.getCustomerId()) + " 把 " + oldWhen + " 的预约改到了 "
                        + when(v.booking()) + "，请重新确认");
        return v;
    }

    private static final int MAX_RESCHEDULE = 3;

    /**
     * 违约金：只有客户取消已确认的预约才可能产生，按预估费用的比例计算。
     * LOOSE 不收；NORMAL 拍摄前 24 小时内取消才收；STRICT 任何时候取消都收。
     */
    static int penalty(Booking b, Photographer p) {
        if (p == null || !Booking.CONFIRMED.equals(b.getStatus())) return 0;
        int percent = nz(p.getPenaltyPercent());
        if (percent <= 0) return 0;
        boolean charge = switch (p.getCancelPolicy() == null ? "" : p.getCancelPolicy()) {
            case "STRICT" -> true;
            case "NORMAL" -> !LocalDateTime.now().isBefore(b.startAt().minusHours(24));
            default -> false;
        };
        return charge ? nz(b.getPriceEstimate()) * percent / 100 : 0;
    }

    /** 拍摄开始后客户不能再取消已确认的预约（摄影师仍可取消，比如客户没到场） */
    private static void checkCustomerCanCancel(Booking b, boolean byCustomer) {
        if (byCustomer && Booking.CONFIRMED.equals(b.getStatus()) && !b.startAt().isAfter(LocalDateTime.now())) {
            throw new BizException("拍摄时间已到，不能再取消，有问题请直接联系摄影师");
        }
    }

    private static Long mustBeParticipant(Booking b) {
        Long uid = Auth.uid();
        if (!uid.equals(b.getCustomerId()) && !uid.equals(b.getPhotographerId())) {
            throw new BizException(403, "无权操作");
        }
        return uid;
    }

    private static int nz(Integer n) {
        return n == null ? 0 : n;
    }

    @Transactional
    public Views.BookingView complete(Long id) {
        Booking b = load(id);
        mustBePhotographer(b);
        mustBe(b, Booking.CONFIRMED);
        if (b.getShootDate().isAfter(LocalDate.now())) {
            throw new BizException("还没到拍摄日期，拍完后再标记完成");
        }
        Views.BookingView v = transit(b, Booking.COMPLETED, w -> {
        });
        photographerMapper.update(null, new LambdaUpdateWrapper<Photographer>()
                .setSql("deal_count = deal_count + 1").eq(Photographer::getUserId, b.getPhotographerId()));
        notify(b, b.getCustomerId(), "拍摄已完成", when(b) + " 的拍摄已完成，快去给摄影师写个评价吧");
        return v;
    }

    /** 评价时用：只能评价自己已完成且未评价过的预约 */
    public Booking loadForReview(Long id) {
        Booking b = load(id);
        if (!Auth.uid().equals(b.getCustomerId())) {
            throw new BizException(403, "只能评价自己的预约");
        }
        mustBe(b, Booking.COMPLETED);
        if (Boolean.TRUE.equals(b.getReviewed())) {
            throw new BizException("已经评价过了");
        }
        return b;
    }

    /** 状态更新带上原状态做条件，并发时只有一个请求能成功 */
    private Views.BookingView transit(Booking b, String to,
                                      java.util.function.Consumer<LambdaUpdateWrapper<Booking>> extra) {
        LambdaUpdateWrapper<Booking> w = new LambdaUpdateWrapper<Booking>()
                .set(Booking::getStatus, to)
                .eq(Booking::getId, b.getId()).eq(Booking::getStatus, b.getStatus());
        extra.accept(w);
        if (bookingMapper.update(null, w) == 0) {
            throw new BizException("预约状态已变化，请刷新后重试");
        }
        Booking fresh = bookingMapper.selectById(b.getId());
        Map<Long, User> users = userService.mapByIds(List.of(fresh.getCustomerId(), fresh.getPhotographerId()));
        return view(fresh, users, Auth.uid());
    }

    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("M月d日 HH:mm");

    private static String when(Booking b) {
        return b.getShootDate().atTime(b.getStartTime()).format(WHEN);
    }

    /** 通知订单的某一方，点击消息打开订单详情 */
    private void notify(Booking b, Long userId, String title, String content) {
        notificationService.send(userId, Notification.TYPE_BOOKING, title, content, "/orders/" + b.getId());
    }

    private Views.BookingView view(Booking b, Map<Long, User> users, Long viewer) {
        User photographer = users.get(b.getPhotographerId());
        boolean showContact = viewer.equals(b.getCustomerId())
                && (Booking.CONFIRMED.equals(b.getStatus()) || Booking.COMPLETED.equals(b.getStatus()));
        return new Views.BookingView(b, Views.UserBrief.of(users.get(b.getCustomerId())),
                Views.UserBrief.of(photographer), showContact ? UserService.contactOf(photographer) : null);
    }

    private Booking load(Long id) {
        Booking b = bookingMapper.selectById(id);
        if (b == null) {
            throw new BizException(404, "预约不存在");
        }
        return b;
    }

    private static void mustBePhotographer(Booking b) {
        if (!Auth.uid().equals(b.getPhotographerId())) {
            throw new BizException(403, "只有摄影师本人可以操作");
        }
    }

    private static void mustBe(Booking b, String... statuses) {
        for (String s : statuses) {
            if (s.equals(b.getStatus())) return;
        }
        throw new BizException("当前状态不能进行该操作");
    }

    private static String trim(String s) {
        return StringUtils.hasText(s) ? s.trim() : null;
    }
}
