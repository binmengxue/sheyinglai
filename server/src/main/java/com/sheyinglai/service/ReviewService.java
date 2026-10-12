package com.sheyinglai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sheyinglai.common.Auth;
import com.sheyinglai.common.BizException;
import com.sheyinglai.common.PageResult;
import com.sheyinglai.dto.BookingReqs;
import com.sheyinglai.dto.Views;
import com.sheyinglai.entity.Booking;
import com.sheyinglai.entity.Photographer;
import com.sheyinglai.entity.Review;
import com.sheyinglai.mapper.BookingMapper;
import com.sheyinglai.mapper.PhotographerMapper;
import com.sheyinglai.mapper.ReviewMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewMapper reviewMapper;
    private final BookingMapper bookingMapper;
    private final PhotographerMapper photographerMapper;
    private final BookingService bookingService;
    private final UserService userService;

    @Transactional
    public Review create(Long bookingId, BookingReqs.ReviewReq req) {
        Booking b = bookingService.loadForReview(bookingId);
        Review r = new Review();
        r.setBookingId(b.getId());
        r.setCustomerId(b.getCustomerId());
        r.setPhotographerId(b.getPhotographerId());
        r.setScore(req.score());
        r.setContent(StringUtils.hasText(req.content()) ? req.content().trim() : null);
        r.setCreatedAt(LocalDateTime.now());
        reviewMapper.insert(r);
        bookingMapper.update(null, new LambdaUpdateWrapper<Booking>()
                .set(Booking::getReviewed, true).eq(Booking::getId, b.getId()));
        refreshRating(b.getPhotographerId());
        return r;
    }

    public PageResult<Views.ReviewView> ofPhotographer(Long photographerId, long page, long size) {
        return page(new LambdaQueryWrapper<Review>().eq(Review::getPhotographerId, photographerId),
                Review::getCustomerId, page, size);
    }

    /** 我写过的评价，user 是被评价的摄影师 */
    public PageResult<Views.ReviewView> mine(long page, long size) {
        return page(new LambdaQueryWrapper<Review>().eq(Review::getCustomerId, Auth.uid()),
                Review::getPhotographerId, page, size);
    }

    /** 管理员删除违规评价，评分重新计算；预约仍标记为已评价，不能再评 */
    @Transactional
    public void delete(Long id) {
        Review r = reviewMapper.selectById(id);
        if (r == null) {
            throw new BizException(404, "评价不存在");
        }
        reviewMapper.deleteById(id);
        refreshRating(r.getPhotographerId());
    }

    private PageResult<Views.ReviewView> page(LambdaQueryWrapper<Review> q, Function<Review, Long> other,
                                              long page, long size) {
        Page<Review> p = reviewMapper.selectPage(Page.of(Math.max(1, page), Math.max(1, Math.min(size, 50))),
                q.orderByDesc(Review::getId));
        Map<Long, Views.UserBrief> users = userService.briefs(p.getRecords().stream().map(other).distinct().toList());
        List<Views.ReviewView> views = p.getRecords().stream()
                .map(r -> new Views.ReviewView(r, users.getOrDefault(other.apply(r), Views.UserBrief.of(null))))
                .toList();
        return new PageResult<>(views, p.getTotal(), p.getCurrent(), p.getSize());
    }

    private void refreshRating(Long photographerId) {
        Map<String, Object> m = reviewMapper.selectMaps(new QueryWrapper<Review>()
                .select("COUNT(*) AS cnt", "SUM(score) AS total").eq("photographer_id", photographerId)).get(0);
        long cnt = ((Number) m.get("cnt")).longValue();
        long total = m.get("total") == null ? 0 : ((Number) m.get("total")).longValue();
        BigDecimal avg = cnt == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf(total).divide(BigDecimal.valueOf(cnt), 2, RoundingMode.HALF_UP);
        photographerMapper.update(null, new LambdaUpdateWrapper<Photographer>()
                .set(Photographer::getRatingAvg, avg)
                .set(Photographer::getRatingCount, (int) cnt)
                .eq(Photographer::getUserId, photographerId));
    }
}
