package com.qs.takeout.modules.finance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("rider_withdraw_record")
public class RiderWithdrawRecord {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long riderId;
    private String withdrawNo;
    private BigDecimal amount;
    private BigDecimal fee;
    private BigDecimal actualAmount;
    private String paymentMode;
    private String accountSnapshot;
    private String auditStatus;
    private String auditReason;
    private LocalDateTime auditedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
