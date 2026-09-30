package com.qs.takeout.modules.pay.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("payment_record")
public class PaymentRecord {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private String orderNo;
    private String channel;
    private String tradeNo;
    private BigDecimal amount;
    private String status;
    private String rawNotify;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
