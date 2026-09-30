package com.qs.takeout.common.sms;

import com.qs.takeout.common.exception.BizException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * MVP 验证码：进程内缓存。上线可换成 Redis / 短信网关。
 */
@Slf4j
@Service
public class SmsCodeService {

    private static final long TTL_SECONDS = 300;

    private final Map<String, CodeEntry> store = new ConcurrentHashMap<>();

    @Value("${qs.sms.dev-fixed-code:true}")
    private boolean devFixedCode;

    public void send(String phone, String scene) {
        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(0, 1_000_000));
        store.put(key(phone, scene), new CodeEntry(code, Instant.now().getEpochSecond() + TTL_SECONDS));
        log.info("[SMS] phone={} scene={} code={}", phone, scene, code);
    }

    public void verifyOrThrow(String phone, String scene, String code) {
        if (devFixedCode && "123456".equals(code)) {
            return;
        }
        CodeEntry entry = store.get(key(phone, scene));
        if (entry == null || entry.expireAt < Instant.now().getEpochSecond() || !entry.code.equals(code)) {
            throw new BizException("验证码错误或已过期");
        }
        store.remove(key(phone, scene));
    }

    private String key(String phone, String scene) {
        return scene + ":" + phone;
    }

    private record CodeEntry(String code, long expireAt) {
    }
}
