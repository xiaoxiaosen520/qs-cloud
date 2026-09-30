package com.qs.takeout.modules.finance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("rider_billing_record")
public class RiderBillingRecord {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long riderId;
    private Long orderId;
    private String orderNo;
    private String type;
    private Integer operateType;
    private BigDecimal amount;
    private BigDecimal fee;
    private String message;
    private LocalDateTime createdAt;
}
