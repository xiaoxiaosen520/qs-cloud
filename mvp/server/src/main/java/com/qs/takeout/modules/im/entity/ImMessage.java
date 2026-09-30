package com.qs.takeout.modules.im.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("im_message")
public class ImMessage {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long sessionId;
    private String senderRole;
    private Long senderId;
    /** TEXT / IMAGE */
    private String msgType;
    private String content;
    private LocalDateTime createdAt;
}
