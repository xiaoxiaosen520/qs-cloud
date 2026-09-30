package com.qs.takeout.modules.im.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ImUnreadVO {

    /** 未读消息总条数 */
    private int unreadMessages;
    /** 有未读的会话数 */
    private int unreadSessions;
}
