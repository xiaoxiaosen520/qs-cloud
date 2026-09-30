package com.qs.takeout.modules.pay.gateway;

import com.qs.takeout.common.exception.BizException;
import com.qs.takeout.modules.pay.config.PayProperties;
import com.wechat.pay.java.core.Config;
import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.core.notification.NotificationConfig;
import com.wechat.pay.java.core.notification.NotificationParser;
import com.wechat.pay.java.core.util.PemUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.security.PrivateKey;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 微信支付 API v3 配置（自动下载平台证书）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WechatPayClientFactory {

    private final PayProperties payProperties;
    private final AtomicReference<Config> configRef = new AtomicReference<>();

    public synchronized Config config() {
        Config cached = configRef.get();
        if (cached != null) {
            return cached;
        }
        PayProperties.Wechat w = payProperties.getWechat();
        if (!StringUtils.hasText(w.getMchId())
                || !StringUtils.hasText(w.getApiV3Key())
                || !StringUtils.hasText(w.getSerialNo())
                || !w.hasPrivateKey()) {
            throw new BizException("微信支付商户参数不完整，请配置 mch-id / api-v3-key / serial-no / 私钥");
        }
        RSAAutoCertificateConfig.Builder builder = new RSAAutoCertificateConfig.Builder()
                .merchantId(w.getMchId().trim())
                .merchantSerialNumber(w.getSerialNo().trim())
                .apiV3Key(w.getApiV3Key().trim());
        if (StringUtils.hasText(w.getPrivateKeyPath())) {
            builder.privateKeyFromPath(w.getPrivateKeyPath().trim());
        } else {
            builder.privateKey(loadPrivateKey(w.getPrivateKey()));
        }
        Config config = builder.build();
        configRef.set(config);
        log.info("wechat pay config ready mchId={}", w.getMchId());
        return config;
    }

    public NotificationParser notificationParser() {
        Config config = config();
        if (!(config instanceof NotificationConfig notificationConfig)) {
            throw new BizException("微信支付回调配置异常");
        }
        return new NotificationParser(notificationConfig);
    }

    /** 改密钥后可清缓存（一般重启即可） */
    public void reset() {
        configRef.set(null);
    }

    private static PrivateKey loadPrivateKey(String raw) {
        String key = raw == null ? "" : raw.trim();
        if (!key.contains("BEGIN")) {
            key = "-----BEGIN PRIVATE KEY-----\n" + key.replaceAll("\\s+", "\n") + "\n-----END PRIVATE KEY-----";
        }
        return PemUtil.loadPrivateKeyFromString(key);
    }
}
