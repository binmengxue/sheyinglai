-- 订单取消规则、改期、拍摄提醒；举报
-- 同时兼容 MySQL 8 和 H2（MySQL 模式），每条 ALTER 只改一列

-- 取消原因
ALTER TABLE booking ADD COLUMN cancel_reason VARCHAR(200);
-- 按摄影师的取消规则算出的违约金（线下结算，这里只记录）
ALTER TABLE booking ADD COLUMN penalty_amount INT;
-- 是否已发过拍摄前一天的提醒
ALTER TABLE booking ADD COLUMN reminded BOOLEAN NOT NULL DEFAULT FALSE;
-- 客户改期的次数
ALTER TABLE booking ADD COLUMN reschedule_count INT NOT NULL DEFAULT 0;

CREATE INDEX idx_booking_status_date ON booking (status, shoot_date);

CREATE TABLE report (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    reporter_id BIGINT       NOT NULL,
    target_type VARCHAR(16)  NOT NULL,
    target_id   BIGINT       NOT NULL,
    reason      VARCHAR(32)  NOT NULL,
    content     VARCHAR(500),
    status      VARCHAR(16)  NOT NULL DEFAULT 'PENDING',
    handled_by  BIGINT,
    handle_note VARCHAR(200),
    handled_at  DATETIME,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_report_status ON report (status, id);
