package com.sheyinglai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 私信会话：两人之间只有一个，userA < userB */
@Data
@TableName("chat_conversation")
public class ChatConversation {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userA;
    private Long userB;
    /** 最后一条消息的摘要，会话列表显示 */
    private String lastMsg;
    private LocalDateTime lastAt;
    private Integer unreadA;
    private Integer unreadB;
    private LocalDateTime createdAt;

    public boolean has(Long uid) {
        return uid.equals(userA) || uid.equals(userB);
    }

    public Long peerOf(Long uid) {
        return uid.equals(userA) ? userB : userA;
    }

    public int unreadOf(Long uid) {
        Integer n = uid.equals(userA) ? unreadA : unreadB;
        return n == null ? 0 : n;
    }
}
