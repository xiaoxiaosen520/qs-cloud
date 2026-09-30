package com.qs.takeout.modules.pay.gateway;

import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.qs.takeout.modules.pay.config.PayProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 按配置构建支付宝客户端（沙箱 / 正式网关）。
 */
@Component
@RequiredArgsConstructor
public class AlipayClientFactory {

    private final PayProperties payProperties;

    public AlipayClient create() {
        PayProperties.Alipay a = payProperties.getAlipay();
        String privateKey = normalizeKey(a.getPrivateKey());
        String alipayPublicKey = normalizeKey(a.getAlipayPublicKey());
        return new DefaultAlipayClient(
                a.resolveGateway(),
                a.getAppId().trim(),
                privateKey,
                "json",
                "UTF-8",
                alipayPublicKey,
                "RSA2"
        );
    }

    /** 去掉 PEM 头尾与空白，SDK 要纯 Base64 体 */
    public static String normalizeKey(String key) {
        if (!StringUtils.hasText(key)) {
            return "";
        }
        return key
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                .replace("-----END RSA PRIVATE KEY-----", "")
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s+", "");
    }
}
