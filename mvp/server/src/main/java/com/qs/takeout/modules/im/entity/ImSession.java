package com.qs.takeout.modules.im.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("im_session")
public class ImSession {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private String sessionType;
    private String status;
    private Long userId;
    private Long shopId;
    private Long merchantId;
    private Long riderId;
    private Long userReadMsgId;
    private Long merchantReadMsgId;
    private Long riderReadMsgId;
    private LocalDateTime closedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
