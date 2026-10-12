package com.sheyinglai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sheyinglai.common.BizException;
import com.sheyinglai.common.PageResult;
import com.sheyinglai.entity.Album;
import com.sheyinglai.entity.Photo;
import com.sheyinglai.mapper.AlbumMapper;
import com.sheyinglai.mapper.PhotoMapper;
import com.sheyinglai.storage.StorageService;
import com.sheyinglai.storage.StoredFile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PhotoService {

    private final PhotoMapper photoMapper;
    private final AlbumMapper albumMapper;
    private final AlbumService albumService;
    private final StorageService storage;

    /** 单张图片上限 1MB，和 application.yml 的 multipart 配置、前端 MAX_MB 保持一致 */
    private static final long MAX_IMAGE_BYTES = 1024 * 1024;

    /** 某个相册里的照片 */
    public PageResult<Photo> page(Long albumId, long page, long size) {
        albumService.get(albumId); // 校验相册可见
        LambdaQueryWrapper<Photo> q = new LambdaQueryWrapper<Photo>()
                .eq(Photo::getAlbumId, albumId).orderByDesc(Photo::getId);
        Page<Photo> p = photoMapper.selectPage(Page.of(Math.max(1, page), Math.max(1, Math.min(size, 100))), q);
        return PageResult.of(p);
    }

    public Photo upload(Long albumId, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new BizException("请选择图片");
        }
        if (file.getSize() > MAX_IMAGE_BYTES) {
            throw new BizException("图片不可以超过 1M");
        }
        Album album = albumService.getOwned(albumId);
        StoredFile f = storage.store(file);

        Photo photo = new Photo();
        photo.setAlbumId(albumId);
        photo.setUserId(album.getUserId());
        photo.setTitle(stripExt(file.getOriginalFilename()));
        photo.setStorageKey(f.key());
        photo.setUrl(f.url());
        photo.setThumbUrl(f.thumbUrl());
        photo.setWidth(f.width());
        photo.setHeight(f.height());
        photo.setSize(f.size());
        photo.setCreatedAt(LocalDateTime.now());
        photoMapper.insert(photo);

        // 相册还没有封面时，用第一张照片做封面
        if (album.getCoverUrl() == null) {
            album.setCoverUrl(photo.getThumbUrl());
            albumMapper.updateById(album);
        }
        return photo;
    }

    public void delete(Long id) {
        Photo photo = photoMapper.selectById(id);
        if (photo == null) {
            throw new BizException(404, "照片不存在");
        }
        albumService.getOwned(photo.getAlbumId());
        storage.delete(photo.getStorageKey());
        photoMapper.deleteById(id);

        // 删掉的是封面图时清空封面（updateById 会忽略 null，所以用 UpdateWrapper）
        albumMapper.update(null, new LambdaUpdateWrapper<Album>()
                .set(Album::getCoverUrl, null)
                .eq(Album::getId, photo.getAlbumId())
                .eq(Album::getCoverUrl, photo.getThumbUrl()));
    }

    private static String stripExt(String name) {
        if (name == null) return null;
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }
}
