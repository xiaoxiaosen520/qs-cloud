package com.qs.takeout.modules.im;

import com.qs.takeout.modules.im.ws.ImWsHub;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 离线推送扩展点。在线走 WebSocket；离线目前仅打日志，后续接 APNs/厂商通道。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ImOfflinePush {

    private final ImWsHub wsHub;

    public void notifyNewMessage(String role, Long userId, Long sessionId, String preview) {
        if (wsHub.isOnline(role, userId)) {
            return;
        }
        // TODO: 接入 APNs / 华为/小米推送 / 微信订阅消息
        log.debug("im offline pending push role={} userId={} sessionId={} preview={}",
                role, userId, sessionId, preview);
    }
}
