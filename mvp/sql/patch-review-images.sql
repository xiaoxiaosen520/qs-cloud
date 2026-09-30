-- 订单评价支持图片
USE qs_takeout;

ALTER TABLE `order_review`
  ADD COLUMN `image_urls` json NULL COMMENT '评价图片 URL 列表' AFTER `content`;
