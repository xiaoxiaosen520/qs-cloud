package com.qs.takeout.modules.promo.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class UserCouponVO {
    private Long id;
    private Long couponId;
    private String name;
    private Long shopId;
    private BigDecimal threshold;
    private BigDecimal discount;
    private String status;
    private LocalDateTime claimedAt;
    private Boolean usable;
}
