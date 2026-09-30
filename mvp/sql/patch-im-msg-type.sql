-- IM 消息类型：TEXT / IMAGE
ALTER TABLE `im_message`
  ADD COLUMN `msg_type` varchar(16) NOT NULL DEFAULT 'TEXT' COMMENT 'TEXT / IMAGE' AFTER `sender_id`;
