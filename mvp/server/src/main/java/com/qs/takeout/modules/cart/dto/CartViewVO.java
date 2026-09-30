package com.qs.takeout.modules.cart.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class CartViewVO {

    private Long shopId;
    private String shopName;
    private String shopLogoUrl;
    private List<Line> items;
    private BigDecimal goodsAmount;
    private BigDecimal minOrderAmount;
    private BigDecimal deliveryFee;
    private BigDecimal packingFee;
    /** 预估应付 = 商品 + 配送 + 打包 */
    private BigDecimal payAmount;
    private Boolean meetMinOrder;

    @Data
    @Builder
    public static class Line {
        private Long id;
        private Long goodsId;
        private Long skuId;
        private String goodsName;
        private String skuName;
        private String coverUrl;
        private BigDecimal price;
        private Integer stock;
        private Integer quantity;
        private BigDecimal lineAmount;
    }
}
