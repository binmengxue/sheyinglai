package com.sheyinglai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sheyinglai.common.BizException;
import com.sheyinglai.dto.AlbumReq;
import com.sheyinglai.entity.Album;
import com.sheyinglai.entity.Photo;
import com.sheyinglai.mapper.AlbumMapper;
import com.sheyinglai.mapper.PhotoMapper;
import com.sheyinglai.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AlbumService {

    private final AlbumMapper albumMapper;
    private final PhotoMapper photoMapper;
    private final StorageService storage;

    /** includeHidden：管理员登录后可以看到不公开的相册 */
    public List<Album> list(boolean includeHidden) {
        List<Album> albums = albumMapper.selectList(new LambdaQueryWrapper<Album>()
                .eq(!includeHidden, Album::getVisible, true)
                .orderByAsc(Album::getSort).orderByDesc(Album::getId));
        albums.forEach(a -> a.setPhotoCount(photoMapper.selectCount(
                new LambdaQueryWrapper<Photo>().eq(Photo::getAlbumId, a.getId()))));
        return albums;
    }

    public Album get(Long id, boolean includeHidden) {
        Album album = albumMapper.selectById(id);
        if (album == null || (!includeHidden && !Boolean.TRUE.equals(album.getVisible()))) {
            throw new BizException(404, "相册不存在");
        }
        return album;
    }

    public Album create(AlbumReq req) {
        Album album = new Album();
        apply(album, req);
        album.setCreatedAt(LocalDateTime.now());
        albumMapper.insert(album);
        return album;
    }

    public Album update(Long id, AlbumReq req) {
        Album album = get(id, true);
        apply(album, req);
        albumMapper.updateById(album);
        return album;
    }

    @Transactional
    public void delete(Long id) {
        get(id, true);
        List<Photo> photos = photoMapper.selectList(new LambdaQueryWrapper<Photo>().eq(Photo::getAlbumId, id));
        photos.forEach(p -> storage.delete(p.getStorageKey()));
        photoMapper.delete(new LambdaQueryWrapper<Photo>().eq(Photo::getAlbumId, id));
        albumMapper.deleteById(id);
    }

    public void setCover(Long albumId, Long photoId) {
        Album album = get(albumId, true);
        Photo photo = photoMapper.selectById(photoId);
        if (photo == null || !photo.getAlbumId().equals(albumId)) {
            throw new BizException("照片不属于该相册");
        }
        album.setCoverUrl(photo.getThumbUrl());
        albumMapper.updateById(album);
    }

    private static void apply(Album album, AlbumReq req) {
        album.setTitle(req.title());
        album.setDescription(req.description());
        album.setVisible(req.visible() == null || req.visible());
        album.setSort(req.sort() == null ? 0 : req.sort());
    }
}
