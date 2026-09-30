package com.qs.takeout.modules.goods.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class MerchantGoodsItemVO {

    private Long id;
    private Long categoryId;
    private String name;
    private String coverUrl;
    private String description;
    private BigDecimal minPrice;
    private Integer status;
    /** SKU 库存合计 */
    private Integer stockTotal;
}
