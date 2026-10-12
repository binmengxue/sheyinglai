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
    /** 所属摄影师 */
    private Long userId;
    /** 一组客片的标题，如「晴天·咖啡店」 */
    private String title;
    private String description;
    /** 客片分类：随拍、人像写真、领证照… */
    private String style;
    /** 拍摄地点，如「无锡市·新吴区婚姻登记服务中心」 */
    private String location;
    private String coverUrl;
    /** 是否公开：false 时只有本人和管理员可见 */
    private Boolean visible;
    private Integer sort;
    private Integer likeCount;
    private LocalDateTime createdAt;

    /** 非数据库字段：照片数量 */
    @TableField(exist = false)
    private Long photoCount;

    /** 非数据库字段：当前登录用户是否喜欢 */
    @TableField(exist = false)
    private Boolean liked;
}
