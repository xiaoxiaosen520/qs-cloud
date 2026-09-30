package com.qs.takeout.modules.pay.gateway;

import com.qs.takeout.modules.order.entity.OrderEntity;
import com.qs.takeout.modules.pay.PayChannels;
import com.qs.takeout.modules.pay.config.PayProperties;
import com.qs.takeout.modules.pay.dto.PayParamsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MockPaymentGateway implements PaymentGateway {

    private final PayProperties payProperties;

    @Override
    public String channel() {
        return PayChannels.MOCK;
    }

    @Override
    public boolean isEnabled() {
        return payProperties.isMockEnabled();
    }

    @Override
    public PayParamsVO createPrepay(OrderEntity order, String client) {
        return PayParamsVO.builder()
                .mode("MOCK")
                .channel(PayChannels.MOCK)
                .client(client)
                .orderId(order.getId())
                .orderNo(order.getOrderNo())
                .amount(order.getPayAmount())
                .hint("联调环境：确认后将立即标记为已支付")
                .build();
    }
}
