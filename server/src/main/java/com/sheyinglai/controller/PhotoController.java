package com.sheyinglai.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.sheyinglai.common.PageResult;
import com.sheyinglai.common.Result;
import com.sheyinglai.entity.Photo;
import com.sheyinglai.service.PhotoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/photos")
@RequiredArgsConstructor
public class PhotoController {

    private final PhotoService photoService;

    /** 分页查询某个相册的照片 */
    @GetMapping
    public Result<PageResult<Photo>> page(@RequestParam Long albumId,
                                          @RequestParam(defaultValue = "1") long page,
                                          @RequestParam(defaultValue = "30") long size) {
        return Result.ok(photoService.page(albumId, page, size));
    }

    /** 上传单张图片（前端多张时逐张调用，自己控制并发） */
    @SaCheckLogin
    @PostMapping
    public Result<Photo> upload(@RequestParam Long albumId, @RequestParam("file") MultipartFile file) throws IOException {
        return Result.ok(photoService.upload(albumId, file));
    }

    @SaCheckLogin
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        photoService.delete(id);
        return Result.ok();
    }
}
