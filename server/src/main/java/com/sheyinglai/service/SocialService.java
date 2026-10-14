package com.sheyinglai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.sheyinglai.common.Auth;
import com.sheyinglai.common.BizException;
import com.sheyinglai.entity.Album;
import com.sheyinglai.entity.AlbumLike;
import com.sheyinglai.entity.Follow;
import com.sheyinglai.entity.Notification;
import com.sheyinglai.mapper.AlbumLikeMapper;
import com.sheyinglai.mapper.AlbumMapper;
import com.sheyinglai.mapper.FollowMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** 关注摄影师、喜欢客片 */
@Service
@RequiredArgsConstructor
public class SocialService {

    private final FollowMapper followMapper;
    private final AlbumLikeMapper likeMapper;
    private final AlbumMapper albumMapper;
    private final AlbumService albumService;
    private final PhotographerService photographerService;
    private final NotificationService notificationService;

    public void follow(Long photographerId) {
        Long uid = Auth.uid();
        if (uid.equals(photographerId)) {
            throw new BizException("不能关注自己");
        }
        photographerService.get(photographerId);
        Follow f = new Follow();
        f.setUserId(uid);
        f.setPhotographerId(photographerId);
        f.setCreatedAt(LocalDateTime.now());
        try {
            followMapper.insert(f);
        } catch (DuplicateKeyException ignored) {
            // 已关注，不再重复通知
            return;
        }
        notificationService.send(photographerId, Notification.TYPE_FOLLOW, "新增粉丝",
                notificationService.nickname(uid) + " 关注了你", "/p/" + uid);
    }

    public void unfollow(Long photographerId) {
        followMapper.delete(new LambdaQueryWrapper<Follow>()
                .eq(Follow::getUserId, Auth.uid()).eq(Follow::getPhotographerId, photographerId));
    }

    @Transactional
    public void like(Long albumId) {
        Long uid = Auth.uid();
        albumService.get(albumId); // 校验可见
        AlbumLike l = new AlbumLike();
        l.setUserId(uid);
        l.setAlbumId(albumId);
        l.setCreatedAt(LocalDateTime.now());
        try {
            likeMapper.insert(l);
        } catch (DuplicateKeyException e) {
            return;
        }
        albumMapper.update(null, new LambdaUpdateWrapper<Album>()
                .setSql("like_count = like_count + 1").eq(Album::getId, albumId));
    }

    @Transactional
    public void unlike(Long albumId) {
        int n = likeMapper.delete(new LambdaQueryWrapper<AlbumLike>()
                .eq(AlbumLike::getUserId, Auth.uid()).eq(AlbumLike::getAlbumId, albumId));
        if (n > 0) {
            albumMapper.update(null, new LambdaUpdateWrapper<Album>()
                    .setSql("like_count = like_count - 1").eq(Album::getId, albumId).gt(Album::getLikeCount, 0));
        }
    }

    /** 我喜欢的客片，按喜欢时间倒序；已隐藏或删除的不再显示 */
    public List<Album> myLikes() {
        List<AlbumLike> likes = likeMapper.selectList(new LambdaQueryWrapper<AlbumLike>()
                .eq(AlbumLike::getUserId, Auth.uid()).orderByDesc(AlbumLike::getId).last("LIMIT 200"));
        if (likes.isEmpty()) return List.of();
        Map<Long, Integer> order = new java.util.HashMap<>();
        for (int i = 0; i < likes.size(); i++) order.put(likes.get(i).getAlbumId(), i);
        List<Album> albums = albumMapper.selectList(new LambdaQueryWrapper<Album>()
                        .in(Album::getId, order.keySet()).eq(Album::getVisible, true))
                .stream().sorted(Comparator.comparing(a -> order.get(a.getId()))).toList();
        albumService.fill(albums);
        return albums;
    }
}
