package com.qs.takeout.modules.pay.gateway;

import com.qs.takeout.modules.order.entity.OrderEntity;
import com.qs.takeout.modules.pay.dto.PayParamsVO;

public interface PaymentGateway {

    String channel();

    boolean isEnabled();

    PayParamsVO createPrepay(OrderEntity order, String client);
}
