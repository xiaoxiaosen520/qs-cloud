package com.qs.takeout.modules.order.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class OrderItemVO {

    private Long id;
    private Long orderId;
    private Long goodsId;
    private Long skuId;
    private String goodsName;
    private String skuName;
    private BigDecimal price;
    private Integer quantity;
    private String coverUrl;
}
