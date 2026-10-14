package com.sheyinglai.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.sheyinglai.common.PageResult;
import com.sheyinglai.common.Result;
import com.sheyinglai.dto.BookingReqs;
import com.sheyinglai.dto.PhotographerReq;
import com.sheyinglai.dto.Views;
import com.sheyinglai.entity.Photographer;
import com.sheyinglai.service.PhotographerService;
import com.sheyinglai.service.ReviewService;
import com.sheyinglai.service.ScheduleService;
import com.sheyinglai.service.SocialService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;

@RestController
@RequestMapping("/photographers")
@RequiredArgsConstructor
public class PhotographerController {

    private final PhotographerService photographerService;
    private final ScheduleService scheduleService;
    private final ReviewService reviewService;
    private final SocialService socialService;

    /**
     * 首页列表。sort：default 综合 / distance 距离（需传 lat、lng） / rating 评分 / price 价格；
     * keyword 按昵称搜索，city 按城市，minPrice / maxPrice 每小时价格区间
     */
    @GetMapping
    public Result<PageResult<Views.PhotographerCard>> list(@RequestParam(required = false) String style,
                                                           @RequestParam(required = false) String sort,
                                                           @RequestParam(required = false) String keyword,
                                                           @RequestParam(required = false) String city,
                                                           @RequestParam(required = false) Integer minPrice,
                                                           @RequestParam(required = false) Integer maxPrice,
                                                           @RequestParam(required = false) Double lat,
                                                           @RequestParam(required = false) Double lng,
                                                           @RequestParam(defaultValue = "1") long page,
                                                           @RequestParam(defaultValue = "10") long size) {
        Views.PhotographerQuery q = new Views.PhotographerQuery(style, sort, keyword, city, minPrice, maxPrice);
        return Result.ok(photographerService.list(q, lat, lng, page, size));
    }

    /** track=false 时不计主页浏览量（档期页等顺带取资料的场景） */
    @GetMapping("/{id}")
    public Result<Views.PhotographerDetail> detail(@PathVariable Long id,
                                                   @RequestParam(required = false) Double lat,
                                                   @RequestParam(required = false) Double lng,
                                                   @RequestParam(defaultValue = "true") boolean track) {
        return Result.ok(photographerService.detail(id, lat, lng, track));
    }

    /** 编辑我的接单资料（收费、精修、设备、爽约规则、档期规则、位置） */
    @SaCheckLogin
    @PutMapping("/me")
    public Result<Photographer> updateMine(@Valid @RequestBody PhotographerReq req) {
        return Result.ok(photographerService.updateMine(req));
    }

    /** 档期，month 形如 2026-10 */
    @GetMapping("/{id}/schedule")
    public Result<Views.ScheduleView> schedule(@PathVariable Long id, @RequestParam String month) {
        return Result.ok(scheduleService.month(id, YearMonth.parse(month)));
    }

    /** 设置或取消某天休息 */
    @SaCheckLogin
    @PutMapping("/me/off")
    public Result<Void> setOff(@Valid @RequestBody BookingReqs.OffReq req) {
        scheduleService.setOff(req.date(), req.off());
        return Result.ok();
    }

    @GetMapping("/{id}/reviews")
    public Result<PageResult<Views.ReviewView>> reviews(@PathVariable Long id,
                                                        @RequestParam(defaultValue = "1") long page,
                                                        @RequestParam(defaultValue = "20") long size) {
        return Result.ok(reviewService.ofPhotographer(id, page, size));
    }

    /** 粉丝列表 */
    @GetMapping("/{id}/fans")
    public Result<PageResult<Views.UserBrief>> fans(@PathVariable Long id,
                                                    @RequestParam(defaultValue = "1") long page,
                                                    @RequestParam(defaultValue = "20") long size) {
        return Result.ok(socialService.fans(id, page, size));
    }

    /** 关注列表 */
    @GetMapping("/{id}/following")
    public Result<PageResult<Views.UserBrief>> following(@PathVariable Long id,
                                                         @RequestParam(defaultValue = "1") long page,
                                                         @RequestParam(defaultValue = "20") long size) {
        return Result.ok(socialService.following(id, page, size));
    }

    @SaCheckLogin
    @PostMapping("/{id}/follow")
    public Result<Void> follow(@PathVariable Long id) {
        socialService.follow(id);
        return Result.ok();
    }

    @SaCheckLogin
    @DeleteMapping("/{id}/follow")
    public Result<Void> unfollow(@PathVariable Long id) {
        socialService.unfollow(id);
        return Result.ok();
    }
}
