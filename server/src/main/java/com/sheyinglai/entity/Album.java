package com.sheyinglai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("album")
public class Album {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String description;
    private String coverUrl;
    /** 是否公开：false 时只有登录后可见 */
    private Boolean visible;
    private Integer sort;
    private LocalDateTime createdAt;

    /** 非数据库字段：照片数量 */
    @TableField(exist = false)
    private Long photoCount;
}
