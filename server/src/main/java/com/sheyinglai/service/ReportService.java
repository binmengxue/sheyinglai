package com.sheyinglai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sheyinglai.common.Auth;
import com.sheyinglai.common.BizException;
import com.sheyinglai.common.PageResult;
import com.sheyinglai.common.RateLimiter;
import com.sheyinglai.dto.Views;
import com.sheyinglai.entity.Album;
import com.sheyinglai.entity.Notification;
import com.sheyinglai.entity.Report;
import com.sheyinglai.entity.Review;
import com.sheyinglai.entity.User;
import com.sheyinglai.mapper.AlbumMapper;
import com.sheyinglai.mapper.ReportMapper;
import com.sheyinglai.mapper.ReviewMapper;
import com.sheyinglai.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** 举报：用户提交，管理员在后台处理 */
@Service
@RequiredArgsConstructor
public class ReportService {

    public static final Set<String> TARGETS = Set.of(Report.TARGET_PHOTOGRAPHER, Report.TARGET_REVIEW, Report.TARGET_ALBUM);

    private final ReportMapper reportMapper;
    private final UserMapper userMapper;
    private final ReviewMapper reviewMapper;
    private final AlbumMapper albumMapper;
    private final NotificationService notificationService;
    private final RateLimiter rateLimiter;

    public void create(String targetType, Long targetId, String reason, String content) {
        Long uid = Auth.uid();
        if (!TARGETS.contains(targetType)) {
            throw new BizException("举报对象不正确");
        }
        rateLimiter.check("report:" + uid, 10, Duration.ofHours(1), "举报太频繁，请稍后再试");
        if (target(targetType, targetId) == null) {
            throw new BizException(404, "举报的内容不存在");
        }
        boolean pending = reportMapper.exists(new LambdaQueryWrapper<Report>()
                .eq(Report::getReporterId, uid).eq(Report::getTargetType, targetType)
                .eq(Report::getTargetId, targetId).eq(Report::getStatus, Report.PENDING));
        if (pending) {
            throw new BizException("你已经举报过了，我们会尽快处理");
        }
        Report r = new Report();
        r.setReporterId(uid);
        r.setTargetType(targetType);
        r.setTargetId(targetId);
        r.setReason(reason);
        r.setContent(StringUtils.hasText(content) ? content.trim() : null);
        r.setStatus(Report.PENDING);
        r.setCreatedAt(LocalDateTime.now());
        reportMapper.insert(r);
    }

    public PageResult<Views.ReportView> list(String status, long page, long size) {
        Page<Report> p = reportMapper.selectPage(Page.of(Math.max(1, page), Math.max(1, Math.min(size, 50))),
                new LambdaQueryWrapper<Report>()
                        .eq(StringUtils.hasText(status), Report::getStatus, status)
                        .orderByDesc(Report::getId));
        Set<Long> reporters = p.getRecords().stream().map(Report::getReporterId).collect(Collectors.toSet());
        Map<Long, User> users = reporters.isEmpty() ? Map.of() : userMapper.selectBatchIds(reporters).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        List<Views.ReportView> views = p.getRecords().stream().map(r -> {
            Views.ReportTarget t = target(r.getTargetType(), r.getTargetId());
            return new Views.ReportView(r, Views.UserBrief.of(users.get(r.getReporterId())), t);
        }).toList();
        return new PageResult<>(views, p.getTotal(), p.getCurrent(), p.getSize());
    }

    /** status：HANDLED 已处理 / IGNORED 不处理；处理结果通知举报人 */
    public void handle(Long id, String status, String note) {
        if (!Report.HANDLED.equals(status) && !Report.IGNORED.equals(status)) {
            throw new BizException("处理结果不正确");
        }
        Report r = reportMapper.selectById(id);
        if (r == null) {
            throw new BizException(404, "举报不存在");
        }
        int n = reportMapper.update(null, new LambdaUpdateWrapper<Report>()
                .set(Report::getStatus, status)
                .set(Report::getHandledBy, Auth.uid())
                .set(Report::getHandleNote, StringUtils.hasText(note) ? note.trim() : null)
                .set(Report::getHandledAt, LocalDateTime.now())
                .eq(Report::getId, id).eq(Report::getStatus, Report.PENDING));
        if (n == 0) {
            throw new BizException("这条举报已经处理过了");
        }
        notificationService.send(r.getReporterId(), Notification.TYPE_REPORT, "举报处理结果",
                Report.HANDLED.equals(status) ? "你的举报已核实并处理，感谢你的反馈" : "你的举报经核实暂不需要处理，感谢你的反馈",
                null);
    }

    /** 举报对象的摘要和链接，不存在（已删除）返回 null */
    private Views.ReportTarget target(String type, Long id) {
        if (id == null) return null;
        return switch (type) {
            case Report.TARGET_PHOTOGRAPHER -> {
                User u = userMapper.selectById(id);
                yield u == null ? null : new Views.ReportTarget(u.getNickname(), "/p/" + id, id);
            }
            case Report.TARGET_REVIEW -> {
                Review rv = reviewMapper.selectById(id);
                yield rv == null ? null : new Views.ReportTarget(
                        rv.getScore() + "星：" + (rv.getContent() == null ? "（无文字）" : rv.getContent()),
                        "/p/" + rv.getPhotographerId(), rv.getPhotographerId());
            }
            case Report.TARGET_ALBUM -> {
                Album a = albumMapper.selectById(id);
                yield a == null ? null : new Views.ReportTarget(a.getTitle(), "/albums/" + id, a.getUserId());
            }
            default -> null;
        };
    }
}
