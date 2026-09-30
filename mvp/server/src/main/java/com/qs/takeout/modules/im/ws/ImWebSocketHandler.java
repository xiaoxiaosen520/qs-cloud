package com.qs.takeout.modules.im.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qs.takeout.common.auth.AuthUser;
import com.qs.takeout.common.jwt.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ImWebSocketHandler extends TextWebSocketHandler {

    public static final String ATTR_USER = "imUser";

    private final JwtService jwtService;
    private final ObjectMapper objectMapper;
    private final ImWsHub hub;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String token = queryParam(session.getUri(), "token");
        if (!StringUtils.hasText(token)) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("missing token"));
            return;
        }
        AuthUser user;
        try {
            user = jwtService.parse(token.trim());
        } catch (Exception e) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("invalid token"));
            return;
        }
        session.getAttributes().put(ATTR_USER, user);
        hub.register(user, session);
        sendJson(session, Map.of("type", "im.connected", "role", user.getRole(), "userId", user.getId()));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        if (!StringUtils.hasText(payload)) {
            return;
        }
        // 心跳：{"type":"ping"}
        if (payload.contains("\"ping\"")) {
            sendJson(session, Map.of("type", "pong"));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        AuthUser user = (AuthUser) session.getAttributes().get(ATTR_USER);
        if (user != null) {
            hub.unregister(user, session);
        }
    }

    private void sendJson(WebSocketSession session, Object body) throws IOException {
        if (session.isOpen()) {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(body)));
        }
    }

    private static String queryParam(URI uri, String key) {
        if (uri == null || uri.getQuery() == null) {
            return null;
        }
        for (String part : uri.getQuery().split("&")) {
            int i = part.indexOf('=');
            if (i > 0 && key.equals(part.substring(0, i))) {
                return part.substring(i + 1);
            }
        }
        return null;
    }
}
