package com.sheyinglai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 用户喜欢的客片（一个相册就是一组客片） */
@Data
@TableName("album_like")
public class AlbumLike {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long albumId;
    private LocalDateTime createdAt;
}
