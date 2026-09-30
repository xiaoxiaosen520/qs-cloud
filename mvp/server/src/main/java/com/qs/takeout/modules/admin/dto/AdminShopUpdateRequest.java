package com.qs.takeout.modules.admin.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AdminShopUpdateRequest {

    /** 1 正常 / 0 平台下架 */
    private Integer status;
    private Integer openStatus;
    private Long categoryId;
    private BigDecimal minOrderAmount;
    private BigDecimal deliveryFee;
    private BigDecimal packingFee;
    private String notice;
    private String businessHours;
    private String phone;
}
