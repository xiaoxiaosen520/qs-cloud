package com.qs.takeout.modules.goods.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("goods_sku")
public class GoodsSku {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long goodsId;
    private Long shopId;
    private String name;
    private BigDecimal price;
    private Integer stock;
    private String barcode;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
