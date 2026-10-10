package com.sheyinglai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("photo")
public class Photo {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long albumId;
    private String title;
    /** 存储中的路径，删除时用，不返回给前端 */
    @JsonIgnore
    private String storageKey;
    /** 原图/大图地址 */
    private String url;
    /** 缩略图地址（列表用） */
    private String thumbUrl;
    private Integer width;
    private Integer height;
    private Long size;
    private LocalDateTime createdAt;
}
