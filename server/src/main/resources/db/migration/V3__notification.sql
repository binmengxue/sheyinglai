-- 站内消息：预约状态变化、新评价、新粉丝时通知对方
-- 同时兼容 MySQL 8 和 H2（MySQL 模式）

CREATE TABLE notification (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT       NOT NULL,
    type       VARCHAR(32)  NOT NULL,
    title      VARCHAR(64)  NOT NULL,
    content    VARCHAR(255),
    link       VARCHAR(255),
    is_read    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_notification_user ON notification (user_id, is_read);
