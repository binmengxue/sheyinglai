package com.sheyinglai.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.sheyinglai.common.Result;
import com.sheyinglai.dto.AlbumReq;
import com.sheyinglai.entity.Album;
import com.sheyinglai.service.AlbumService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/albums")
@RequiredArgsConstructor
public class AlbumController {

    private final AlbumService albumService;

    /** 某个摄影师的客片，可按分类筛选 */
    @GetMapping
    public Result<List<Album>> list(@RequestParam Long userId, @RequestParam(required = false) String style) {
        return Result.ok(albumService.list(userId, style));
    }

    /** 我的客片（含不公开的），工作台管理用 */
    @SaCheckLogin
    @GetMapping("/mine")
    public Result<List<Album>> mine() {
        return Result.ok(albumService.mine());
    }

    @GetMapping("/{id}")
    public Result<Album> get(@PathVariable Long id) {
        return Result.ok(albumService.get(id));
    }

    @SaCheckLogin
    @PostMapping
    public Result<Album> create(@Valid @RequestBody AlbumReq req) {
        return Result.ok(albumService.create(req));
    }

    @SaCheckLogin
    @PutMapping("/{id}")
    public Result<Album> update(@PathVariable Long id, @Valid @RequestBody AlbumReq req) {
        return Result.ok(albumService.update(id, req));
    }

    @SaCheckLogin
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        albumService.delete(id);
        return Result.ok();
    }

    @SaCheckLogin
    @PutMapping("/{id}/cover/{photoId}")
    public Result<Void> setCover(@PathVariable Long id, @PathVariable Long photoId) {
        albumService.setCover(id, photoId);
        return Result.ok();
    }
}
