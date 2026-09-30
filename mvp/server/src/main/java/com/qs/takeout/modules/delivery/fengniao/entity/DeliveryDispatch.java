package com.qs.takeout.modules.delivery.fengniao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("delivery_dispatch")
public class DeliveryDispatch {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private String orderNo;
    private String channel;
    private String partnerOrderCode;
    private String trackingNo;
    private String storeCode;
    private String status;
    private Integer anubisStatus;
    private String riderName;
    private String riderPhone;
    private BigDecimal fee;
    private String failReason;
    private Integer mock;
    private LocalDateTime nextMockAt;
    private String lastNotifyJson;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
