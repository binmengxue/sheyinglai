package com.sheyinglai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.sheyinglai.common.Auth;
import com.sheyinglai.common.BizException;
import com.sheyinglai.common.PageResult;
import com.sheyinglai.dto.PhotographerReq;
import com.sheyinglai.dto.Views;
import com.sheyinglai.entity.Album;
import com.sheyinglai.entity.Follow;
import com.sheyinglai.entity.Photographer;
import com.sheyinglai.entity.User;
import com.sheyinglai.mapper.AlbumMapper;
import com.sheyinglai.mapper.FollowMapper;
import com.sheyinglai.mapper.PhotographerMapper;
import com.sheyinglai.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PhotographerService {

    /** 首页卡片上展示的客片组数 */
    private static final int CARD_ALBUMS = 5;

    private final PhotographerMapper photographerMapper;

    /**
     * 在当前事务里锁住摄影师这一行，让同一摄影师的接单、设休息等操作串行执行，
     * 避免「先查冲突再更新」在并发时同时通过。
     */
    public void lock(Long userId) {
        photographerMapper.selectOne(new LambdaQueryWrapper<Photographer>()
                .eq(Photographer::getUserId, userId).last("FOR UPDATE"));
    }
    private final UserMapper userMapper;
    private final AlbumMapper albumMapper;
    private final AlbumService albumService;
    private final FollowMapper followMapper;
    private final StatService statService;

    /** 没有摄影师资料就建一条默认的（字段默认值见 V2 迁移脚本） */
    public Photographer ensure(Long userId) {
        Photographer p = photographerMapper.selectById(userId);
        if (p == null) {
            p = new Photographer();
            p.setUserId(userId);
            p.setMakeupNote("不提供妆造");
            try {
                photographerMapper.insert(p);
            } catch (DuplicateKeyException ignored) {
                // 并发时另一个请求已经插入
            }
            p = photographerMapper.selectById(userId);
        }
        return p;
    }

    public Photographer updateMine(PhotographerReq req) {
        if (!req.workEnd().isAfter(req.workStart())) {
            throw new BizException("结束时间要晚于开始时间");
        }
        Long uid = Auth.uid();
        ensure(uid);
        String styles = req.styles() == null ? null : req.styles().stream()
                .map(String::trim).filter(StringUtils::hasText).map(s -> s.replace(",", "，"))
                .distinct().collect(Collectors.joining(","));
        photographerMapper.update(null, new LambdaUpdateWrapper<Photographer>()
                .set(Photographer::getTag, req.tag())
                .set(Photographer::getPricePerHour, req.pricePerHour())
                .set(Photographer::getExtraPersonPrice, nz(req.extraPersonPrice()))
                .set(Photographer::getMinHours, req.minHours())
                .set(Photographer::getFreeKm, nz(req.freeKm()))
                .set(Photographer::getKmFee, nz(req.kmFee()))
                .set(Photographer::getRawCount, nz(req.rawCount()))
                .set(Photographer::getRetouchCount, nz(req.retouchCount()))
                .set(Photographer::getRetouchExtraPrice, nz(req.retouchExtraPrice()))
                .set(Photographer::getRetouchRule, req.retouchRule())
                .set(Photographer::getMakeupNote, req.makeupNote())
                .set(Photographer::getEquipment, req.equipment())
                .set(Photographer::getCancelPolicy, req.cancelPolicy() == null ? "NORMAL" : req.cancelPolicy())
                .set(Photographer::getPenaltyPercent, nz(req.penaltyPercent()))
                .set(Photographer::getRefundPromise, Boolean.TRUE.equals(req.refundPromise()))
                .set(Photographer::getStyles, StringUtils.hasText(styles) ? styles : null)
                .set(Photographer::getWorkDays, req.workDays())
                .set(Photographer::getWorkStart, req.workStart())
                .set(Photographer::getWorkEnd, req.workEnd())
                // 经纬度不返回给前端，没重新定位时不传，保留原值
                .set(req.lat() != null && req.lng() != null, Photographer::getLat, req.lat())
                .set(req.lat() != null && req.lng() != null, Photographer::getLng, req.lng())
                .set(Photographer::getAddress, req.address())
                .eq(Photographer::getUserId, uid));
        return photographerMapper.selectById(uid);
    }

    /**
     * 首页摄影师列表。
     * 摄影师数量不大，先全部取出在内存里算距离、排序、分页；量大以后再改成 SQL 计算。
     *
     * @param sort    default 综合（成交多、评分高）/ distance 距离 / rating 评分 / price 价格从低到高
     * @param keyword 按昵称搜索
     * @param city    按城市筛选（用户资料里填的城市，包含即可）
     */
    public PageResult<Views.PhotographerCard> list(Views.PhotographerQuery q, Double lat, Double lng,
                                                   long page, long size) {
        String style = q.style();
        String sort = q.sort();
        List<Photographer> all = photographerMapper.selectList(new LambdaQueryWrapper<Photographer>()
                .eq(Photographer::getVisible, true)
                .gt(Photographer::getPricePerHour, 0)
                .ge(q.minPrice() != null, Photographer::getPricePerHour, q.minPrice())
                .le(q.maxPrice() != null, Photographer::getPricePerHour, q.maxPrice())
                .like(StringUtils.hasText(style), Photographer::getStyles, style));
        Map<Long, User> users = usersOf(all);
        String keyword = StringUtils.hasText(q.keyword()) ? q.keyword().trim() : null;
        String city = StringUtils.hasText(q.city()) ? q.city().trim() : null;
        all = all.stream().filter(p -> {
            User u = users.get(p.getUserId());
            return isNormal(u)
                    && (keyword == null || u.getNickname().contains(keyword))
                    && (city == null || (u.getCity() != null && u.getCity().contains(city)));
        }).toList();

        boolean hasLoc = lat != null && lng != null;
        Map<Long, Double> dist = hasLoc ? all.stream().filter(p -> p.getLat() != null && p.getLng() != null)
                .collect(Collectors.toMap(Photographer::getUserId, p -> distanceKm(lat, lng, p.getLat(), p.getLng())))
                : Map.of();

        Comparator<Photographer> byDefault = Comparator.comparing(Photographer::getDealCount, Comparator.reverseOrder())
                .thenComparing(Photographer::getRatingAvg, Comparator.reverseOrder())
                .thenComparing(Photographer::getUserId, Comparator.reverseOrder());
        Comparator<Photographer> cmp = switch (sort == null ? "" : sort) {
            case "distance" -> hasLoc ? Comparator.<Photographer, Double>comparing(
                    p -> dist.getOrDefault(p.getUserId(), Double.MAX_VALUE)).thenComparing(byDefault) : byDefault;
            case "rating" -> Comparator.comparing(Photographer::getRatingAvg, Comparator.reverseOrder())
                    .thenComparing(Photographer::getRatingCount, Comparator.reverseOrder()).thenComparing(byDefault);
            case "price" -> Comparator.comparing(Photographer::getPricePerHour).thenComparing(byDefault);
            default -> byDefault;
        };
        long pageNo = Math.max(1, page);
        long pageSize = Math.max(1, Math.min(size, 50));
        List<Photographer> pageList = all.stream().sorted(cmp)
                .skip((pageNo - 1) * pageSize).limit(pageSize).toList();

        List<Long> ids = pageList.stream().map(Photographer::getUserId).toList();
        Map<Long, Integer> consult = statService.recentConsult(ids);
        Long viewer = Auth.uidOrNull();
        Map<Long, List<Album>> albums = cardAlbums(ids);
        List<Views.PhotographerCard> cards = pageList.stream()
                .map(p -> card(p, users.get(p.getUserId()), dist.get(p.getUserId()),
                        consult.getOrDefault(p.getUserId(), 0), albums.getOrDefault(p.getUserId(), List.of())))
                .toList();
        ids.stream().filter(id -> !id.equals(viewer)).forEach(id -> statService.incr(id, StatService.Kind.EXPOSURE));
        return new PageResult<>(cards, all.size(), pageNo, pageSize);
    }

    public Views.PhotographerDetail detail(Long id, Double lat, Double lng, boolean track) {
        Photographer p = photographerMapper.selectById(id);
        User user = userMapper.selectById(id);
        if (p == null || user == null) {
            throw new BizException(404, "摄影师不存在");
        }
        boolean self = id.equals(Auth.uidOrNull());
        if (!self && !Auth.isAdmin() && (!Boolean.TRUE.equals(p.getVisible()) || !isNormal(user))) {
            throw new BizException(404, "摄影师不存在");
        }
        Double dist = lat != null && lng != null && p.getLat() != null && p.getLng() != null
                ? distanceKm(lat, lng, p.getLat(), p.getLng()) : null;
        int consult = statService.recentConsult(List.of(id)).getOrDefault(id, 0);

        Long viewer = Auth.uidOrNull();
        long fans = followMapper.selectCount(new LambdaQueryWrapper<Follow>().eq(Follow::getPhotographerId, id));
        long following = followMapper.selectCount(new LambdaQueryWrapper<Follow>().eq(Follow::getUserId, id));
        boolean followed = viewer != null && followMapper.exists(new LambdaQueryWrapper<Follow>()
                .eq(Follow::getUserId, viewer).eq(Follow::getPhotographerId, id));
        // 排名：成交单数比他多的公开摄影师人数 + 1
        int rank = Math.toIntExact(photographerMapper.selectCount(new LambdaQueryWrapper<Photographer>()
                .eq(Photographer::getVisible, true).gt(Photographer::getPricePerHour, 0)
                .gt(Photographer::getDealCount, p.getDealCount()))) + 1;

        boolean manage = Auth.canManage(id);
        List<Views.StyleCount> styleCounts = albumMapper.selectMaps(new QueryWrapper<Album>()
                        .select("style", "COUNT(*) AS cnt").eq("user_id", id)
                        .eq(!manage, "visible", true).groupBy("style"))
                .stream().map(m -> new Views.StyleCount(
                        m.get("style") == null ? "其它" : String.valueOf(m.get("style")),
                        ((Number) m.get("cnt")).longValue()))
                .sorted(Comparator.comparing(Views.StyleCount::count).reversed())
                .toList();

        if (track && !self) {
            statService.incr(id, StatService.Kind.PROFILE_VIEW);
        }
        return new Views.PhotographerDetail(card(p, user, dist, consult, cardAlbums(List.of(id)).getOrDefault(id, List.of())), p, user.getBio(), user.getCity(),
                fans, following, followed, self, rank, styleCounts);
    }

    /** 能被预约：公开、资料完整、账号正常 */
    public Photographer getBookable(Long id) {
        Photographer p = photographerMapper.selectById(id);
        User user = userMapper.selectById(id);
        if (p == null || !p.isReady() || !Boolean.TRUE.equals(p.getVisible()) || !isNormal(user)) {
            throw new BizException("该摄影师暂不接单");
        }
        return p;
    }

    public Photographer get(Long id) {
        Photographer p = photographerMapper.selectById(id);
        if (p == null) {
            throw new BizException(404, "摄影师不存在");
        }
        return p;
    }

    /** 一次查出这些摄影师卡片上要展示的客片（每人前 CARD_ALBUMS 个），避免每张卡片单独查询 */
    private Map<Long, List<Album>> cardAlbums(List<Long> userIds) {
        if (userIds.isEmpty()) return Map.of();
        List<Album> all = albumMapper.selectList(new LambdaQueryWrapper<Album>()
                .in(Album::getUserId, userIds).eq(Album::getVisible, true)
                .isNotNull(Album::getCoverUrl)
                .orderByAsc(Album::getSort).orderByDesc(Album::getId));
        Map<Long, List<Album>> byUser = new HashMap<>();
        for (Album a : all) {
            List<Album> list = byUser.computeIfAbsent(a.getUserId(), k -> new ArrayList<>());
            if (list.size() < CARD_ALBUMS) list.add(a);
        }
        albumService.fill(byUser.values().stream().flatMap(List::stream).toList());
        return byUser;
    }

    private Views.PhotographerCard card(Photographer p, User u, Double dist, int consult, List<Album> albums) {
        List<String> styles = p.getStyles() == null ? List.of()
                : Arrays.stream(p.getStyles().split(",")).filter(StringUtils::hasText).toList();
        return new Views.PhotographerCard(p.getUserId(), u.getNickname(), u.getAvatarUrl(), u.getGender(), p.getTag(),
                p.getRatingAvg(), p.getRatingCount(), p.getDealCount(),
                p.getPricePerHour(), p.getMinHours(), p.getFreeKm(), p.getRawCount(), p.getRetouchCount(),
                styles, p.getWorkDays(), p.getRefundPromise(),
                dist == null ? null : Math.round(dist * 10) / 10.0, consult,
                albums.stream().map(a -> new Views.AlbumBrief(a.getId(), a.getCoverUrl(), a.getPhotoCount())).toList());
    }

    private Map<Long, User> usersOf(List<Photographer> list) {
        List<Long> ids = list.stream().map(Photographer::getUserId).toList();
        if (ids.isEmpty()) return Map.of();
        return userMapper.selectBatchIds(ids).stream().collect(Collectors.toMap(User::getId, u -> u));
    }

    private static boolean isNormal(User u) {
        return u != null && Objects.equals(User.STATUS_NORMAL, u.getStatus());
    }

    private static int nz(Integer v) {
        return v == null ? 0 : v;
    }

    /** 球面距离（公里），Haversine 公式 */
    static double distanceKm(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 6371 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
