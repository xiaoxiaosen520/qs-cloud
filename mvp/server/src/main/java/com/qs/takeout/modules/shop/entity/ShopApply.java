package com.qs.takeout.modules.shop.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("shop_apply")
public class ShopApply {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long merchantId;
    private String contactName;
    private String contactPhone;
    private String shopName;
    private String shopType;
    private Long categoryId;
    private String notice;
    private String logoUrl;
    private String withinUrl;
    private String licenseUrl;
    private String idCardFrontUrl;
    private String idCardBackUrl;
    private String address;
    private String houseNumber;
    private BigDecimal lat;
    private BigDecimal lng;
    private String status;
    private String rejectReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
