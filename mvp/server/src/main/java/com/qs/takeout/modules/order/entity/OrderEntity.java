package com.qs.takeout.modules.order.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("orders")
public class OrderEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderNo;
    private Long userId;
    private Long shopId;
    private Long riderId;
    private String status;
    private String deliveryType;
    private BigDecimal goodsAmount;
    private BigDecimal packingFee;
    private BigDecimal deliveryFee;
    private BigDecimal discountAmount;
    private Long userCouponId;
    private BigDecimal payAmount;
    private String remark;
    private String addressSnapshot;
    private LocalDateTime paidAt;
    private LocalDateTime acceptedAt;
    private LocalDateTime pickedUpAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime completedAt;
    /** 待支付超时时刻（允许 update 写 null 清掉） */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime payDeadlineAt;
    /** 待接单超时时刻 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime acceptDeadlineAt;
    /** 履约超时自动完成时刻 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime autoCompleteAt;
    private String cancelReason;
    private String refundReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
