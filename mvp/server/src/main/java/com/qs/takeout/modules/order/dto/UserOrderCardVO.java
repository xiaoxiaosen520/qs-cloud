package com.qs.takeout.modules.order.dto;

import com.qs.takeout.modules.order.entity.OrderEntity;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class UserOrderCardVO {

    private OrderEntity order;
    private Long shopId;
    private String shopName;
    private String shopLogoUrl;
    /** 订单商品封面（最多 4 张，按明细顺序） */
    private List<String> goodsCoverUrls;
    private Integer itemCount;
    private String itemSummary;
    private Boolean reviewed;
}
