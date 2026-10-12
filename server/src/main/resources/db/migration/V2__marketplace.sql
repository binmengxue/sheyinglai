-- 多摄影师约拍平台：用户、摄影师资料、档期、预约、评价、关注、喜欢、经营统计
-- 同时兼容 MySQL 8 和 H2（MySQL 模式），每条 ALTER 只改一列

CREATE TABLE app_user (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    phone         VARCHAR(20)  NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    nickname      VARCHAR(32)  NOT NULL,
    avatar_url    VARCHAR(1024),
    gender        INT          NOT NULL DEFAULT 0,
    bio           VARCHAR(500),
    city          VARCHAR(32),
    wechat        VARCHAR(64),
    role          VARCHAR(16)  NOT NULL DEFAULT 'USER',
    status        VARCHAR(16)  NOT NULL DEFAULT 'NORMAL',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_user_phone UNIQUE (phone)
);

CREATE TABLE photographer (
    user_id             BIGINT PRIMARY KEY,
    tag                 VARCHAR(16),
    price_per_hour      INT,
    extra_person_price  INT          NOT NULL DEFAULT 0,
    min_hours           INT          NOT NULL DEFAULT 1,
    free_km             INT          NOT NULL DEFAULT 3,
    km_fee              INT          NOT NULL DEFAULT 0,
    raw_count           INT          NOT NULL DEFAULT 0,
    retouch_count       INT          NOT NULL DEFAULT 0,
    retouch_extra_price INT          NOT NULL DEFAULT 0,
    retouch_rule        VARCHAR(500),
    makeup_note         VARCHAR(200),
    equipment           VARCHAR(500),
    cancel_policy       VARCHAR(16)  NOT NULL DEFAULT 'NORMAL',
    penalty_percent     INT          NOT NULL DEFAULT 0,
    refund_promise      BOOLEAN      NOT NULL DEFAULT FALSE,
    styles              VARCHAR(200),
    work_days           VARCHAR(16)  NOT NULL DEFAULT 'ALL',
    work_start          TIME         NOT NULL DEFAULT '09:00:00',
    work_end            TIME         NOT NULL DEFAULT '19:00:00',
    lat                 DOUBLE,
    lng                 DOUBLE,
    address             VARCHAR(200),
    rating_avg          DECIMAL(3, 2) NOT NULL DEFAULT 0,
    rating_count        INT          NOT NULL DEFAULT 0,
    deal_count          INT          NOT NULL DEFAULT 0,
    visible             BOOLEAN      NOT NULL DEFAULT TRUE
);

ALTER TABLE album ADD COLUMN user_id BIGINT;
ALTER TABLE album ADD COLUMN style VARCHAR(32);
ALTER TABLE album ADD COLUMN location VARCHAR(100);
ALTER TABLE album ADD COLUMN like_count INT NOT NULL DEFAULT 0;
CREATE INDEX idx_album_user ON album (user_id);

ALTER TABLE photo ADD COLUMN user_id BIGINT;
CREATE INDEX idx_photo_user ON photo (user_id);

CREATE TABLE schedule_off (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    photographer_id BIGINT NOT NULL,
    off_date        DATE   NOT NULL,
    CONSTRAINT uk_schedule_off UNIQUE (photographer_id, off_date)
);

CREATE TABLE booking (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id      BIGINT       NOT NULL,
    photographer_id  BIGINT       NOT NULL,
    shoot_date       DATE         NOT NULL,
    start_time       TIME         NOT NULL,
    hours            INT          NOT NULL,
    people           INT          NOT NULL DEFAULT 1,
    address          VARCHAR(200),
    style            VARCHAR(32),
    remark           VARCHAR(500),
    customer_contact VARCHAR(64)  NOT NULL,
    price_estimate   INT          NOT NULL,
    status           VARCHAR(16)  NOT NULL,
    cancelled_by     BIGINT,
    reviewed         BOOLEAN      NOT NULL DEFAULT FALSE,
    responded_at     DATETIME,
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_booking_customer ON booking (customer_id);
CREATE INDEX idx_booking_photographer ON booking (photographer_id, shoot_date);

CREATE TABLE review (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_id      BIGINT       NOT NULL,
    customer_id     BIGINT       NOT NULL,
    photographer_id BIGINT       NOT NULL,
    score           INT          NOT NULL,
    content         VARCHAR(500),
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_review_booking UNIQUE (booking_id)
);
CREATE INDEX idx_review_photographer ON review (photographer_id);

CREATE TABLE follow (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    photographer_id BIGINT NOT NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_follow UNIQUE (user_id, photographer_id)
);
CREATE INDEX idx_follow_photographer ON follow (photographer_id);

CREATE TABLE album_like (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT NOT NULL,
    album_id   BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_album_like UNIQUE (user_id, album_id)
);

CREATE TABLE stat_daily (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    photographer_id BIGINT NOT NULL,
    stat_date       DATE   NOT NULL,
    exposure        INT    NOT NULL DEFAULT 0,
    profile_view    INT    NOT NULL DEFAULT 0,
    consult         INT    NOT NULL DEFAULT 0,
    CONSTRAINT uk_stat_daily UNIQUE (photographer_id, stat_date)
);
