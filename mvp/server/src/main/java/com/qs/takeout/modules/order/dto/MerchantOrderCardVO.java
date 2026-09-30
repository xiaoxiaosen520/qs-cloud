package com.qs.takeout.modules.order.dto;

import com.qs.takeout.modules.order.entity.OrderEntity;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MerchantOrderCardVO {

    private OrderEntity order;
    private Integer itemCount;
    private String itemSummary;
}
