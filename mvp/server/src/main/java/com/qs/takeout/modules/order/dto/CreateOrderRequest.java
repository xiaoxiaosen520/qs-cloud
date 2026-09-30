package com.qs.takeout.modules.order.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateOrderRequest {

    @NotNull(message = "shopId 不能为空")
    private Long shopId;

    @NotNull(message = "addressId 不能为空")
    private Long addressId;

    private String remark;

    /** true：创建后直接 mock 支付成功（本地联调，兼容旧接口） */
    private Boolean mockPay;

    /** MOCK / WECHAT / ALIPAY，默认 MOCK */
    private String payChannel;

    /** SELF 商家自配 / PLATFORM 蜂鸟众包（默认） */
    private String deliveryType;

    /** 用户持有的优惠券 id（user_coupon.id） */
    private Long userCouponId;
}
