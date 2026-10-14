-- 私信（聊一聊）：两人之间一个会话，消息按会话存
-- 同时兼容 MySQL 8 和 H2（MySQL 模式）

-- user_a < user_b，保证同一对用户只有一个会话；unread_a / unread_b 是各自的未读数
CREATE TABLE chat_conversation (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_a     BIGINT       NOT NULL,
    user_b     BIGINT       NOT NULL,
    last_msg   VARCHAR(255),
    last_at    DATETIME,
    unread_a   INT          NOT NULL DEFAULT 0,
    unread_b   INT          NOT NULL DEFAULT 0,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_chat_pair UNIQUE (user_a, user_b)
);

CREATE INDEX idx_chat_conv_b ON chat_conversation (user_b);

-- type：TEXT 文字，IMAGE 图片（content 是图片地址）
CREATE TABLE chat_message (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    conversation_id BIGINT        NOT NULL,
    sender_id       BIGINT        NOT NULL,
    type            VARCHAR(16)   NOT NULL,
    content         VARCHAR(1000) NOT NULL,
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_chat_msg_conv ON chat_message (conversation_id, id);
