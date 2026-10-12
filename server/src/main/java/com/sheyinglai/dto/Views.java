package com.sheyinglai.dto;

import com.sheyinglai.entity.Booking;
import com.sheyinglai.entity.Photographer;
import com.sheyinglai.entity.Review;
import com.sheyinglai.entity.User;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** 返回给前端的组合视图，避免把手机号、微信等隐私字段直接带出去 */
public final class Views {

    private Views() {
    }

    /** 对外公开的用户信息 */
    public record UserBrief(Long id, String nickname, String avatarUrl, Integer gender) {
        public static UserBrief of(User u) {
            return u == null ? new UserBrief(null, "已注销用户", null, 0)
                    : new UserBrief(u.getId(), u.getNickname(), u.getAvatarUrl(), u.getGender());
        }
    }

    /** 登录用户自己的信息 */
    public record Me(boolean loggedIn, User user, Photographer photographer, boolean admin) {
    }

    public record LoginResult(String token, Me me) {
    }

    /** 首页卡片上的客片缩略图 */
    public record AlbumBrief(Long id, String coverUrl, Long photoCount) {
    }

    /** 首页摄影师卡片 */
    public record PhotographerCard(
            Long id, String nickname, String avatarUrl, Integer gender, String tag,
            BigDecimal ratingAvg, Integer ratingCount, Integer dealCount,
            Integer pricePerHour, Integer minHours, Integer freeKm, Integer rawCount, Integer retouchCount,
            List<String> styles, String workDays, Boolean refundPromise,
            Double distanceKm, Integer recentConsult, List<AlbumBrief> albums) {
    }

    public record StyleCount(String style, long count) {
    }

    /** 摄影师主页 */
    public record PhotographerDetail(
            PhotographerCard card, Photographer profile, String bio, String city,
            long fans, long following, boolean followed, boolean self, int rank,
            List<StyleCount> styleCounts) {
    }

    public record Slot(LocalTime start, LocalTime end) {
    }

    /** 档期里的一天：EXPIRED 已过期 / OFF 休息 / FULL 约满 / AVAILABLE 可约 */
    public record DayInfo(LocalDate date, String status, List<Slot> free) {
    }

    public record ScheduleView(Integer minHours, LocalTime workStart, LocalTime workEnd, List<DayInfo> days) {
    }

    /**
     * 预约单视图。photographerContact 只在摄影师确认后给客户看；
     * 客户填的联系方式摄影师随时可见，方便确认前沟通。
     */
    public record BookingView(Booking booking, UserBrief customer, UserBrief photographer,
                              String photographerContact) {
    }

    /** user：评价列表里是评价人，「我的点评」里是被评价的摄影师 */
    public record ReviewView(Review review, UserBrief user) {
    }

    public record Metric(long value, Double dayChange, Double weekChange, Double monthChange) {
    }

    public record StatsView(
            Integer avgPrice, Integer maxPrice, Double dealRate, Double acceptRate, Double cancelRate,
            Long avgReplyMinutes,
            Metric exposure, Metric profileView, Metric consult,
            long exposure30, long profileView30, long consult30) {
    }

    public record AdminUserView(User user, Photographer photographer) {
    }
}
