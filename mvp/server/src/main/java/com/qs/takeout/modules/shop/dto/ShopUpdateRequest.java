package com.qs.takeout.modules.shop.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ShopUpdateRequest {

    private String notice;
    private String businessHours;
    private String logoUrl;
    private BigDecimal minOrderAmount;
    private BigDecimal deliveryFee;
    private BigDecimal packingFee;

    @NotNull(message = "openStatus 不能为空")
    private Integer openStatus;
}
