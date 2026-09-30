package com.qs.takeout.modules.pay.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Data
@Component
@ConfigurationProperties(prefix = "qs.pay")
public class PayProperties {

    /** 是否开放 Mock 支付（联调） */
    private boolean mockEnabled = true;

    private Wechat wechat = new Wechat();
    private Alipay alipay = new Alipay();

    /** 支付回调公网基址，如 https://api.example.com */
    private String notifyBaseUrl = "http://127.0.0.1:8080";

    @Data
    public static class Wechat {
        private boolean enabled = false;
        /** 未接真商户时，允许走「模拟微信」流程（仍调 mock 确认） */
        private boolean devSimulate = true;
        /** 移动应用 AppID（开放平台） */
        private String appId = "";
        /** 商户号 */
        private String mchId = "";
        /** APIv3 密钥（32 字节） */
        private String apiV3Key = "";
        /** 商户 API 证书序列号 */
        private String serialNo = "";
        /**
         * 商户 API 私钥文件路径（apiclient_key.pem）。
         * 与 privateKey 二选一，优先 path。
         */
        private String privateKeyPath = "";
        /** 商户 API 私钥内容（PKCS8 PEM 或纯 Base64） */
        private String privateKey = "";
        private String notifyPath = "/api/pay/notify/wechat";

        public boolean hasPrivateKey() {
            return StringUtils.hasText(privateKeyPath) || StringUtils.hasText(privateKey);
        }
    }

    @Data
    public static class Alipay {
        private boolean enabled = false;
        private boolean devSimulate = true;
        private boolean sandbox = true;
        private String appId = "";
        private String privateKey = "";
        private String alipayPublicKey = "";
        private String gateway = "";
        private String notifyPath = "/api/pay/notify/alipay";
        private String returnUrl = "";

        public String resolveGateway() {
            if (StringUtils.hasText(gateway)) {
                return gateway.trim();
            }
            return sandbox
                    ? "https://openapi.alipaydev.com/gateway.do"
                    : "https://openapi.alipay.com/gateway.do";
        }
    }
}
