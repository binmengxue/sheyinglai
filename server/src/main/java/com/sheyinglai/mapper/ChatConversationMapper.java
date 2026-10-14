package com.sheyinglai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sheyinglai.entity.ChatConversation;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface ChatConversationMapper extends BaseMapper<ChatConversation> {

    /** 某人所有会话的未读数之和 */
    @Select("SELECT COALESCE(SUM(CASE WHEN user_a = #{uid} THEN unread_a ELSE unread_b END), 0) "
            + "FROM chat_conversation WHERE user_a = #{uid} OR user_b = #{uid}")
    long unreadTotal(@Param("uid") Long uid);
}
