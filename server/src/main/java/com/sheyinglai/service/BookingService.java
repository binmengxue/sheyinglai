package com.sheyinglai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sheyinglai.common.Auth;
import com.sheyinglai.common.BizException;
import com.sheyinglai.common.PageResult;
import com.sheyinglai.dto.BookingReqs;
import com.sheyinglai.dto.Views;
import com.sheyinglai.entity.Booking;
import com.sheyinglai.entity.Photographer;
import com.sheyinglai.entity.User;
import com.sheyinglai.mapper.BookingMapper;
import com.sheyinglai.mapper.PhotographerMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
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

    @Transactional
    public Booking create(BookingReqs.CreateReq req) {
        Long uid = Auth.uid();
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
        b.setCreatedAt(LocalDateTime.now());
        bookingMapper.insert(b);
        statService.incr(p.getUserId(), StatService.Kind.CONSULT);
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
        // 同一时段可能有多个待确认的预约，确认前检查不和已确认的冲突
        boolean overlap = bookingMapper.selectList(new LambdaQueryWrapper<Booking>()
                        .eq(Booking::getPhotographerId, b.getPhotographerId())
                        .eq(Booking::getShootDate, b.getShootDate())
                        .eq(Booking::getStatus, Booking.CONFIRMED))
                .stream().anyMatch(o -> o.getStartTime().isBefore(b.endTime()) && b.getStartTime().isBefore(o.endTime()));
        if (overlap) {
            throw new BizException("和已确认的预约时间冲突，请先拒绝或与客户协商改期");
        }
        return transit(b, Booking.CONFIRMED, w -> w.set(Booking::getRespondedAt, LocalDateTime.now()));
    }

    public Views.BookingView reject(Long id) {
        Booking b = load(id);
        mustBePhotographer(b);
        mustBe(b, Booking.PENDING);
        return transit(b, Booking.REJECTED, w -> w.set(Booking::getRespondedAt, LocalDateTime.now()));
    }

    public Views.BookingView cancel(Long id) {
        Booking b = load(id);
        Long uid = Auth.uid();
        if (!uid.equals(b.getCustomerId()) && !uid.equals(b.getPhotographerId())) {
            throw new BizException(403, "无权操作");
        }
        mustBe(b, Booking.PENDING, Booking.CONFIRMED);
        return transit(b, Booking.CANCELLED, w -> w.set(Booking::getCancelledBy, uid));
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
