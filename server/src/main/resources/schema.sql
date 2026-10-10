-- 同时兼容 MySQL 8 和 H2（MySQL 模式）
CREATE TABLE IF NOT EXISTS album (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    title       VARCHAR(64)  NOT NULL,
    description VARCHAR(500),
    cover_url   VARCHAR(1024),
    visible     BOOLEAN      NOT NULL DEFAULT TRUE,
    sort        INT          NOT NULL DEFAULT 0,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS photo (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    album_id    BIGINT        NOT NULL,
    title       VARCHAR(255),
    storage_key VARCHAR(255)  NOT NULL,
    url         VARCHAR(1024) NOT NULL,
    thumb_url   VARCHAR(1024),
    width       INT,
    height      INT,
    size        BIGINT,
    created_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_photo_album (album_id)
);
