package com.sheyinglai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.sheyinglai.common.Auth;
import com.sheyinglai.common.BizException;
import com.sheyinglai.dto.AlbumReq;
import com.sheyinglai.entity.Album;
import com.sheyinglai.entity.AlbumLike;
import com.sheyinglai.entity.Photo;
import com.sheyinglai.entity.Photographer;
import com.sheyinglai.entity.User;
import com.sheyinglai.mapper.AlbumLikeMapper;
import com.sheyinglai.mapper.AlbumMapper;
import com.sheyinglai.mapper.PhotoMapper;
import com.sheyinglai.mapper.PhotographerMapper;
import com.sheyinglai.mapper.UserMapper;
import com.sheyinglai.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** 相册 = 摄影师的一组客片 */
@Service
@RequiredArgsConstructor
public class AlbumService {

    private final AlbumMapper albumMapper;
    private final PhotoMapper photoMapper;
    private final AlbumLikeMapper likeMapper;
    private final UserMapper userMapper;
    private final PhotographerMapper photographerMapper;
    private final StorageService storage;

    /** 某个摄影师的客片；本人和管理员能看到不公开的 */
    public List<Album> list(Long userId, String style) {
        if (!Auth.canManage(userId) && !ownerPublic(userId)) {
            return List.of();
        }
        List<Album> albums = albumMapper.selectList(new LambdaQueryWrapper<Album>()
                .eq(Album::getUserId, userId)
                .eq(!Auth.canManage(userId), Album::getVisible, true)
                .eq(StringUtils.hasText(style), Album::getStyle, style)
                .orderByAsc(Album::getSort).orderByDesc(Album::getId));
        fill(albums);
        return albums;
    }

    public List<Album> mine() {
        return list(Auth.uid(), null);
    }

    public Album get(Long id) {
        Album album = albumMapper.selectById(id);
        if (album == null || !Auth.canManage(album.getUserId())
                && (!Boolean.TRUE.equals(album.getVisible()) || !ownerPublic(album.getUserId()))) {
            throw new BizException(404, "相册不存在");
        }
        fill(List.of(album));
        return album;
    }

    /** 摄影师被封禁或被管理员隐藏后，客片对外也不可见 */
    private boolean ownerPublic(Long userId) {
        User u = userMapper.selectById(userId);
        Photographer p = photographerMapper.selectById(userId);
        return u != null && User.STATUS_NORMAL.equals(u.getStatus()) && p != null && Boolean.TRUE.equals(p.getVisible());
    }

    /** 取出来并校验是本人或管理员 */
    public Album getOwned(Long id) {
        Album album = albumMapper.selectById(id);
        if (album == null) {
            throw new BizException(404, "相册不存在");
        }
        Auth.checkOwner(album.getUserId());
        return album;
    }

    public Album create(AlbumReq req) {
        Album album = new Album();
        album.setUserId(Auth.uid());
        apply(album, req);
        album.setLikeCount(0);
        album.setCreatedAt(LocalDateTime.now());
        albumMapper.insert(album);
        return album;
    }

    public Album update(Long id, AlbumReq req) {
        Album album = getOwned(id);
        apply(album, req);
        // 用 UpdateWrapper 才能把分类、地点、描述清空（updateById 会忽略 null）
        albumMapper.update(null, new LambdaUpdateWrapper<Album>()
                .set(Album::getTitle, album.getTitle())
                .set(Album::getDescription, album.getDescription())
                .set(Album::getStyle, album.getStyle())
                .set(Album::getLocation, album.getLocation())
                .set(Album::getVisible, album.getVisible())
                .set(Album::getSort, album.getSort())
                .eq(Album::getId, id));
        return album;
    }

    @Transactional
    public void delete(Long id) {
        getOwned(id);
        List<String> keys = photoMapper.selectList(new LambdaQueryWrapper<Photo>().eq(Photo::getAlbumId, id))
                .stream().map(Photo::getStorageKey).toList();
        afterCommit(() -> keys.forEach(storage::delete));
        photoMapper.delete(new LambdaQueryWrapper<Photo>().eq(Photo::getAlbumId, id));
        likeMapper.delete(new LambdaQueryWrapper<AlbumLike>().eq(AlbumLike::getAlbumId, id));
        albumMapper.deleteById(id);
    }

    /** 事务提交后再执行（删文件等不能回滚的操作）；不在事务里时立即执行 */
    private static void afterCommit(Runnable task) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            task.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                task.run();
            }
        });
    }

    public void setCover(Long albumId, Long photoId) {
        Album album = getOwned(albumId);
        Photo photo = photoMapper.selectById(photoId);
        if (photo == null || !photo.getAlbumId().equals(albumId)) {
            throw new BizException("照片不属于该相册");
        }
        album.setCoverUrl(photo.getThumbUrl());
        albumMapper.updateById(album);
    }

    /** 补上照片数和「我是否喜欢」，一次查询批量完成 */
    public void fill(List<Album> albums) {
        if (albums.isEmpty()) return;
        List<Long> ids = albums.stream().map(Album::getId).toList();
        Map<Long, Long> counts = photoMapper.selectMaps(new QueryWrapper<Photo>()
                        .select("album_id", "COUNT(*) AS cnt").in("album_id", ids).groupBy("album_id"))
                .stream().collect(Collectors.toMap(m -> toLong(m.get("album_id")), m -> toLong(m.get("cnt"))));
        Long uid = Auth.uidOrNull();
        Set<Long> liked = uid == null ? Set.of() : likeMapper.selectList(new LambdaQueryWrapper<AlbumLike>()
                        .eq(AlbumLike::getUserId, uid).in(AlbumLike::getAlbumId, ids))
                .stream().map(AlbumLike::getAlbumId).collect(Collectors.toSet());
        albums.forEach(a -> {
            a.setPhotoCount(counts.getOrDefault(a.getId(), 0L));
            a.setLiked(liked.contains(a.getId()));
        });
    }

    private static long toLong(Object o) {
        return ((Number) o).longValue();
    }

    private static void apply(Album album, AlbumReq req) {
        album.setTitle(req.title());
        album.setDescription(req.description());
        album.setStyle(StringUtils.hasText(req.style()) ? req.style().trim() : null);
        album.setLocation(StringUtils.hasText(req.location()) ? req.location().trim() : null);
        album.setVisible(req.visible() == null || req.visible());
        album.setSort(req.sort() == null ? 0 : req.sort());
    }
}
