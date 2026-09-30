-- 会话已读水位（各角色各自维护）
ALTER TABLE `im_session`
  ADD COLUMN `user_read_msg_id` bigint NOT NULL DEFAULT 0 COMMENT '用户已读到的最大消息ID' AFTER `rider_id`,
  ADD COLUMN `merchant_read_msg_id` bigint NOT NULL DEFAULT 0 COMMENT '商家已读到的最大消息ID' AFTER `user_read_msg_id`,
  ADD COLUMN `rider_read_msg_id` bigint NOT NULL DEFAULT 0 COMMENT '骑手已读到的最大消息ID' AFTER `merchant_read_msg_id`;
