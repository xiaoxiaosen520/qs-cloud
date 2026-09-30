package com.qs.takeout.modules.promo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("coupon")
public class Coupon {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private Long shopId;
    private BigDecimal threshold;
    private BigDecimal discount;
    private Integer total;
    private Integer claimed;
    private Integer status;
    private LocalDateTime createdAt;
}
