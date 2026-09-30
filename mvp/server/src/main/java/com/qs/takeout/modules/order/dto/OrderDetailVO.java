package com.qs.takeout.modules.order.dto;

import com.qs.takeout.modules.order.entity.OrderEntity;
import com.qs.takeout.modules.order.entity.OrderItemEntity;
import com.qs.takeout.modules.order.entity.OrderReview;
import com.qs.takeout.modules.order.entity.OrderStatusLog;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class OrderDetailVO {

    private OrderEntity order;
    private List<OrderItemVO> items;
    /** 骑手端详情补充 */
    private Map<String, Object> shop;
    private Map<String, Object> address;
    /** 用户端配送追踪：骑手实时位置等 */
    private Map<String, Object> rider;
    /** 蜂鸟发单信息 */
    private Map<String, Object> dispatch;
    private Boolean pickedUp;
    private String phase;
    private Integer distanceMeters;
    private List<OrderStatusLog> logs;
    private Boolean reviewed;
    private OrderReview review;
}
