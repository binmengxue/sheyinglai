package com.sheyinglai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 私信消息 */
@Data
@TableName("chat_message")
public class ChatMessage {
    public static final String TYPE_TEXT = "TEXT";
    /** content 是图片地址 */
    public static final String TYPE_IMAGE = "IMAGE";

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long conversationId;
    private Long senderId;
    private String type;
    private String content;
    private LocalDateTime createdAt;
}
