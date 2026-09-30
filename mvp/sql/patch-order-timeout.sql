-- 订单超时：deadline 字段 + 配置（可对已有库执行；重复执行可能报已存在，可忽略）
USE qs_takeout;

ALTER TABLE `orders`
  ADD COLUMN `pay_deadline_at` datetime(0) NULL DEFAULT NULL COMMENT '待支付超时时刻' AFTER `completed_at`,
  ADD COLUMN `accept_deadline_at` datetime(0) NULL DEFAULT NULL COMMENT '待接单超时时刻' AFTER `pay_deadline_at`,
  ADD COLUMN `auto_complete_at` datetime(0) NULL DEFAULT NULL COMMENT '履约超时自动完成时刻' AFTER `accept_deadline_at`;

ALTER TABLE `orders` ADD INDEX `idx_pay_deadline` (`status`, `pay_deadline_at`);
ALTER TABLE `orders` ADD INDEX `idx_accept_deadline` (`status`, `accept_deadline_at`);
ALTER TABLE `orders` ADD INDEX `idx_auto_complete` (`status`, `auto_complete_at`);

INSERT INTO `sys_config` (`config_key`, `config_value`, `remark`) VALUES
('accept_timeout_minutes', '5', '已支付待接单自动退款分钟'),
('auto_complete_minutes', '240', '配送中/自配履约超时自动完成分钟')
ON DUPLICATE KEY UPDATE `remark` = VALUES(`remark`);
