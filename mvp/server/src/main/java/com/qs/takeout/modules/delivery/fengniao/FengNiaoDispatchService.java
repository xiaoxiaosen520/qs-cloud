package com.qs.takeout.modules.delivery.fengniao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qs.takeout.common.exception.BizException;
import com.qs.takeout.modules.delivery.fengniao.entity.DeliveryDispatch;
import com.qs.takeout.modules.delivery.fengniao.mapper.DeliveryDispatchMapper;
import com.qs.takeout.modules.finance.MerchantSettleService;
import com.qs.takeout.modules.order.OrderDeadlineSupport;
import com.qs.takeout.modules.order.OrderStatus;
import com.qs.takeout.modules.order.entity.OrderEntity;
import com.qs.takeout.modules.order.entity.OrderStatusLog;
import com.qs.takeout.modules.order.mapper.OrderMapper;
import com.qs.takeout.modules.order.mapper.OrderStatusLogMapper;
import com.qs.takeout.modules.shop.entity.Shop;
import com.qs.takeout.modules.shop.mapper.ShopMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class FengNiaoDispatchService {

    private final DeliveryDispatchMapper dispatchMapper;
    private final OrderMapper orderMapper;
    private final OrderStatusLogMapper orderStatusLogMapper;
    private final ShopMapper shopMapper;
    private final FengNiaoClient fengNiaoClient;
    private final FengNiaoProperties properties;
    private final OrderDeadlineSupport orderDeadlineSupport;
    private final MerchantSettleService merchantSettleService;
    private final ObjectMapper objectMapper;

    public boolean isPlatform(OrderEntity order) {
        return order != null && !"SELF".equalsIgnoreCase(order.getDeliveryType());
    }

    public DeliveryDispatch ofOrder(Long orderId) {
        return dispatchMapper.selectOne(new LambdaQueryWrapper<DeliveryDispatch>()
                .eq(DeliveryDispatch::getOrderId, orderId)
                .last("limit 1"));
    }

    public Map<String, Object> toVo(Long orderId) {
        DeliveryDispatch d = ofOrder(orderId);
        if (d == null) {
            return null;
        }
        Map<String, Object> m = new HashMap<>();
        m.put("channel", d.getChannel());
        m.put("status", d.getStatus());
        m.put("statusText", FengNiaoStatuses.labelOf(d.getStatus()));
        m.put("trackingNo", d.getTrackingNo());
        m.put("riderName", d.getRiderName());
        m.put("riderPhone", d.getRiderPhone());
        m.put("fee", d.getFee());
        m.put("failReason", d.getFailReason());
        m.put("mock", d.getMock() != null && d.getMock() == 1);
        m.put("anubisStatus", d.getAnubisStatus());
        return m;
    }

    /** 商家接单后发蜂鸟；失败不阻断接单，可重试。 */
    public void dispatch(Long orderId) {
        OrderEntity order = orderMapper.selectById(orderId);
        if (order == null || !isPlatform(order)) {
            return;
        }
        if (!OrderStatus.ACCEPTED.equals(order.getStatus()) && !OrderStatus.DELIVERING.equals(order.getStatus())) {
            throw new BizException("当前状态不可呼叫蜂鸟");
        }
        DeliveryDispatch exist = ofOrder(orderId);
        if (exist != null && !FengNiaoStatuses.FAILED.equals(exist.getStatus())
                && !FengNiaoStatuses.CANCELLED.equals(exist.getStatus())) {
            return;
        }

        Shop shop = shopMapper.selectById(order.getShopId());
        Map<String, Object> addr = parseAddress(order.getAddressSnapshot());
        String storeCode = shop != null && StringUtils.hasText(shop.getFengniaoStoreCode())
                ? shop.getFengniaoStoreCode()
                : "SHOP_" + order.getShopId();

        DeliveryDispatch row = exist == null ? new DeliveryDispatch() : exist;
        row.setOrderId(order.getId());
        row.setOrderNo(order.getOrderNo());
        row.setChannel("FENG_NIAO");
        row.setPartnerOrderCode(order.getOrderNo());
        row.setStoreCode(storeCode);
        row.setMock(properties.isMock() ? 1 : 0);
        row.setFailReason("");
        row.setRiderName("");
        row.setRiderPhone("");

        try {
            FengNiaoClient.CreateResult r = fengNiaoClient.createOrder(new FengNiaoClient.CreateCommand(
                    order.getOrderNo(),
                    properties.getNotifyUrl(),
                    storeCode,
                    shop == null ? "店铺" : shop.getName(),
                    shop == null ? "" : nz(shop.getAddress()),
                    shop == null ? BigDecimal.ZERO : nzDec(shop.getLat()),
                    shop == null ? BigDecimal.ZERO : nzDec(shop.getLng()),
                    shop == null ? "" : nz(shop.getPhone()),
                    str(addr, "contactName"),
                    str(addr, "contactPhone"),
                    str(addr, "detail"),
                    dec(addr, "lat"),
                    dec(addr, "lng"),
                    order.getGoodsAmount(),
                    order.getDeliveryFee(),
                    order.getRemark()
            ));
            row.setTrackingNo(r.trackingNo() == null ? "" : r.trackingNo());
            row.setFee(r.fee() == null ? nzDec(order.getDeliveryFee()) : r.fee());
            row.setRiderName(r.riderName() == null ? "" : r.riderName());
            row.setRiderPhone(r.riderPhone() == null ? "" : r.riderPhone());
            row.setStatus(FengNiaoStatuses.CREATED);
            row.setAnubisStatus(FengNiaoStatuses.ANUBIS_WAYBILL);
            if (properties.isMock()) {
                row.setNextMockAt(LocalDateTime.now().plus(Duration.ofMillis(properties.getMockStepMs())));
            } else {
                row.setNextMockAt(null);
            }
            if (row.getId() == null) {
                dispatchMapper.insert(row);
            } else {
                dispatchMapper.updateById(row);
            }
            logStatus(order.getId(), order.getStatus(), order.getStatus(), "SYSTEM", null, "已呼叫蜂鸟众包配送");
            if (order.getAutoCompleteAt() == null) {
                order.setAutoCompleteAt(orderDeadlineSupport.autoCompleteFromNow());
                orderMapper.updateById(order);
            }
            log.info("fengniao dispatched order={} tracking={} mock={}", order.getOrderNo(), row.getTrackingNo(),
                    properties.isMock());
        } catch (Exception e) {
            row.setStatus(FengNiaoStatuses.FAILED);
            row.setFailReason(e.getMessage() == null ? "发单失败" : e.getMessage());
            row.setAnubisStatus(0);
            row.setNextMockAt(null);
            if (row.getId() == null) {
                dispatchMapper.insert(row);
            } else {
                dispatchMapper.updateById(row);
            }
            log.warn("fengniao dispatch failed order={}: {}", order.getOrderNo(), e.getMessage());
        }
    }

    public void cancelIfActive(Long orderId, String reason) {
        DeliveryDispatch d = ofOrder(orderId);
        if (d == null) {
            return;
        }
        if (FengNiaoStatuses.COMPLETED.equals(d.getStatus())
                || FengNiaoStatuses.CANCELLED.equals(d.getStatus())
                || FengNiaoStatuses.FAILED.equals(d.getStatus())) {
            return;
        }
        try {
            fengNiaoClient.cancelOrder(d.getPartnerOrderCode(), reason);
        } catch (Exception e) {
            log.warn("fengniao cancel call failed orderId={}: {}", orderId, e.getMessage());
        }
        d.setStatus(FengNiaoStatuses.CANCELLED);
        d.setAnubisStatus(FengNiaoStatuses.ANUBIS_CANCELLED);
        d.setNextMockAt(null);
        d.setFailReason(reason == null ? "" : reason);
        dispatchMapper.updateById(d);
    }

    @Transactional
    public void handleNotify(Map<String, Object> body) {
        if (body == null || body.isEmpty()) {
            return;
        }
        String partner = str(body, "partner_order_code", "partnerOrderCode", "order_no");
        if (!StringUtils.hasText(partner)) {
            throw new BizException("缺少 partner_order_code");
        }
        DeliveryDispatch d = dispatchMapper.selectOne(new LambdaQueryWrapper<DeliveryDispatch>()
                .eq(DeliveryDispatch::getPartnerOrderCode, partner)
                .last("limit 1"));
        if (d == null) {
            log.warn("fengniao notify unknown order {}", partner);
            return;
        }
        int anubis = intVal(body, "order_status", "orderStatus", "status");
        applyStatus(d, anubis, str(body, "carrier_driver_name", "riderName"),
                str(body, "carrier_driver_phone", "riderPhone"), writeJson(body));
    }

    @Transactional
    public void applyStatus(DeliveryDispatch d, int anubis, String riderName, String riderPhone, String rawJson) {
        if (d == null) {
            return;
        }
        int prev = d.getAnubisStatus() == null ? -1 : d.getAnubisStatus();
        boolean cancel = anubis == FengNiaoStatuses.ANUBIS_CANCELLED || anubis == FengNiaoStatuses.ANUBIS_EXCEPTION;
        if (!cancel && anubis <= prev && d.getStatus() != null && !FengNiaoStatuses.CREATED.equals(d.getStatus())) {
            return;
        }
        String local = FengNiaoStatuses.localOf(anubis);
        d.setAnubisStatus(anubis);
        d.setStatus(local);
        if (StringUtils.hasText(riderName)) {
            d.setRiderName(riderName);
        }
        if (StringUtils.hasText(riderPhone)) {
            d.setRiderPhone(riderPhone);
        }
        if (rawJson != null) {
            d.setLastNotifyJson(rawJson);
        }
        if (properties.isMock() && d.getMock() != null && d.getMock() == 1
                && !FengNiaoStatuses.COMPLETED.equals(local)
                && !FengNiaoStatuses.CANCELLED.equals(local)) {
            d.setNextMockAt(LocalDateTime.now().plus(Duration.ofMillis(properties.getMockStepMs())));
        } else {
            d.setNextMockAt(null);
        }
        dispatchMapper.updateById(d);
        syncOrder(d, anubis);
    }

    private void syncOrder(DeliveryDispatch d, int anubis) {
        OrderEntity order = orderMapper.selectById(d.getOrderId());
        if (order == null) {
            return;
        }
        String from = order.getStatus();
        if (anubis == FengNiaoStatuses.ANUBIS_RIDER_ACCEPT || anubis == FengNiaoStatuses.ANUBIS_ARRIVED_SHOP) {
            if (OrderStatus.ACCEPTED.equals(from)) {
                order.setStatus(OrderStatus.DELIVERING);
                if (order.getAutoCompleteAt() == null) {
                    order.setAutoCompleteAt(orderDeadlineSupport.autoCompleteFromNow());
                }
                orderMapper.updateById(order);
                logStatus(order.getId(), from, OrderStatus.DELIVERING, "SYSTEM", null,
                        "蜂鸟骑手接单" + (StringUtils.hasText(d.getRiderName()) ? "：" + d.getRiderName() : ""));
            } else if (anubis == FengNiaoStatuses.ANUBIS_ARRIVED_SHOP) {
                logStatus(order.getId(), from, from, "SYSTEM", null, "蜂鸟骑手已到店");
            }
            return;
        }
        if (anubis == FengNiaoStatuses.ANUBIS_DELIVERING) {
            if (order.getPickedUpAt() == null) {
                order.setPickedUpAt(LocalDateTime.now());
                if (OrderStatus.ACCEPTED.equals(from)) {
                    order.setStatus(OrderStatus.DELIVERING);
                }
                orderMapper.updateById(order);
                logStatus(order.getId(), from, order.getStatus(), "SYSTEM", null, "蜂鸟骑手已取餐");
            }
            return;
        }
        if (anubis == FengNiaoStatuses.ANUBIS_DONE) {
            if (OrderStatus.COMPLETED.equals(from) || OrderStatus.CANCELLED.equals(from)
                    || OrderStatus.REFUNDED.equals(from)) {
                return;
            }
            order.setStatus(OrderStatus.COMPLETED);
            order.setDeliveredAt(LocalDateTime.now());
            order.setCompletedAt(LocalDateTime.now());
            order.setAutoCompleteAt(null);
            orderMapper.updateById(order);
            logStatus(order.getId(), from, OrderStatus.COMPLETED, "SYSTEM", null, "蜂鸟确认送达");
            merchantSettleService.settleOrderIncome(order);
            return;
        }
        if (anubis == FengNiaoStatuses.ANUBIS_CANCELLED || anubis == FengNiaoStatuses.ANUBIS_EXCEPTION) {
            logStatus(order.getId(), from, from, "SYSTEM", null, "蜂鸟运单取消/异常，请改派或退款");
        }
    }

    public List<DeliveryDispatch> dueMock(LocalDateTime now, int limit) {
        return dispatchMapper.selectList(new LambdaQueryWrapper<DeliveryDispatch>()
                .eq(DeliveryDispatch::getMock, 1)
                .in(DeliveryDispatch::getStatus, FengNiaoStatuses.CREATED, FengNiaoStatuses.RIDER_ACCEPTED,
                        FengNiaoStatuses.ARRIVED, FengNiaoStatuses.DELIVERING)
                .isNotNull(DeliveryDispatch::getNextMockAt)
                .le(DeliveryDispatch::getNextMockAt, now)
                .orderByAsc(DeliveryDispatch::getNextMockAt)
                .last("LIMIT " + limit));
    }

    public int nextMockAnubis(String local) {
        return switch (local) {
            case FengNiaoStatuses.CREATED -> FengNiaoStatuses.ANUBIS_RIDER_ACCEPT;
            case FengNiaoStatuses.RIDER_ACCEPTED -> FengNiaoStatuses.ANUBIS_ARRIVED_SHOP;
            case FengNiaoStatuses.ARRIVED -> FengNiaoStatuses.ANUBIS_DELIVERING;
            case FengNiaoStatuses.DELIVERING -> FengNiaoStatuses.ANUBIS_DONE;
            default -> FengNiaoStatuses.ANUBIS_DONE;
        };
    }

    private void logStatus(Long orderId, String from, String to, String opType, Long opId, String remark) {
        OrderStatusLog logRow = new OrderStatusLog();
        logRow.setOrderId(orderId);
        logRow.setFromStatus(from == null ? "" : from);
        logRow.setToStatus(to);
        logRow.setOperatorType(opType);
        logRow.setOperatorId(opId);
        logRow.setRemark(remark == null ? "" : remark);
        orderStatusLogMapper.insert(logRow);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseAddress(String snapshot) {
        if (!StringUtils.hasText(snapshot)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(snapshot, Map.class);
        } catch (Exception e) {
            return Map.of();
        }
    }

    private String writeJson(Map<String, Object> body) {
        try {
            return objectMapper.writeValueAsString(body);
        } catch (Exception e) {
            return String.valueOf(body);
        }
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    private static BigDecimal nzDec(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static String str(Map<String, Object> m, String... keys) {
        if (m == null) {
            return "";
        }
        for (String k : keys) {
            Object v = m.get(k);
            if (v != null && StringUtils.hasText(String.valueOf(v))) {
                return String.valueOf(v);
            }
        }
        return "";
    }

    private static BigDecimal dec(Map<String, Object> m, String key) {
        Object v = m == null ? null : m.get(key);
        if (v == null) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(String.valueOf(v));
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private static int intVal(Map<String, Object> m, String... keys) {
        for (String k : keys) {
            Object v = m.get(k);
            if (v instanceof Number n) {
                return n.intValue();
            }
            if (v != null) {
                try {
                    return Integer.parseInt(String.valueOf(v));
                } catch (Exception ignored) {
                }
            }
        }
        return 0;
    }
}
