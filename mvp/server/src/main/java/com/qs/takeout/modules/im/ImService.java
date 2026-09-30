package com.qs.takeout.modules.im;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qs.takeout.common.auth.AuthContext;
import com.qs.takeout.common.auth.AuthUser;
import com.qs.takeout.common.auth.Roles;
import com.qs.takeout.common.exception.BizException;
import com.qs.takeout.modules.auth.entity.MerchantAccount;
import com.qs.takeout.modules.auth.entity.RiderAccount;
import com.qs.takeout.modules.auth.mapper.MerchantAccountMapper;
import com.qs.takeout.modules.auth.mapper.RiderAccountMapper;
import com.qs.takeout.modules.im.dto.ImOpenRequest;
import com.qs.takeout.modules.im.dto.ImReadRequest;
import com.qs.takeout.modules.im.dto.ImSendRequest;
import com.qs.takeout.modules.im.dto.ImSessionCardVO;
import com.qs.takeout.modules.im.dto.ImSessionVO;
import com.qs.takeout.modules.im.dto.ImUnreadVO;
import com.qs.takeout.modules.im.entity.ImMessage;
import com.qs.takeout.modules.im.entity.ImSession;
import com.qs.takeout.modules.im.mapper.ImMessageMapper;
import com.qs.takeout.modules.im.mapper.ImSessionMapper;
import com.qs.takeout.modules.im.ws.ImWsHub;
import com.qs.takeout.modules.order.OrderStatus;
import com.qs.takeout.modules.order.entity.OrderEntity;
import com.qs.takeout.modules.order.mapper.OrderMapper;
import com.qs.takeout.modules.shop.entity.Shop;
import com.qs.takeout.modules.shop.mapper.ShopMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ImService {

    private static final Set<String> MERCHANT_CHAT_STATUSES = Set.of(
            OrderStatus.PAID, OrderStatus.ACCEPTED, OrderStatus.DELIVERING,
            OrderStatus.COMPLETED, OrderStatus.REFUNDING
    );
    private static final Set<String> RIDER_CHAT_STATUSES = Set.of(
            OrderStatus.DELIVERING, OrderStatus.COMPLETED
    );
    private static final Set<String> CLOSED_ORDER_STATUSES = Set.of(
            OrderStatus.CANCELLED, OrderStatus.REFUNDED
    );

    private final ImSessionMapper sessionMapper;
    private final ImMessageMapper messageMapper;
    private final OrderMapper orderMapper;
    private final ShopMapper shopMapper;
    private final MerchantAccountMapper merchantAccountMapper;
    private final RiderAccountMapper riderAccountMapper;
    private final ImWsHub wsHub;
    private final ObjectMapper objectMapper;
    private final ImOfflinePush offlinePush;

    @Transactional
    public ImSessionVO open(ImOpenRequest req) {
        AuthUser user = AuthContext.require();
        String type = req.getType().trim().toUpperCase();
        if (!ImSessionTypes.USER_MERCHANT.equals(type) && !ImSessionTypes.USER_RIDER.equals(type)) {
            throw new BizException("不支持的会话类型");
        }
        OrderEntity order = requireOrder(req.getOrderId());
        assertCanOpen(user, order, type);

        ImSession session = sessionMapper.selectOne(new LambdaQueryWrapper<ImSession>()
                .eq(ImSession::getOrderId, order.getId())
                .eq(ImSession::getSessionType, type));
        if (session == null) {
            session = createSession(order, type);
        } else {
            syncSessionStatus(session, order);
            if (ImSessionTypes.USER_RIDER.equals(type) && order.getRiderId() != null) {
                session.setRiderId(order.getRiderId());
                sessionMapper.updateById(session);
            }
        }
        markReadInternal(session, user, null);
        session = sessionMapper.selectById(session.getId());
        return toVo(session, order, true);
    }

    public ImSessionVO detail(Long sessionId, Long afterId) {
        AuthUser user = AuthContext.require();
        ImSession session = requireParticipant(sessionId, user);
        OrderEntity order = requireOrder(session.getOrderId());
        syncSessionStatus(session, order);
        return toVo(session, order, true, afterId);
    }

    public List<ImSessionCardVO> listMine() {
        AuthUser user = AuthContext.require();
        List<ImSession> sessions = loadMySessions(user);
        List<ImSessionCardVO> cards = new java.util.ArrayList<>();
        for (ImSession session : sessions) {
            OrderEntity order = orderMapper.selectById(session.getOrderId());
            if (order == null) {
                continue;
            }
            syncSessionStatus(session, order);
            ImMessage last = messageMapper.selectOne(new LambdaQueryWrapper<ImMessage>()
                    .eq(ImMessage::getSessionId, session.getId())
                    .orderByDesc(ImMessage::getId)
                    .last("limit 1"));
            int unread = unreadOf(session, user.getRole());
            cards.add(ImSessionCardVO.builder()
                    .sessionId(session.getId())
                    .orderId(session.getOrderId())
                    .orderNo(order.getOrderNo())
                    .sessionType(session.getSessionType())
                    .status(session.getStatus())
                    .title(cardTitle(user, session, order))
                    .lastContent(last == null ? "" : previewContent(last))
                    .lastSenderRole(last == null ? "" : last.getSenderRole())
                    .updatedAt(session.getUpdatedAt() == null ? "" : session.getUpdatedAt().toString())
                    .canSend(ImSessionTypes.OPEN.equals(session.getStatus()) && canSend(order, session.getSessionType()))
                    .unreadCount(unread)
                    .lastFromPeer(last != null && !user.getRole().equals(last.getSenderRole())
                            && !"SYSTEM".equals(last.getSenderRole()))
                    .build());
        }
        return cards;
    }

    public ImUnreadVO unreadSummary() {
        AuthUser user = AuthContext.require();
        List<ImSession> sessions = loadMySessions(user);
        int messages = 0;
        int sessionsWithUnread = 0;
        for (ImSession session : sessions) {
            int n = unreadOf(session, user.getRole());
            if (n > 0) {
                sessionsWithUnread++;
                messages += n;
            }
        }
        return ImUnreadVO.builder()
                .unreadMessages(messages)
                .unreadSessions(sessionsWithUnread)
                .build();
    }

    @Transactional
    public ImSessionVO markRead(Long sessionId, ImReadRequest req) {
        AuthUser user = AuthContext.require();
        ImSession session = requireParticipant(sessionId, user);
        OrderEntity order = requireOrder(session.getOrderId());
        Long lastMsgId = req == null ? null : req.getLastMsgId();
        markReadInternal(session, user, lastMsgId);
        session = sessionMapper.selectById(sessionId);
        ImSessionVO vo = toVo(session, order, false);
        // 通知对方：我已读到此水位
        Map<String, Object> evt = new LinkedHashMap<>();
        evt.put("type", "im.peer_read");
        evt.put("sessionId", session.getId());
        evt.put("peerReadMsgId", readWatermark(session, user.getRole()));
        evt.put("readerRole", user.getRole());
        wsHub.pushSessionPeers(session, user.getRole(), user.getId(), evt);
        return vo;
    }

    private List<ImSession> loadMySessions(AuthUser user) {
        LambdaQueryWrapper<ImSession> q = new LambdaQueryWrapper<ImSession>()
                .orderByDesc(ImSession::getUpdatedAt)
                .last("limit 50");
        if (Roles.USER.equals(user.getRole())) {
            q.eq(ImSession::getUserId, user.getId());
        } else if (Roles.MERCHANT.equals(user.getRole())) {
            MerchantAccount m = merchantAccountMapper.selectById(user.getId());
            if (m == null || m.getShopId() == null) {
                return List.of();
            }
            q.and(w -> w.eq(ImSession::getMerchantId, user.getId())
                    .or()
                    .eq(ImSession::getShopId, m.getShopId()));
            q.eq(ImSession::getSessionType, ImSessionTypes.USER_MERCHANT);
        } else if (Roles.RIDER.equals(user.getRole())) {
            q.eq(ImSession::getRiderId, user.getId())
                    .eq(ImSession::getSessionType, ImSessionTypes.USER_RIDER);
        } else {
            return List.of();
        }
        return sessionMapper.selectList(q);
    }

    private int unreadOf(ImSession session, String role) {
        long after = readWatermark(session, role);
        return messageMapper.countUnread(session.getId(), after, role);
    }

    private long readWatermark(ImSession session, String role) {
        Long v = null;
        if (Roles.USER.equals(role)) {
            v = session.getUserReadMsgId();
        } else if (Roles.MERCHANT.equals(role)) {
            v = session.getMerchantReadMsgId();
        } else if (Roles.RIDER.equals(role)) {
            v = session.getRiderReadMsgId();
        }
        return v == null ? 0L : v;
    }

    private void markReadInternal(ImSession session, AuthUser user, Long lastMsgId) {
        Long target = lastMsgId;
        if (target == null || target <= 0) {
            ImMessage last = messageMapper.selectOne(new LambdaQueryWrapper<ImMessage>()
                    .eq(ImMessage::getSessionId, session.getId())
                    .orderByDesc(ImMessage::getId)
                    .last("limit 1"));
            target = last == null ? 0L : last.getId();
        }
        long cur = readWatermark(session, user.getRole());
        if (target <= cur) {
            return;
        }
        if (Roles.USER.equals(user.getRole())) {
            session.setUserReadMsgId(target);
        } else if (Roles.MERCHANT.equals(user.getRole())) {
            session.setMerchantReadMsgId(target);
        } else if (Roles.RIDER.equals(user.getRole())) {
            session.setRiderReadMsgId(target);
        }
        sessionMapper.updateById(session);
    }

    private String cardTitle(AuthUser user, ImSession session, OrderEntity order) {
        if (Roles.MERCHANT.equals(user.getRole()) || Roles.RIDER.equals(user.getRole())) {
            return "顾客 · " + order.getOrderNo();
        }
        if (ImSessionTypes.USER_RIDER.equals(session.getSessionType())) {
            RiderAccount rider = session.getRiderId() == null ? null : riderAccountMapper.selectById(session.getRiderId());
            return rider == null || !StringUtils.hasText(rider.getName()) ? "骑手" : ("骑手 · " + rider.getName());
        }
        Shop shop = shopMapper.selectById(session.getShopId());
        return shop == null ? "商家" : shop.getName();
    }

    public List<ImMessage> messages(Long sessionId, Long afterId) {
        AuthUser user = AuthContext.require();
        requireParticipant(sessionId, user);
        return listMessages(sessionId, afterId);
    }

    @Transactional
    public ImMessage send(Long sessionId, ImSendRequest req) {
        AuthUser user = AuthContext.require();
        ImSession session = requireParticipant(sessionId, user);
        OrderEntity order = requireOrder(session.getOrderId());
        syncSessionStatus(session, order);
        if (!ImSessionTypes.OPEN.equals(session.getStatus()) || !canSend(order, session.getSessionType())) {
            throw new BizException("会话已结束，无法发送");
        }
        String msgType = StringUtils.hasText(req.getMsgType())
                ? req.getMsgType().trim().toUpperCase()
                : ImMsgTypes.TEXT;
        if (!ImMsgTypes.TEXT.equals(msgType) && !ImMsgTypes.IMAGE.equals(msgType)) {
            throw new BizException("不支持的消息类型");
        }
        String content = req.getContent().trim();
        if (!StringUtils.hasText(content)) {
            throw new BizException("消息不能为空");
        }
        if (ImMsgTypes.IMAGE.equals(msgType)) {
            if (!content.startsWith("/uploads/") && !content.startsWith("http")) {
                throw new BizException("图片地址无效");
            }
            if (content.length() > 500) {
                throw new BizException("图片地址过长");
            }
        } else if (content.length() > 500) {
            throw new BizException("消息过长");
        }
        ImMessage msg = new ImMessage();
        msg.setSessionId(session.getId());
        msg.setSenderRole(user.getRole());
        msg.setSenderId(user.getId());
        msg.setMsgType(msgType);
        msg.setContent(content);
        messageMapper.insert(msg);
        session.setUpdatedAt(LocalDateTime.now());
        if (Roles.USER.equals(user.getRole())) {
            session.setUserReadMsgId(msg.getId());
        } else if (Roles.MERCHANT.equals(user.getRole())) {
            session.setMerchantReadMsgId(msg.getId());
        } else if (Roles.RIDER.equals(user.getRole())) {
            session.setRiderReadMsgId(msg.getId());
        }
        sessionMapper.updateById(session);

        Map<String, Object> evt = new LinkedHashMap<>();
        evt.put("type", "im.message");
        evt.put("sessionId", session.getId());
        evt.put("orderId", session.getOrderId());
        evt.put("sessionType", session.getSessionType());
        evt.put("message", msg);
        evt.put("preview", previewContent(msg));
        wsHub.pushSessionPeers(session, user.getRole(), user.getId(), evt);
        notifyOfflinePeers(session, user.getRole(), user.getId(), previewContent(msg));
        return msg;
    }

    private void notifyOfflinePeers(ImSession session, String excludeRole, Long excludeUserId, String preview) {
        if (session.getUserId() != null
                && !(Roles.USER.equals(excludeRole) && session.getUserId().equals(excludeUserId))) {
            offlinePush.notifyNewMessage(Roles.USER, session.getUserId(), session.getId(), preview);
        }
        if (session.getMerchantId() != null
                && !(Roles.MERCHANT.equals(excludeRole) && session.getMerchantId().equals(excludeUserId))) {
            offlinePush.notifyNewMessage(Roles.MERCHANT, session.getMerchantId(), session.getId(), preview);
        }
        if (session.getRiderId() != null
                && !(Roles.RIDER.equals(excludeRole) && session.getRiderId().equals(excludeUserId))) {
            offlinePush.notifyNewMessage(Roles.RIDER, session.getRiderId(), session.getId(), preview);
        }
    }

    private ImSession createSession(OrderEntity order, String type) {
        MerchantAccount merchant = merchantAccountMapper.selectOne(new LambdaQueryWrapper<MerchantAccount>()
                .eq(MerchantAccount::getShopId, order.getShopId())
                .last("limit 1"));
        ImSession session = new ImSession();
        session.setOrderId(order.getId());
        session.setSessionType(type);
        session.setUserId(order.getUserId());
        session.setShopId(order.getShopId());
        session.setMerchantId(merchant == null ? null : merchant.getId());
        if (ImSessionTypes.USER_RIDER.equals(type)) {
            session.setRiderId(order.getRiderId());
        }
        session.setStatus(canSend(order, type) ? ImSessionTypes.OPEN : ImSessionTypes.CLOSED);
        if (ImSessionTypes.CLOSED.equals(session.getStatus())) {
            session.setClosedAt(LocalDateTime.now());
        }
        sessionMapper.insert(session);

        ImMessage tip = new ImMessage();
        tip.setSessionId(session.getId());
        tip.setSenderRole("SYSTEM");
        tip.setSenderId(0L);
        tip.setMsgType(ImMsgTypes.TEXT);
        tip.setContent(ImSessionTypes.USER_MERCHANT.equals(type)
                ? "订单沟通已开启（买家与商家），订单结束后将关闭发送"
                : "订单沟通已开启（买家与骑手），订单结束后将关闭发送");
        messageMapper.insert(tip);
        AuthUser opener = AuthContext.require();
        if (Roles.USER.equals(opener.getRole())) {
            session.setUserReadMsgId(tip.getId());
        } else if (Roles.MERCHANT.equals(opener.getRole())) {
            session.setMerchantReadMsgId(tip.getId());
        } else if (Roles.RIDER.equals(opener.getRole())) {
            session.setRiderReadMsgId(tip.getId());
        }
        sessionMapper.updateById(session);
        return session;
    }

    private void assertCanOpen(AuthUser user, OrderEntity order, String type) {
        if (Roles.USER.equals(user.getRole())) {
            if (!user.getId().equals(order.getUserId())) {
                throw new BizException("订单不存在");
            }
        } else if (Roles.MERCHANT.equals(user.getRole())) {
            MerchantAccount m = merchantAccountMapper.selectById(user.getId());
            if (m == null || m.getShopId() == null || !m.getShopId().equals(order.getShopId())) {
                throw new BizException("无权查看该订单会话");
            }
            if (!ImSessionTypes.USER_MERCHANT.equals(type)) {
                throw new BizException("商家仅可打开用户会话");
            }
        } else if (Roles.RIDER.equals(user.getRole())) {
            if (order.getRiderId() == null || !user.getId().equals(order.getRiderId())) {
                throw new BizException("无权查看该订单会话");
            }
            if (!ImSessionTypes.USER_RIDER.equals(type)) {
                throw new BizException("骑手仅可打开用户会话");
            }
        } else {
            throw new BizException(403, "无权限");
        }

        if (ImSessionTypes.USER_MERCHANT.equals(type)) {
            if (!MERCHANT_CHAT_STATUSES.contains(order.getStatus()) && !CLOSED_ORDER_STATUSES.contains(order.getStatus())) {
                throw new BizException("当前订单状态暂不可联系商家");
            }
            if (OrderStatus.PENDING_PAY.equals(order.getStatus())) {
                throw new BizException("请先完成支付后再联系商家");
            }
        } else {
            if (order.getRiderId() == null) {
                throw new BizException("暂无骑手接单，请稍后再联系");
            }
            if (!RIDER_CHAT_STATUSES.contains(order.getStatus()) && !CLOSED_ORDER_STATUSES.contains(order.getStatus())) {
                throw new BizException("配送开始后可联系骑手");
            }
        }
    }

    private void syncSessionStatus(ImSession session, OrderEntity order) {
        boolean shouldOpen = canSend(order, session.getSessionType());
        if (shouldOpen && ImSessionTypes.CLOSED.equals(session.getStatus())) {
            // 完成后不允许重新打开发送
            return;
        }
        if (!shouldOpen && ImSessionTypes.OPEN.equals(session.getStatus())) {
            session.setStatus(ImSessionTypes.CLOSED);
            session.setClosedAt(LocalDateTime.now());
            sessionMapper.updateById(session);
        }
    }

    private boolean canSend(OrderEntity order, String type) {
        if (CLOSED_ORDER_STATUSES.contains(order.getStatus())) {
            return false;
        }
        if (OrderStatus.COMPLETED.equals(order.getStatus())) {
            return false;
        }
        if (ImSessionTypes.USER_MERCHANT.equals(type)) {
            return MERCHANT_CHAT_STATUSES.contains(order.getStatus())
                    && !OrderStatus.COMPLETED.equals(order.getStatus());
        }
        return OrderStatus.DELIVERING.equals(order.getStatus());
    }

    private ImSession requireParticipant(Long sessionId, AuthUser user) {
        ImSession session = sessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BizException("会话不存在");
        }
        boolean ok = false;
        if (Roles.USER.equals(user.getRole())) {
            ok = user.getId().equals(session.getUserId());
        } else if (Roles.MERCHANT.equals(user.getRole())) {
            ok = session.getMerchantId() != null && user.getId().equals(session.getMerchantId());
            if (!ok) {
                MerchantAccount m = merchantAccountMapper.selectById(user.getId());
                ok = m != null && m.getShopId() != null && m.getShopId().equals(session.getShopId());
            }
        } else if (Roles.RIDER.equals(user.getRole())) {
            ok = session.getRiderId() != null && user.getId().equals(session.getRiderId());
        }
        if (!ok) {
            throw new BizException(403, "无权访问该会话");
        }
        return session;
    }

    private OrderEntity requireOrder(Long orderId) {
        OrderEntity order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException("订单不存在");
        }
        return order;
    }

    private List<ImMessage> listMessages(Long sessionId, Long afterId) {
        LambdaQueryWrapper<ImMessage> q = new LambdaQueryWrapper<ImMessage>()
                .eq(ImMessage::getSessionId, sessionId)
                .orderByAsc(ImMessage::getId);
        if (afterId != null && afterId > 0) {
            q.gt(ImMessage::getId, afterId);
        } else {
            q.last("limit 200");
        }
        return messageMapper.selectList(q);
    }

    private ImSessionVO toVo(ImSession session, OrderEntity order, boolean withMessages) {
        return toVo(session, order, withMessages, null);
    }

    private ImSessionVO toVo(ImSession session, OrderEntity order, boolean withMessages, Long afterId) {
        AuthUser me = AuthContext.get();
        String title;
        if (me != null && (Roles.MERCHANT.equals(me.getRole()) || Roles.RIDER.equals(me.getRole()))) {
            title = "顾客";
        } else if (ImSessionTypes.USER_MERCHANT.equals(session.getSessionType())) {
            Shop shop = shopMapper.selectById(session.getShopId());
            title = shop == null ? "联系商家" : shop.getName();
        } else {
            RiderAccount rider = session.getRiderId() == null ? null : riderAccountMapper.selectById(session.getRiderId());
            title = rider == null || !StringUtils.hasText(rider.getName()) ? "联系骑手" : ("骑手 · " + rider.getName());
        }
        String peerPhone = "";
        String peerLabel = "";
        if (me != null) {
            String[] peer = resolvePeerContact(me.getRole(), session, order);
            peerPhone = peer[0];
            peerLabel = peer[1];
        }
        return ImSessionVO.builder()
                .session(session)
                .title(title)
                .orderNo(order.getOrderNo())
                .orderStatus(order.getStatus())
                .canSend(ImSessionTypes.OPEN.equals(session.getStatus()) && canSend(order, session.getSessionType()))
                .peerReadMsgId(peerReadMsgId(session, me == null ? null : me.getRole()))
                .peerPhone(peerPhone)
                .peerLabel(peerLabel)
                .messages(withMessages ? listMessages(session.getId(), afterId) : List.of())
                .build();
    }

    private String previewContent(ImMessage msg) {
        if (msg == null) {
            return "";
        }
        if (ImMsgTypes.IMAGE.equals(msg.getMsgType())) {
            return "[图片]";
        }
        return msg.getContent() == null ? "" : msg.getContent();
    }

    /** @return [phone, label] */
    private String[] resolvePeerContact(String myRole, ImSession session, OrderEntity order) {
        if (Roles.USER.equals(myRole)) {
            if (ImSessionTypes.USER_RIDER.equals(session.getSessionType())) {
                RiderAccount rider = session.getRiderId() == null ? null : riderAccountMapper.selectById(session.getRiderId());
                String phone = rider == null || !StringUtils.hasText(rider.getPhone()) ? "" : rider.getPhone();
                return new String[]{phone, "骑手"};
            }
            Shop shop = shopMapper.selectById(session.getShopId());
            String phone = shop == null || !StringUtils.hasText(shop.getPhone()) ? "" : shop.getPhone();
            return new String[]{phone, "商家"};
        }
        String phone = extractContactPhone(order.getAddressSnapshot());
        return new String[]{phone == null ? "" : phone, "顾客"};
    }

    private String extractContactPhone(String snapshot) {
        if (!StringUtils.hasText(snapshot)) {
            return "";
        }
        try {
            JsonNode node = objectMapper.readTree(snapshot);
            JsonNode phone = node.get("contactPhone");
            if (phone != null && phone.isTextual()) {
                return phone.asText();
            }
        } catch (Exception ignored) {
            // ignore
        }
        return "";
    }

    /** 当前用户视角下，对方已读水位 */
    private Long peerReadMsgId(ImSession session, String myRole) {
        if (myRole == null) {
            return 0L;
        }
        if (Roles.USER.equals(myRole)) {
            if (ImSessionTypes.USER_RIDER.equals(session.getSessionType())) {
                return session.getRiderReadMsgId() == null ? 0L : session.getRiderReadMsgId();
            }
            return session.getMerchantReadMsgId() == null ? 0L : session.getMerchantReadMsgId();
        }
        if (Roles.MERCHANT.equals(myRole) || Roles.RIDER.equals(myRole)) {
            return session.getUserReadMsgId() == null ? 0L : session.getUserReadMsgId();
        }
        return 0L;
    }
}
