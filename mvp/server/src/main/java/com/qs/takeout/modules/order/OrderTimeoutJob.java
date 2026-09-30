package com.qs.takeout.modules.order;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qs.takeout.modules.order.entity.OrderEntity;
import com.qs.takeout.modules.order.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单超时扫表：待支付取消、待接单退款、履约超时自动完成。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutJob {

    private static final int BATCH = 50;

    private final OrderMapper orderMapper;
    private final OrderService orderService;

    @Scheduled(fixedDelayString = "${qs.order.timeout-scan-ms:15000}")
    public void scan() {
        LocalDateTime now = LocalDateTime.now();
        closePayTimeouts(now);
        closeAcceptTimeouts(now);
        closeAutoCompletes(now);
    }

    private void closePayTimeouts(LocalDateTime now) {
        List<OrderEntity> list = orderMapper.selectList(new LambdaQueryWrapper<OrderEntity>()
                .eq(OrderEntity::getStatus, OrderStatus.PENDING_PAY)
                .isNotNull(OrderEntity::getPayDeadlineAt)
                .le(OrderEntity::getPayDeadlineAt, now)
                .orderByAsc(OrderEntity::getPayDeadlineAt)
                .last("LIMIT " + BATCH));
        for (OrderEntity o : list) {
            try {
                orderService.closePayTimeout(o.getId());
            } catch (Exception e) {
                log.warn("pay timeout close failed orderId={}: {}", o.getId(), e.getMessage());
            }
        }
    }

    private void closeAcceptTimeouts(LocalDateTime now) {
        List<OrderEntity> list = orderMapper.selectList(new LambdaQueryWrapper<OrderEntity>()
                .eq(OrderEntity::getStatus, OrderStatus.PAID)
                .isNotNull(OrderEntity::getAcceptDeadlineAt)
                .le(OrderEntity::getAcceptDeadlineAt, now)
                .orderByAsc(OrderEntity::getAcceptDeadlineAt)
                .last("LIMIT " + BATCH));
        for (OrderEntity o : list) {
            try {
                orderService.closeAcceptTimeout(o.getId());
            } catch (Exception e) {
                log.warn("accept timeout close failed orderId={}: {}", o.getId(), e.getMessage());
            }
        }
    }

    private void closeAutoCompletes(LocalDateTime now) {
        List<OrderEntity> list = orderMapper.selectList(new LambdaQueryWrapper<OrderEntity>()
                .in(OrderEntity::getStatus, OrderStatus.ACCEPTED, OrderStatus.DELIVERING)
                .isNotNull(OrderEntity::getAutoCompleteAt)
                .le(OrderEntity::getAutoCompleteAt, now)
                .orderByAsc(OrderEntity::getAutoCompleteAt)
                .last("LIMIT " + BATCH));
        for (OrderEntity o : list) {
            try {
                orderService.closeAutoComplete(o.getId());
            } catch (Exception e) {
                log.warn("auto complete failed orderId={}: {}", o.getId(), e.getMessage());
            }
        }
    }
}
