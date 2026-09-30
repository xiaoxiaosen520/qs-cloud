package com.qs.takeout.modules.delivery.fengniao;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qs.takeout.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 蜂鸟即时配送开放平台（Anubis）。
 * 正式签名/入参以 https://open.ele.me/documents 为准；配置 app-id/secret 后启用。
 */
@Slf4j
@RequiredArgsConstructor
public class AnubisFengNiaoClient implements FengNiaoClient {

    private final FengNiaoProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();

    private volatile String cachedToken;
    private volatile long tokenExpireAt;

    @Override
    public CreateResult createOrder(CreateCommand cmd) {
        requireKeys();
        try {
            String token = accessToken();
            Map<String, Object> body = Map.of(
                    "partner_order_code", cmd.partnerOrderCode(),
                    "notify_url", cmd.notifyUrl(),
                    "chain_store_code", cmd.storeCode(),
                    "transport_info", Map.of(
                            "transport_name", nz(cmd.shopName()),
                            "transport_address", nz(cmd.shopAddress()),
                            "transport_latitude", cmd.shopLat(),
                            "transport_longitude", cmd.shopLng(),
                            "position_source", properties.getPositionSource(),
                            "transport_tel", nz(cmd.shopPhone())
                    ),
                    "receiver_info", Map.of(
                            "receiver_name", nz(cmd.receiverName()),
                            "receiver_primary_phone", nz(cmd.receiverPhone()),
                            "receiver_address", nz(cmd.receiverAddress()),
                            "receiver_latitude", cmd.receiverLat(),
                            "receiver_longitude", cmd.receiverLng(),
                            "position_source", properties.getPositionSource()
                    ),
                    "order_total_amount", cmd.goodsAmount(),
                    "order_actual_amount", cmd.goodsAmount(),
                    "order_remark", nz(cmd.remark())
            );
            JsonNode data = invoke("/v3/invoke?action=order.create", token, body);
            String tracking = text(data, "order_id", "tracking_no", "waybill_no");
            BigDecimal fee = decimal(data, "actual_delivery_amount_cent", "total_delivery_amount_cent");
            if (fee != null && fee.compareTo(BigDecimal.valueOf(100)) >= 0
                    && data.has("actual_delivery_amount_cent")) {
                fee = fee.movePointLeft(2);
            }
            return new CreateResult(
                    StringUtils.hasText(tracking) ? tracking : cmd.partnerOrderCode(),
                    fee == null ? cmd.deliveryFee() : fee,
                    text(data, "carrier_driver_name"),
                    text(data, "carrier_driver_phone")
            );
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.warn("fengniao create failed: {}", e.getMessage());
            throw new BizException("蜂鸟发单失败：" + e.getMessage());
        }
    }

    @Override
    public void cancelOrder(String partnerOrderCode, String reason) {
        requireKeys();
        try {
            String token = accessToken();
            invoke("/v3/invoke?action=order.cancel", token, Map.of(
                    "partner_order_code", partnerOrderCode,
                    "order_cancel_reason_code", 2,
                    "order_cancel_code", 0,
                    "order_cancel_description", reason == null ? "商家/平台取消" : reason
            ));
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.warn("fengniao cancel failed code={}: {}", partnerOrderCode, e.getMessage());
            throw new BizException("取消蜂鸟运单失败：" + e.getMessage());
        }
    }

    private void requireKeys() {
        if (!StringUtils.hasText(properties.getAppId()) || !StringUtils.hasText(properties.getSecret())) {
            throw new BizException("未配置蜂鸟 app-id/secret，请先用 mock 或填写开放平台密钥");
        }
    }

    private String accessToken() throws Exception {
        long now = System.currentTimeMillis();
        if (StringUtils.hasText(cachedToken) && now < tokenExpireAt - 60_000) {
            return cachedToken;
        }
        int salt = ThreadLocalRandom.current().nextInt(1000, 9999);
        String signSource = properties.getAppId() + salt + properties.getSecret();
        String signature = md5(urlEncode(Base64.getEncoder().encodeToString(signSource.getBytes(StandardCharsets.UTF_8))));
        String url = base() + "/get_access_token?app_id=" + urlEncode(properties.getAppId())
                + "&salt=" + salt + "&signature=" + signature;
        HttpRequest req = HttpRequest.newBuilder(URI.create(url)).GET().timeout(Duration.ofSeconds(10)).build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        JsonNode root = objectMapper.readTree(resp.body());
        if (root.path("code").asInt() != 200) {
            throw new BizException("蜂鸟 token 失败：" + root.path("msg").asText());
        }
        cachedToken = root.path("data").path("access_token").asText();
        tokenExpireAt = root.path("data").path("expire_time").asLong(now + 86_400_000L);
        if (tokenExpireAt < 10_000_000_000L) {
            tokenExpireAt = now + tokenExpireAt;
        }
        return cachedToken;
    }

    private JsonNode invoke(String path, String token, Map<String, Object> business) throws Exception {
        String dataJson = objectMapper.writeValueAsString(business);
        int salt = ThreadLocalRandom.current().nextInt(1000, 9999);
        String raw = properties.getAppId() + dataJson + salt;
        String signature = md5(urlEncode(Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8)))
                + properties.getSecret());
        Map<String, Object> payload = Map.of(
                "app_id", properties.getAppId(),
                "access_token", token,
                "data", dataJson,
                "salt", salt,
                "signature", signature
        );
        String body = objectMapper.writeValueAsString(payload);
        HttpRequest req = HttpRequest.newBuilder(URI.create(base() + path))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(12))
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        JsonNode root = objectMapper.readTree(resp.body());
        if (root.path("code").asInt(root.path("errno").asInt(-1)) != 200
                && root.path("code").asInt() != 0) {
            throw new BizException(root.path("msg").asText(root.path("message").asText("蜂鸟接口错误")));
        }
        return root.path("data").isMissingNode() ? root : root.path("data");
    }

    private String base() {
        return properties.isSandbox()
                ? "https://exam-anubis.ele.me/anubis-webapi"
                : "https://open-anubis.ele.me/anubis-webapi";
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    private static String text(JsonNode n, String... keys) {
        if (n == null) {
            return "";
        }
        for (String k : keys) {
            String v = n.path(k).asText("");
            if (StringUtils.hasText(v)) {
                return v;
            }
        }
        return "";
    }

    private static BigDecimal decimal(JsonNode n, String... keys) {
        if (n == null) {
            return null;
        }
        for (String k : keys) {
            if (n.has(k) && n.get(k).isNumber()) {
                return n.get(k).decimalValue();
            }
        }
        return null;
    }

    private static String urlEncode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private static String md5(String s) throws Exception {
        byte[] d = MessageDigest.getInstance("MD5").digest(s.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(d);
    }
}
