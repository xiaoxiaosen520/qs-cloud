package com.qs.takeout.modules.im.dto;

import lombok.Data;

@Data
public class ImReadRequest {

    /** 已读到的消息 ID，缺省则读到当前会话最新一条 */
    private Long lastMsgId;
}
