package com.qs.takeout.modules.pay.gateway;

import com.qs.takeout.common.exception.BizException;
import com.qs.takeout.modules.order.entity.OrderEntity;
import com.qs.takeout.modules.pay.PayChannels;
import com.qs.takeout.modules.pay.PayClients;
import com.qs.takeout.modules.pay.config.PayProperties;
import com.qs.takeout.modules.pay.dto.PayParamsVO;
import com.wechat.pay.java.core.Config;
import com.wechat.pay.java.core.exception.ServiceException;
import com.wechat.pay.java.core.notification.RequestParam;
import com.wechat.pay.java.service.payments.app.AppServiceExtension;
import com.wechat.pay.java.service.payments.app.model.Amount;
import com.wechat.pay.java.service.payments.app.model.PrepayRequest;
import com.wechat.pay.java.service.payments.app.model.PrepayWithRequestPaymentResponse;
import com.wechat.pay.java.service.payments.model.Transaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 微信支付：App 统一下单（API v3）；回调验签解密。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WechatPaymentGateway implements PaymentGateway {

    private final PayProperties payProperties;
    private final WechatPayClientFactory wechatPayClientFactory;

    @Override
    public String channel() {
        return PayChannels.WECHAT;
    }

    @Override
    public boolean isEnabled() {
        PayProperties.Wechat w = payProperties.getWechat();
        if (w.isEnabled() && configured(w)) {
            return true;
        }
        return w.isDevSimulate();
    }

    public boolean isLiveConfigured() {
        PayProperties.Wechat w = payProperties.getWechat();
        return w.isEnabled() && configured(w);
    }

    @Override
    public PayParamsVO createPrepay(OrderEntity order, String client) {
        PayProperties.Wechat w = payProperties.getWechat();
        if (w.isEnabled() && configured(w)) {
            if (PayClients.APP.equals(client)) {
                return createAppPay(order, w);
            }
            throw new BizException("当前仅开通微信 App 支付；小程序/H5 需另接 JSAPI/H5");
        }
        if (w.isDevSimulate()) {
            return simulate(order, client);
        }
        throw new BizException("微信支付未开通，请在 application.yml 配置 qs.pay.wechat");
    }

    private PayParamsVO createAppPay(OrderEntity order, PayProperties.Wechat w) {
        try {
            Config config = wechatPayClientFactory.config();
            AppServiceExtension service = new AppServiceExtension.Builder().config(config).build();

            PrepayRequest request = new PrepayRequest();
            request.setAppid(w.getAppId().trim());
            request.setMchid(w.getMchId().trim());
            request.setDescription(subjectOf(order));
            request.setOutTradeNo(order.getOrderNo());
            request.setNotifyUrl(notifyUrl());
            Amount amount = new Amount();
            amount.setTotal(toFen(order.getPayAmount()));
            amount.setCurrency("CNY");
            request.setAmount(amount);

            PrepayWithRequestPaymentResponse resp = service.prepayWithRequestPayment(request);
            Map<String, String> params = new HashMap<>();
            params.put("appid", nullToEmpty(resp.getAppid()));
            params.put("partnerid", nullToEmpty(resp.getPartnerId()));
            params.put("prepayid", nullToEmpty(resp.getPrepayId()));
            params.put("package", nullToEmpty(resp.getPackageVal()));
            params.put("noncestr", nullToEmpty(resp.getNonceStr()));
            params.put("timestamp", nullToEmpty(resp.getTimestamp()));
            params.put("sign", nullToEmpty(resp.getSign()));

            return PayParamsVO.builder()
                    .mode("WECHAT_APP")
                    .channel(PayChannels.WECHAT)
                    .client(PayClients.APP)
                    .orderId(order.getId())
                    .orderNo(order.getOrderNo())
                    .amount(order.getPayAmount())
                    .params(params)
                    .hint("请完成微信支付")
                    .build();
        } catch (ServiceException e) {
            log.error("wechat app prepay failed orderNo={} code={} msg={}",
                    order.getOrderNo(), e.getErrorCode(), e.getErrorMessage());
            throw new BizException("微信下单失败：" + e.getErrorMessage());
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("wechat app prepay error orderNo={}", order.getOrderNo(), e);
            throw new BizException("微信下单失败：" + e.getMessage());
        }
    }

    /**
     * 验签并解密支付通知。
     * @return 解密后的交易，失败抛 BizException
     */
    public Transaction parseNotify(String body, String timestamp, String nonce, String signature, String serial) {
        if (!StringUtils.hasText(body)
                || !StringUtils.hasText(timestamp)
                || !StringUtils.hasText(nonce)
                || !StringUtils.hasText(signature)
                || !StringUtils.hasText(serial)) {
            throw new BizException("微信回调头缺失");
        }
        RequestParam requestParam = new RequestParam.Builder()
                .serialNumber(serial)
                .nonce(nonce)
                .signature(signature)
                .timestamp(timestamp)
                .body(body)
                .build();
        return wechatPayClientFactory.notificationParser().parse(requestParam, Transaction.class);
    }

    private PayParamsVO simulate(OrderEntity order, String client) {
        String mode = switch (client == null ? "" : client) {
            case PayClients.MP_WEIXIN -> "WECHAT_MP_SIMULATE";
            case PayClients.APP -> "WECHAT_APP_SIMULATE";
            case PayClients.H5 -> "WECHAT_H5_SIMULATE";
            default -> "WECHAT_APP_SIMULATE";
        };
        Map<String, String> params = new HashMap<>();
        params.put("timeStamp", String.valueOf(System.currentTimeMillis() / 1000));
        params.put("nonceStr", UUID.randomUUID().toString().replace("-", ""));
        params.put("package", "prepay_id=SIMULATE_" + order.getOrderNo());
        params.put("signType", "RSA");
        params.put("paySign", "SIMULATE");
        return PayParamsVO.builder()
                .mode(mode)
                .channel(PayChannels.WECHAT)
                .client(client)
                .orderId(order.getId())
                .orderNo(order.getOrderNo())
                .amount(order.getPayAmount())
                .params(params)
                .hint("模拟微信支付：将自动完成支付（未接真商户）")
                .build();
    }

    private String notifyUrl() {
        String base = payProperties.getNotifyBaseUrl();
        if (!StringUtils.hasText(base)) {
            throw new BizException("请配置 qs.pay.notify-base-url 为公网可访问地址");
        }
        String path = payProperties.getWechat().getNotifyPath();
        if (!StringUtils.hasText(path)) {
            path = "/api/pay/notify/wechat";
        }
        return base.replaceAll("/$", "") + (path.startsWith("/") ? path : "/" + path);
    }

    private static int toFen(BigDecimal yuan) {
        if (yuan == null || yuan.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("订单金额异常");
        }
        return yuan.setScale(2, RoundingMode.HALF_UP).multiply(new BigDecimal(100)).intValueExact();
    }

    private static String subjectOf(OrderEntity order) {
        String s = "区惠外卖-" + order.getOrderNo();
        return s.length() > 127 ? s.substring(0, 127) : s;
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private boolean configured(PayProperties.Wechat w) {
        return StringUtils.hasText(w.getAppId())
                && StringUtils.hasText(w.getMchId())
                && StringUtils.hasText(w.getApiV3Key())
                && StringUtils.hasText(w.getSerialNo())
                && w.hasPrivateKey();
    }
}
