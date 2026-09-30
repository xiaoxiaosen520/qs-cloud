-- 订单临时 1v1 会话（用户↔商家 / 用户↔骑手）
-- 可在已有库执行：docker exec -i mysql-old-data mysql -uroot -proot qs_takeout < mvp/sql/patch-im.sql

CREATE TABLE IF NOT EXISTS `im_session` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_id` bigint NOT NULL,
  `session_type` varchar(32) NOT NULL COMMENT 'USER_MERCHANT / USER_RIDER',
  `status` varchar(16) NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN / CLOSED',
  `user_id` bigint NOT NULL,
  `shop_id` bigint NOT NULL,
  `merchant_id` bigint NULL DEFAULT NULL,
  `rider_id` bigint NULL DEFAULT NULL,
  `user_read_msg_id` bigint NOT NULL DEFAULT 0 COMMENT '用户已读到的最大消息ID',
  `merchant_read_msg_id` bigint NOT NULL DEFAULT 0 COMMENT '商家已读到的最大消息ID',
  `rider_read_msg_id` bigint NOT NULL DEFAULT 0 COMMENT '骑手已读到的最大消息ID',
  `closed_at` datetime NULL DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_type` (`order_id`, `session_type`),
  KEY `idx_user` (`user_id`, `updated_at`),
  KEY `idx_merchant` (`merchant_id`, `updated_at`),
  KEY `idx_rider` (`rider_id`, `updated_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单临时会话';

CREATE TABLE IF NOT EXISTS `im_message` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `session_id` bigint NOT NULL,
  `sender_role` varchar(16) NOT NULL COMMENT 'USER / MERCHANT / RIDER / SYSTEM',
  `sender_id` bigint NOT NULL DEFAULT 0,
  `content` varchar(1000) NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_session` (`session_id`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会话消息';
