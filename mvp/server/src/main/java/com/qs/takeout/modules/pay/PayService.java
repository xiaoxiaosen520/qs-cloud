package com.qs.takeout.modules.pay;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qs.takeout.common.auth.AuthContext;
import com.qs.takeout.common.auth.AuthUser;
import com.qs.takeout.common.auth.Roles;
import com.qs.takeout.common.exception.BizException;
import com.qs.takeout.modules.order.OrderDeadlineSupport;
import com.qs.takeout.modules.order.OrderStatus;
import com.qs.takeout.modules.order.entity.OrderEntity;
import com.qs.takeout.modules.order.mapper.OrderMapper;
import com.qs.takeout.modules.order.mapper.OrderStatusLogMapper;
import com.qs.takeout.modules.order.entity.OrderStatusLog;
import com.qs.takeout.modules.pay.config.PayProperties;
import com.qs.takeout.modules.pay.dto.PayChannelVO;
import com.qs.takeout.modules.pay.dto.PayParamsVO;
import com.qs.takeout.modules.pay.dto.PrepayRequest;
import com.qs.takeout.modules.pay.entity.PaymentRecord;
import com.qs.takeout.modules.pay.gateway.AlipayPaymentGateway;
import com.qs.takeout.modules.pay.gateway.MockPaymentGateway;
import com.qs.takeout.modules.pay.gateway.PaymentGateway;
import com.qs.takeout.modules.pay.gateway.WechatPaymentGateway;
import com.qs.takeout.modules.pay.mapper.PaymentRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PayService {

    private final PayProperties payProperties;
    private final OrderMapper orderMapper;
    private final PaymentRecordMapper paymentRecordMapper;
    private final OrderStatusLogMapper orderStatusLogMapper;
    private final OrderDeadlineSupport orderDeadlineSupport;
    private final MockPaymentGateway mockPaymentGateway;
    private final WechatPaymentGateway wechatPaymentGateway;
    private final AlipayPaymentGateway alipayPaymentGateway;

    public List<PayChannelVO> listChannels() {
        return List.of(
                channelOf(mockPaymentGateway, "模拟支付", "联调/测试环境"),
                channelOf(wechatPaymentGateway, "微信支付", channelDesc(wechatPaymentGateway, payProperties.getWechat().isEnabled())),
                channelOf(alipayPaymentGateway, "支付宝", channelDesc(alipayPaymentGateway, payProperties.getAlipay().isEnabled()))
        );
    }

    private String channelDesc(PaymentGateway gateway, boolean merchantConfigured) {
        if (gateway == alipayPaymentGateway && alipayPaymentGateway.isLiveConfigured()) {
            return payProperties.getAlipay().isSandbox() ? "沙箱 App/H5 支付已接通" : "正式支付宝已接通";
        }
        if (gateway == wechatPaymentGateway && wechatPaymentGateway.isLiveConfigured()) {
            return "微信 App 支付已接通";
        }
        if (merchantConfigured) {
            return "商户已配置，待完成统一下单接入";
        }
        if (gateway.isEnabled()) {
            return "模拟流程可用，配置商户号后切换真支付";
        }
        return "未开通";
    }

    private PayChannelVO channelOf(PaymentGateway g, String name, String desc) {
        return PayChannelVO.builder()
                .code(g.channel())
                .name(name)
                .enabled(g.isEnabled())
                .desc(desc)
                .build();
    }

    public PayParamsVO prepay(PrepayRequest req) {
        OrderEntity order = requireOwnPendingPay(req.getOrderId());
        PaymentGateway gateway = gatewayOf(req.getChannel());
        if (!gateway.isEnabled()) {
            throw new BizException("该支付方式暂未开通");
        }
        updatePendingPaymentChannel(order.getId(), req.getChannel());
        return gateway.createPrepay(order, req.getClient());
    }

    @Transactional
    public void confirmMockPay(Long orderId) {
        AuthUser user = AuthContext.require();
        OrderEntity order = orderMapper.selectById(orderId);
        if (order == null || !user.getId().equals(order.getUserId())) {
            throw new BizException("订单不存在");
        }
        markPaid(orderId, PayChannels.MOCK, "MOCK" + System.currentTimeMillis(), Roles.USER, user.getId(), "mock支付成功");
    }

    @Transactional
    public void handleNotify(String channel, String tradeNo, String rawBody, Long orderIdFromNotify) {
        if (!StringUtils.hasText(tradeNo)) {
            tradeNo = channel + System.currentTimeMillis();
        }
        markPaid(orderIdFromNotify, channel, tradeNo, "SYSTEM", null, channel + "支付回调");
        PaymentRecord pay = latestPayment(orderIdFromNotify);
        if (pay != null) {
            pay.setRawNotify(rawBody == null ? "" : rawBody);
            paymentRecordMapper.updateById(pay);
        }
    }

    /**
     * 支付宝异步通知：验签后按 out_trade_no 标记已支付。
     * @return 应回写给支付宝的字符串 success / failure
     */
    @Transactional
    public String handleAlipayNotify(Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return "failure";
        }
        if (!alipayPaymentGateway.verifyNotify(params)) {
            return "failure";
        }
        String tradeStatus = params.get("trade_status");
        if (!"TRADE_SUCCESS".equals(tradeStatus) && !"TRADE_FINISHED".equals(tradeStatus)) {
            return "success";
        }
        String outTradeNo = params.get("out_trade_no");
        String tradeNo = params.get("trade_no");
        if (!StringUtils.hasText(outTradeNo)) {
            return "failure";
        }
        OrderEntity order = orderMapper.selectOne(new LambdaQueryWrapper<OrderEntity>()
                .eq(OrderEntity::getOrderNo, outTradeNo)
                .last("limit 1"));
        if (order == null) {
            return "failure";
        }
        String totalAmount = params.get("total_amount");
        if (StringUtils.hasText(totalAmount) && order.getPayAmount() != null) {
            if (order.getPayAmount().setScale(2, java.math.RoundingMode.HALF_UP)
                    .compareTo(new java.math.BigDecimal(totalAmount)) != 0) {
                return "failure";
            }
        }
        String raw = params.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("&"));
        handleNotify(PayChannels.ALIPAY, tradeNo, raw, order.getId());
        return "success";
    }

    /**
     * 微信支付异步通知：验签解密后按 out_trade_no 标记已支付。
     * @return SUCCESS / FAIL（由 Controller 包装 JSON）
     */
    @Transactional
    public String handleWechatNotify(String body, String timestamp, String nonce, String signature, String serial) {
        try {
            com.wechat.pay.java.service.payments.model.Transaction tx =
                    wechatPaymentGateway.parseNotify(body, timestamp, nonce, signature, serial);
            if (tx == null) {
                return "FAIL";
            }
            if (tx.getTradeState() != com.wechat.pay.java.service.payments.model.Transaction.TradeStateEnum.SUCCESS) {
                return "SUCCESS";
            }
            String outTradeNo = tx.getOutTradeNo();
            String tradeNo = tx.getTransactionId();
            if (!StringUtils.hasText(outTradeNo)) {
                return "FAIL";
            }
            OrderEntity order = orderMapper.selectOne(new LambdaQueryWrapper<OrderEntity>()
                    .eq(OrderEntity::getOrderNo, outTradeNo)
                    .last("limit 1"));
            if (order == null) {
                return "FAIL";
            }
            if (tx.getAmount() != null && tx.getAmount().getTotal() != null && order.getPayAmount() != null) {
                int fen = order.getPayAmount().setScale(2, java.math.RoundingMode.HALF_UP)
                        .multiply(new java.math.BigDecimal(100)).intValueExact();
                if (!tx.getAmount().getTotal().equals(fen)) {
                    return "FAIL";
                }
            }
            handleNotify(PayChannels.WECHAT, tradeNo, body, order.getId());
            return "SUCCESS";
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(PayService.class)
                    .warn("wechat notify handle failed: {}", e.toString());
            return "FAIL";
        }
    }

    @Transactional
    public void markPaid(Long orderId, String channel, String tradeNo, String operatorType, Long operatorId, String remark) {
        OrderEntity order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException("订单不存在");
        }
        if (OrderStatus.PAID.equals(order.getStatus())) {
            return;
        }
        if (!OrderStatus.PENDING_PAY.equals(order.getStatus())) {
            throw new BizException("订单状态不可支付");
        }
        LocalDateTime acceptDeadline = orderDeadlineSupport.acceptDeadlineFromNow();
        int n = orderMapper.casMarkPaid(orderId, acceptDeadline);
        if (n != 1) {
            // 可能已被超时任务取消
            OrderEntity fresh = orderMapper.selectById(orderId);
            if (fresh != null && OrderStatus.PAID.equals(fresh.getStatus())) {
                return;
            }
            throw new BizException("订单已关闭，无法支付");
        }
        logStatus(orderId, OrderStatus.PENDING_PAY, OrderStatus.PAID, operatorType, operatorId, remark);

        PaymentRecord pay = latestPayment(orderId);
        if (pay == null) {
            pay = new PaymentRecord();
            pay.setOrderId(orderId);
            pay.setOrderNo(order.getOrderNo());
            pay.setAmount(order.getPayAmount());
            pay.setStatus("PENDING");
            paymentRecordMapper.insert(pay);
        }
        pay.setChannel(channel);
        pay.setTradeNo(tradeNo);
        pay.setStatus("SUCCESS");
        paymentRecordMapper.updateById(pay);
    }

    public void createPendingPayment(OrderEntity order, String channel) {
        PaymentRecord pay = new PaymentRecord();
        pay.setOrderId(order.getId());
        pay.setOrderNo(order.getOrderNo());
        pay.setChannel(StringUtils.hasText(channel) ? channel : PayChannels.MOCK);
        pay.setTradeNo("");
        pay.setAmount(order.getPayAmount());
        pay.setStatus("PENDING");
        paymentRecordMapper.insert(pay);
    }

    private void updatePendingPaymentChannel(Long orderId, String channel) {
        PaymentRecord pay = latestPayment(orderId);
        if (pay != null && "PENDING".equals(pay.getStatus())) {
            pay.setChannel(channel);
            paymentRecordMapper.updateById(pay);
        }
    }

    private PaymentRecord latestPayment(Long orderId) {
        return paymentRecordMapper.selectOne(new LambdaQueryWrapper<PaymentRecord>()
                .eq(PaymentRecord::getOrderId, orderId)
                .orderByDesc(PaymentRecord::getId)
                .last("limit 1"));
    }

    private OrderEntity requireOwnPendingPay(Long orderId) {
        AuthUser user = AuthContext.require();
        OrderEntity order = orderMapper.selectById(orderId);
        if (order == null || !user.getId().equals(order.getUserId())) {
            throw new BizException("订单不存在");
        }
        if (!OrderStatus.PENDING_PAY.equals(order.getStatus())) {
            throw new BizException("订单状态不可支付");
        }
        return order;
    }

    private PaymentGateway gatewayOf(String channel) {
        Map<String, PaymentGateway> map = List.of(mockPaymentGateway, wechatPaymentGateway, alipayPaymentGateway)
                .stream()
                .collect(Collectors.toMap(PaymentGateway::channel, Function.identity()));
        PaymentGateway g = map.get(channel == null ? "" : channel.toUpperCase());
        if (g == null) {
            throw new BizException("不支持的支付方式");
        }
        return g;
    }

    private void logStatus(Long orderId, String from, String to, String operatorType, Long operatorId, String remark) {
        OrderStatusLog log = new OrderStatusLog();
        log.setOrderId(orderId);
        log.setFromStatus(from == null ? "" : from);
        log.setToStatus(to);
        log.setOperatorType(operatorType == null ? "SYSTEM" : operatorType);
        log.setOperatorId(operatorId);
        log.setRemark(remark == null ? "" : remark);
        orderStatusLogMapper.insert(log);
    }
}
