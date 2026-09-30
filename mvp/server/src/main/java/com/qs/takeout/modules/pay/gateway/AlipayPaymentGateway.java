package com.qs.takeout.modules.pay.gateway;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.domain.AlipayTradeAppPayModel;
import com.alipay.api.domain.AlipayTradeWapPayModel;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradeAppPayRequest;
import com.alipay.api.request.AlipayTradeWapPayRequest;
import com.alipay.api.response.AlipayTradeAppPayResponse;
import com.qs.takeout.common.exception.BizException;
import com.qs.takeout.modules.order.entity.OrderEntity;
import com.qs.takeout.modules.pay.PayChannels;
import com.qs.takeout.modules.pay.PayClients;
import com.qs.takeout.modules.pay.config.PayProperties;
import com.qs.takeout.modules.pay.dto.PayParamsVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

/**
 * 支付宝支付：沙箱/正式 App 支付；H5 走手机网站支付表单。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AlipayPaymentGateway implements PaymentGateway {

    private final PayProperties payProperties;
    private final AlipayClientFactory alipayClientFactory;

    @Override
    public String channel() {
        return PayChannels.ALIPAY;
    }

    @Override
    public boolean isEnabled() {
        PayProperties.Alipay a = payProperties.getAlipay();
        if (a.isEnabled() && configured(a)) {
            return true;
        }
        return a.isDevSimulate();
    }

    public boolean isLiveConfigured() {
        PayProperties.Alipay a = payProperties.getAlipay();
        return a.isEnabled() && configured(a);
    }

    @Override
    public PayParamsVO createPrepay(OrderEntity order, String client) {
        PayProperties.Alipay a = payProperties.getAlipay();
        if (a.isEnabled() && configured(a)) {
            if (PayClients.APP.equals(client)) {
                return createAppPay(order);
            }
            if (PayClients.H5.equals(client)) {
                return createWapPay(order);
            }
            throw new BizException("当前客户端暂不支持支付宝，请使用 App 或 H5");
        }
        if (a.isDevSimulate()) {
            return simulate(order, client);
        }
        throw new BizException("支付宝未开通，请在 application.yml / application-local.yml 配置 qs.pay.alipay");
    }

    private PayParamsVO createAppPay(OrderEntity order) {
        PayProperties.Alipay a = payProperties.getAlipay();
        try {
            AlipayClient client = alipayClientFactory.create();
            AlipayTradeAppPayRequest request = new AlipayTradeAppPayRequest();
            request.setNotifyUrl(notifyUrl());

            AlipayTradeAppPayModel model = new AlipayTradeAppPayModel();
            model.setOutTradeNo(order.getOrderNo());
            model.setTotalAmount(amountOf(order));
            model.setSubject(subjectOf(order));
            model.setProductCode("QUICK_MSECURITY_PAY");
            model.setTimeoutExpress("30m");
            request.setBizModel(model);

            AlipayTradeAppPayResponse response = client.sdkExecute(request);
            if (response == null || !StringUtils.hasText(response.getBody())) {
                throw new BizException("支付宝下单失败：空响应");
            }
            Map<String, String> params = new HashMap<>();
            params.put("orderStr", response.getBody());
            // 安卓 SDK 默认正式环境；沙箱必须客户端 EnvUtils.setEnv(SANDBOX)
            params.put("sandbox", a.isSandbox() ? "true" : "false");
            return PayParamsVO.builder()
                    .mode("ALIPAY_APP")
                    .channel(PayChannels.ALIPAY)
                    .client(PayClients.APP)
                    .orderId(order.getId())
                    .orderNo(order.getOrderNo())
                    .amount(order.getPayAmount())
                    .params(params)
                    .hint(a.isSandbox() ? "沙箱 App 支付：请用沙箱支付宝钱包付款" : "请完成支付宝付款")
                    .build();
        } catch (AlipayApiException e) {
            log.error("alipay app pay failed orderNo={}", order.getOrderNo(), e);
            throw new BizException("支付宝下单失败：" + e.getErrMsg());
        }
    }

    private PayParamsVO createWapPay(OrderEntity order) {
        PayProperties.Alipay a = payProperties.getAlipay();
        try {
            AlipayClient client = alipayClientFactory.create();
            AlipayTradeWapPayRequest request = new AlipayTradeWapPayRequest();
            request.setNotifyUrl(notifyUrl());
            if (StringUtils.hasText(a.getReturnUrl())) {
                request.setReturnUrl(a.getReturnUrl().trim());
            }

            AlipayTradeWapPayModel model = new AlipayTradeWapPayModel();
            model.setOutTradeNo(order.getOrderNo());
            model.setTotalAmount(amountOf(order));
            model.setSubject(subjectOf(order));
            model.setProductCode("QUICK_WAP_WAY");
            model.setTimeoutExpress("30m");
            request.setBizModel(model);

            // pageExecute 得到可跳转的 HTML form / URL
            String formHtml = client.pageExecute(request).getBody();
            Map<String, String> params = new HashMap<>();
            params.put("formHtml", formHtml);
            return PayParamsVO.builder()
                    .mode("ALIPAY_H5")
                    .channel(PayChannels.ALIPAY)
                    .client(PayClients.H5)
                    .orderId(order.getId())
                    .orderNo(order.getOrderNo())
                    .amount(order.getPayAmount())
                    .params(params)
                    .hint(a.isSandbox()
                            ? "沙箱手机网站支付（需已签约手机网站支付；仅 App 支付时请用真机 App 调起）"
                            : "请在支付宝收银台完成付款")
                    .build();
        } catch (AlipayApiException e) {
            log.error("alipay wap pay failed orderNo={}", order.getOrderNo(), e);
            throw new BizException("支付宝 H5 下单失败：" + e.getErrMsg());
        }
    }

    public boolean verifyNotify(Map<String, String> params) {
        PayProperties.Alipay a = payProperties.getAlipay();
        if (!configured(a)) {
            return false;
        }
        try {
            return AlipaySignature.rsaCheckV1(
                    params,
                    AlipayClientFactory.normalizeKey(a.getAlipayPublicKey()),
                    "UTF-8",
                    "RSA2"
            );
        } catch (AlipayApiException e) {
            log.warn("alipay notify verify failed: {}", e.getErrMsg());
            return false;
        }
    }

    private PayParamsVO simulate(OrderEntity order, String client) {
        String mode = PayClients.APP.equals(client) ? "ALIPAY_APP_SIMULATE" : "ALIPAY_H5_SIMULATE";
        Map<String, String> params = new HashMap<>();
        params.put("orderStr", "SIMULATE_" + order.getOrderNo());
        return PayParamsVO.builder()
                .mode(mode)
                .channel(PayChannels.ALIPAY)
                .client(client)
                .orderId(order.getId())
                .orderNo(order.getOrderNo())
                .amount(order.getPayAmount())
                .params(params)
                .hint("模拟支付宝：将自动完成支付（未配沙箱密钥或 enabled=false）")
                .build();
    }

    private String notifyUrl() {
        String base = payProperties.getNotifyBaseUrl();
        if (!StringUtils.hasText(base)) {
            throw new BizException("请配置 qs.pay.notify-base-url 为公网可访问地址");
        }
        String path = payProperties.getAlipay().getNotifyPath();
        if (!StringUtils.hasText(path)) {
            path = "/api/pay/notify/alipay";
        }
        return base.replaceAll("/$", "") + (path.startsWith("/") ? path : "/" + path);
    }

    private static String amountOf(OrderEntity order) {
        if (order.getPayAmount() == null) {
            throw new BizException("订单金额异常");
        }
        return order.getPayAmount().setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static String subjectOf(OrderEntity order) {
        String s = "区惠外卖-" + order.getOrderNo();
        return s.length() > 128 ? s.substring(0, 128) : s;
    }

    private boolean configured(PayProperties.Alipay a) {
        return StringUtils.hasText(a.getAppId())
                && StringUtils.hasText(a.getPrivateKey())
                && StringUtils.hasText(a.getAlipayPublicKey());
    }
}
