package com.qs.takeout.modules.shop.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("shop")
public class Shop {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String shopType;
    private Long categoryId;
    private String logoUrl;
    private String withinUrl;
    private String licenseUrl;
    private String idCardFrontUrl;
    private String idCardBackUrl;
    private String notice;
    private String address;
    private BigDecimal lat;
    private BigDecimal lng;
    private String phone;
    /** 蜂鸟门店编码，空则用 SHOP_{id} */
    private String fengniaoStoreCode;
    private BigDecimal minOrderAmount;
    private BigDecimal deliveryFee;
    private BigDecimal packingFee;
    private String businessHours;
    private Integer openStatus;
    private Integer status;
    private BigDecimal score;
    private Integer monthSales;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
