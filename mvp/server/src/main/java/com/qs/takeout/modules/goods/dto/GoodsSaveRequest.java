package com.qs.takeout.modules.goods.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class GoodsSaveRequest {

    @NotNull(message = "分类不能为空")
    private Long categoryId;

    @NotBlank(message = "商品名不能为空")
    private String name;

    private String coverUrl;
    private String description;
    private Integer sort;
    /** 1上架 0下架，默认上架 */
    private Integer status;

    @NotEmpty(message = "至少需要一个规格")
    @Valid
    private List<SkuItem> skus;

    @Data
    public static class SkuItem {
        private Long id;

        @NotBlank(message = "规格名不能为空")
        private String name;

        @NotNull(message = "价格不能为空")
        private BigDecimal price;

        /** 便利店请填真实库存；餐饮可填大数 */
        @NotNull(message = "库存不能为空")
        private Integer stock;

        private String barcode;
        private Integer status;
    }
}
