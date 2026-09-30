package com.qs.takeout.modules.delivery.dto;

import com.qs.takeout.modules.order.entity.OrderEntity;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
public class RiderOrderVO {

    private OrderEntity order;
    private String shopName;
    private String shopAddress;
    private String shopPhone;
    private BigDecimal shopLat;
    private BigDecimal shopLng;
    private Map<String, Object> address;
    private Integer itemCount;
    private Integer waitMinutes;
    private Integer distanceMeters;
    private BigDecimal income;
    private Boolean pickedUp;
    /** POOL / WAIT_PICKUP / ON_WAY / COMPLETED */
    private String phase;
}
