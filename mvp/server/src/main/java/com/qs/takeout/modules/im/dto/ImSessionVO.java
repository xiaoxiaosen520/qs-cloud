package com.qs.takeout.modules.im.dto;

import com.qs.takeout.modules.im.entity.ImMessage;
import com.qs.takeout.modules.im.entity.ImSession;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ImSessionVO {

    private ImSession session;
    private String title;
    private String orderNo;
    private String orderStatus;
    private boolean canSend;
    /** 对方已读到的最大消息 id，用于气泡「已读/未读」 */
    private Long peerReadMsgId;
    /** 对方电话，用于一键拨号 */
    private String peerPhone;
    private String peerLabel;
    private List<ImMessage> messages;
}
