package com.qs.takeout.modules.im.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qs.takeout.common.auth.AuthUser;
import com.qs.takeout.common.auth.Roles;
import com.qs.takeout.modules.im.entity.ImSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class ImWsHub {

    private final ObjectMapper objectMapper;

    /** role:userId -> sessions */
    private final Map<String, Set<WebSocketSession>> online = new ConcurrentHashMap<>();

    public void register(AuthUser user, WebSocketSession session) {
        online.computeIfAbsent(key(user.getRole(), user.getId()), k -> ConcurrentHashMap.newKeySet()).add(session);
    }

    public void unregister(AuthUser user, WebSocketSession session) {
        Set<WebSocketSession> set = online.get(key(user.getRole(), user.getId()));
        if (set != null) {
            set.remove(session);
            if (set.isEmpty()) {
                online.remove(key(user.getRole(), user.getId()));
            }
        }
    }

    public boolean isOnline(String role, Long userId) {
        Set<WebSocketSession> set = online.get(key(role, userId));
        return set != null && !set.isEmpty();
    }

    public void pushToUser(String role, Long userId, Object payload) {
        Set<WebSocketSession> set = online.get(key(role, userId));
        if (set == null || set.isEmpty()) {
            return;
        }
        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            log.warn("im ws serialize fail", e);
            return;
        }
        TextMessage msg = new TextMessage(json);
        for (WebSocketSession s : set) {
            try {
                if (s.isOpen()) {
                    synchronized (s) {
                        s.sendMessage(msg);
                    }
                }
            } catch (Exception e) {
                log.debug("im ws send fail: {}", e.getMessage());
            }
        }
    }

    /** 推给会话双方（可排除发送者） */
    public void pushSessionPeers(ImSession session, String excludeRole, Long excludeUserId, Object payload) {
        if (session.getUserId() != null
                && !(Roles.USER.equals(excludeRole) && session.getUserId().equals(excludeUserId))) {
            pushToUser(Roles.USER, session.getUserId(), payload);
        }
        if (session.getMerchantId() != null
                && !(Roles.MERCHANT.equals(excludeRole) && session.getMerchantId().equals(excludeUserId))) {
            pushToUser(Roles.MERCHANT, session.getMerchantId(), payload);
        }
        if (session.getRiderId() != null
                && !(Roles.RIDER.equals(excludeRole) && session.getRiderId().equals(excludeUserId))) {
            pushToUser(Roles.RIDER, session.getRiderId(), payload);
        }
    }

    private static String key(String role, Long id) {
        return role + ":" + id;
    }
}
