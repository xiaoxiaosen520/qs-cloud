package com.qs.takeout.modules.finance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("merchant_billing_record")
public class MerchantBillingRecord {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long merchantId;
    private Long shopId;
    private Long orderId;
    private String orderNo;
    private String type;
    private Integer operateType;
    private BigDecimal amount;
    private BigDecimal fee;
    private String message;
    private LocalDateTime createdAt;
}
