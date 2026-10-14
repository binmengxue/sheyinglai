-- 补充常用查询的索引（兼容 MySQL 8 和 H2 MySQL 模式）

-- 「我写的点评」按客户查
CREATE INDEX idx_review_customer ON review (customer_id);
-- 删除相册时按相册删喜欢记录、统计喜欢数
CREATE INDEX idx_album_like_album ON album_like (album_id);
