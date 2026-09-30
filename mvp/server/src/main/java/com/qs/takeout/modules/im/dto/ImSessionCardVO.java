package com.qs.takeout.modules.im.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ImSessionCardVO {

    private Long sessionId;
    private Long orderId;
    private String orderNo;
    private String sessionType;
    private String status;
    private String title;
    private String lastContent;
    private String lastSenderRole;
    private String updatedAt;
    private boolean canSend;
    /** 对我方未读条数 */
    private int unreadCount;
    /** 最后一条是否对方发的 */
    private boolean lastFromPeer;
}
