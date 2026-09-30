package com.qs.takeout.modules.delivery;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qs.takeout.common.auth.AuthContext;
import com.qs.takeout.common.auth.AuthUser;
import com.qs.takeout.common.auth.Roles;
import com.qs.takeout.common.exception.BizException;
import com.qs.takeout.modules.auth.entity.RiderAccount;
import com.qs.takeout.modules.auth.mapper.RiderAccountMapper;
import com.qs.takeout.common.sms.SmsCodeService;
import com.qs.takeout.modules.delivery.dto.RiderChangePhoneRequest;
import com.qs.takeout.modules.delivery.dto.RiderDailyStatVO;
import com.qs.takeout.modules.delivery.dto.RiderLocationRequest;
import com.qs.takeout.modules.delivery.dto.RiderOrderVO;
import com.qs.takeout.modules.delivery.dto.RiderProfileUpdateRequest;
import com.qs.takeout.modules.delivery.dto.RiderReviewVO;
import com.qs.takeout.modules.delivery.dto.RiderStatsVO;
import com.qs.takeout.modules.finance.MerchantSettleService;
import com.qs.takeout.modules.finance.RiderSettleService;
import com.qs.takeout.modules.goods.entity.Goods;
import com.qs.takeout.modules.goods.mapper.GoodsMapper;
import com.qs.takeout.modules.delivery.RiderProperties;
import com.qs.takeout.modules.order.OrderDeadlineSupport;
import com.qs.takeout.modules.order.OrderStatus;
import com.qs.takeout.modules.order.ReviewService;
import com.qs.takeout.modules.order.dto.OrderDetailVO;
import com.qs.takeout.modules.order.dto.OrderItemVO;
import com.qs.takeout.modules.order.entity.OrderEntity;
import com.qs.takeout.modules.order.entity.OrderItemEntity;
import com.qs.takeout.modules.order.entity.OrderReview;
import com.qs.takeout.modules.order.entity.OrderStatusLog;
import com.qs.takeout.modules.order.mapper.OrderGrabMapper;
import com.qs.takeout.modules.order.mapper.OrderItemMapper;
import com.qs.takeout.modules.order.mapper.OrderMapper;
import com.qs.takeout.modules.order.mapper.OrderReviewMapper;
import com.qs.takeout.modules.order.mapper.OrderStatusLogMapper;
import com.qs.takeout.modules.shop.entity.Shop;
import com.qs.takeout.modules.shop.mapper.ShopMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RiderService {

    private final RiderAccountMapper riderAccountMapper;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderStatusLogMapper orderStatusLogMapper;
    private final OrderGrabMapper orderGrabMapper;
    private final ShopMapper shopMapper;
    private final GoodsMapper goodsMapper;
    private final MerchantSettleService merchantSettleService;
    private final RiderSettleService riderSettleService;
    private final OrderDeadlineSupport orderDeadlineSupport;
    private final RiderProperties riderProperties;
    private final ReviewService reviewService;
    private final OrderReviewMapper orderReviewMapper;
    private final SmsCodeService smsCodeService;
    private final ObjectMapper objectMapper;

    public RiderAccount requireRider() {
        AuthUser user = AuthContext.require();
        if (!Roles.RIDER.equals(user.getRole())) {
            throw new BizException(403, "仅骑手可操作");
        }
        RiderAccount rider = riderAccountMapper.selectById(user.getId());
        if (rider == null || rider.getStatus() != null && rider.getStatus() == 0) {
            throw new BizException("骑手账号无效");
        }
        return rider;
    }

    public RiderAccount profile() {
        return requireRider();
    }

    public RiderAccount setOnline(boolean online) {
        RiderAccount rider = requireRider();
        rider.setOnline(online ? 1 : 0);
        riderAccountMapper.updateById(rider);
        return rider;
    }

    public RiderStatsVO stats() {
        RiderAccount rider = requireRider();
        LocalDateTime dayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        List<OrderEntity> delivering = orderMapper.selectList(new LambdaQueryWrapper<OrderEntity>()
                .eq(OrderEntity::getRiderId, rider.getId())
                .eq(OrderEntity::getStatus, OrderStatus.DELIVERING));
        int waitPickup = 0;
        int onWay = 0;
        for (OrderEntity o : delivering) {
            if (o.getPickedUpAt() == null) waitPickup++;
            else onWay++;
        }
        List<OrderEntity> todayDone = orderMapper.selectList(new LambdaQueryWrapper<OrderEntity>()
                .eq(OrderEntity::getRiderId, rider.getId())
                .eq(OrderEntity::getStatus, OrderStatus.COMPLETED)
                .ge(OrderEntity::getCompletedAt, dayStart));
        BigDecimal income = todayDone.stream()
                .map(o -> o.getDeliveryFee() == null ? BigDecimal.ZERO : o.getDeliveryFee())
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        LocalDateTime weekStart = LocalDateTime.of(LocalDate.now().minusDays(6), LocalTime.MIN);
        LocalDateTime monthStart = LocalDateTime.of(LocalDate.now().withDayOfMonth(1), LocalTime.MIN);
        BigDecimal weekIncome = sumDeliveryIncome(rider.getId(), weekStart);
        BigDecimal monthIncome = sumDeliveryIncome(rider.getId(), monthStart);
        Long totalDone = orderMapper.selectCount(new LambdaQueryWrapper<OrderEntity>()
                .eq(OrderEntity::getRiderId, rider.getId())
                .eq(OrderEntity::getStatus, OrderStatus.COMPLETED));
        return RiderStatsVO.builder()
                .online(rider.getOnline() == null ? 0 : rider.getOnline())
                .waitPickupCount(waitPickup)
                .onWayCount(onWay)
                .todayCompleted(todayDone.size())
                .todayIncome(income)
                .weekIncome(weekIncome)
                .monthIncome(monthIncome)
                .totalCompleted(totalDone == null ? 0 : totalDone.intValue())
                .withdrawableBalance(rider.getWithdrawableBalance() == null ? BigDecimal.ZERO : rider.getWithdrawableBalance())
                .build();
    }

    private BigDecimal sumDeliveryIncome(Long riderId, LocalDateTime since) {
        List<OrderEntity> list = orderMapper.selectList(new LambdaQueryWrapper<OrderEntity>()
                .eq(OrderEntity::getRiderId, riderId)
                .eq(OrderEntity::getStatus, OrderStatus.COMPLETED)
                .ge(OrderEntity::getCompletedAt, since));
        return list.stream()
                .map(o -> o.getDeliveryFee() == null ? BigDecimal.ZERO : o.getDeliveryFee())
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public List<RiderOrderVO> pool() {
        requireRider();
        if (!riderProperties.isSelfEnabled()) {
            return List.of();
        }
        List<OrderEntity> orders = orderMapper.selectList(new LambdaQueryWrapper<OrderEntity>()
                .eq(OrderEntity::getStatus, OrderStatus.ACCEPTED)
                .eq(OrderEntity::getDeliveryType, "PLATFORM")
                .isNull(OrderEntity::getRiderId)
                .orderByAsc(OrderEntity::getAcceptedAt)
                .last("limit 50"));
        return orders.stream().map(o -> toCard(o, "POOL")).toList();
    }

    public List<RiderOrderVO> mine(String phase) {
        RiderAccount rider = requireRider();
        LambdaQueryWrapper<OrderEntity> q = new LambdaQueryWrapper<OrderEntity>()
                .eq(OrderEntity::getRiderId, rider.getId())
                .orderByDesc(OrderEntity::getId);
        String p = phase == null ? "" : phase.trim().toUpperCase();
        if ("COMPLETED".equals(p)) {
            q.eq(OrderEntity::getStatus, OrderStatus.COMPLETED);
        } else if ("WAIT_PICKUP".equals(p)) {
            q.eq(OrderEntity::getStatus, OrderStatus.DELIVERING).isNull(OrderEntity::getPickedUpAt);
        } else if ("ON_WAY".equals(p)) {
            q.eq(OrderEntity::getStatus, OrderStatus.DELIVERING).isNotNull(OrderEntity::getPickedUpAt);
        } else if ("CANCELLED".equals(p)) {
            q.in(OrderEntity::getStatus, OrderStatus.CANCELLED, OrderStatus.REFUNDED);
        } else if ("DELIVERING".equals(p)) {
            q.eq(OrderEntity::getStatus, OrderStatus.DELIVERING);
        } else {
            q.in(OrderEntity::getStatus, OrderStatus.DELIVERING, OrderStatus.COMPLETED);
        }
        return orderMapper.selectList(q).stream()
                .map(o -> toCard(o, resolvePhase(o)))
                .toList();
    }

    public OrderDetailVO detail(Long orderId) {
        OrderEntity order = requireOwnOrPool(orderId);
        return buildDetail(order);
    }

    @Transactional
    public OrderDetailVO grab(Long orderId) {
        if (!riderProperties.isSelfEnabled()) {
            throw new BizException("自有骑手暂未开放，订单由蜂鸟众包配送");
        }
        RiderAccount rider = requireRider();
        if (rider.getOnline() == null || rider.getOnline() == 0) {
            throw new BizException("请先上线接单");
        }
        int n = orderGrabMapper.grab(orderId, rider.getId());
        if (n != 1) {
            throw new BizException("抢单失败，可能已被抢走");
        }
        OrderEntity order = orderMapper.selectById(orderId);
        if (order != null) {
            order.setAutoCompleteAt(orderDeadlineSupport.autoCompleteFromNow());
            orderMapper.updateById(order);
        }
        logStatus(orderId, OrderStatus.ACCEPTED, OrderStatus.DELIVERING, rider.getId(), "骑手抢单");
        return detail(orderId);
    }

    @Transactional
    public OrderDetailVO pickup(Long orderId) {
        RiderAccount rider = requireRider();
        OrderEntity order = requireMineDelivering(orderId, rider.getId());
        if (order.getPickedUpAt() != null) {
            throw new BizException("已取餐，请前往送达");
        }
        order.setPickedUpAt(LocalDateTime.now());
        orderMapper.updateById(order);
        logStatus(orderId, order.getStatus(), order.getStatus(), rider.getId(), "骑手已取餐");
        return detail(orderId);
    }

    @Transactional
    public OrderDetailVO deliver(Long orderId) {
        RiderAccount rider = requireRider();
        OrderEntity order = requireMineDelivering(orderId, rider.getId());
        if (order.getPickedUpAt() == null) {
            throw new BizException("请先确认取餐");
        }
        String from = order.getStatus();
        order.setStatus(OrderStatus.COMPLETED);
        order.setDeliveredAt(LocalDateTime.now());
        order.setCompletedAt(LocalDateTime.now());
        order.setAutoCompleteAt(null);
        orderMapper.updateById(order);
        logStatus(orderId, from, OrderStatus.COMPLETED, rider.getId(), "骑手确认送达");
        merchantSettleService.settleOrderIncome(order);
        riderSettleService.settleDeliveryIncome(order);
        return detail(orderId);
    }

    public RiderAccount updateProfile(RiderProfileUpdateRequest req) {
        RiderAccount rider = requireRider();
        if (req.getName() != null && !req.getName().isBlank()) {
            rider.setName(req.getName().trim());
        }
        if (req.getAvatarUrl() != null) {
            rider.setAvatarUrl(req.getAvatarUrl().trim());
        }
        if (req.getRealName() != null) {
            rider.setRealName(req.getRealName().trim());
        }
        riderAccountMapper.updateById(rider);
        return rider;
    }

    @Transactional
    public RiderAccount changePhone(RiderChangePhoneRequest req) {
        RiderAccount rider = requireRider();
        if (req == null || !StringUtils.hasText(req.getPhone()) || !StringUtils.hasText(req.getCode())) {
            throw new BizException("手机号或验证码不能为空");
        }
        String phone = req.getPhone().trim();
        if (!phone.matches("^1\\d{10}$")) {
            throw new BizException("手机号不正确");
        }
        smsCodeService.verifyOrThrow(phone, "LOGIN_RIDER", req.getCode().trim());
        if (phone.equals(rider.getPhone())) {
            return rider;
        }
        Long exists = riderAccountMapper.selectCount(new LambdaQueryWrapper<RiderAccount>()
                .eq(RiderAccount::getPhone, phone)
                .ne(RiderAccount::getId, rider.getId()));
        if (exists != null && exists > 0) {
            throw new BizException("该手机号已被占用");
        }
        rider.setPhone(phone);
        riderAccountMapper.updateById(rider);
        return rider;
    }

    public List<RiderOrderVO> cancelledRecent(Integer minutes) {
        RiderAccount rider = requireRider();
        int m = minutes == null || minutes < 1 ? 60 : Math.min(minutes, 24 * 60);
        LocalDateTime since = LocalDateTime.now().minusMinutes(m);
        List<OrderEntity> orders = orderMapper.selectList(new LambdaQueryWrapper<OrderEntity>()
                .eq(OrderEntity::getRiderId, rider.getId())
                .in(OrderEntity::getStatus, OrderStatus.CANCELLED, OrderStatus.REFUNDED)
                .ge(OrderEntity::getUpdatedAt, since)
                .orderByDesc(OrderEntity::getUpdatedAt)
                .last("limit 30"));
        return orders.stream().map(o -> toCard(o, "CANCELLED")).toList();
    }

    public List<RiderDailyStatVO> dailyStats(Integer days) {
        RiderAccount rider = requireRider();
        int d = days == null || days < 1 ? 7 : Math.min(days, 31);
        LocalDate today = LocalDate.now();
        List<RiderDailyStatVO> list = new ArrayList<>();
        for (int i = d - 1; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            LocalDateTime start = LocalDateTime.of(day, LocalTime.MIN);
            LocalDateTime end = LocalDateTime.of(day, LocalTime.MAX);
            List<OrderEntity> done = orderMapper.selectList(new LambdaQueryWrapper<OrderEntity>()
                    .eq(OrderEntity::getRiderId, rider.getId())
                    .eq(OrderEntity::getStatus, OrderStatus.COMPLETED)
                    .ge(OrderEntity::getCompletedAt, start)
                    .le(OrderEntity::getCompletedAt, end));
            BigDecimal income = done.stream()
                    .map(o -> o.getDeliveryFee() == null ? BigDecimal.ZERO : o.getDeliveryFee())
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(2, RoundingMode.HALF_UP);
            list.add(RiderDailyStatVO.builder()
                    .date(day.format(DateTimeFormatter.ISO_LOCAL_DATE))
                    .completed(done.size())
                    .income(income)
                    .build());
        }
        return list;
    }

    public List<RiderReviewVO> reviews(Integer limit) {
        RiderAccount rider = requireRider();
        int size = limit == null || limit < 1 ? 30 : Math.min(limit, 100);
        List<OrderEntity> orders = orderMapper.selectList(new LambdaQueryWrapper<OrderEntity>()
                .eq(OrderEntity::getRiderId, rider.getId())
                .eq(OrderEntity::getStatus, OrderStatus.COMPLETED)
                .orderByDesc(OrderEntity::getId)
                .last("limit 200"));
        List<RiderReviewVO> out = new ArrayList<>();
        for (OrderEntity order : orders) {
            if (out.size() >= size) break;
            OrderReview review = orderReviewMapper.selectOne(new LambdaQueryWrapper<OrderReview>()
                    .eq(OrderReview::getOrderId, order.getId())
                    .last("limit 1"));
            if (review == null) continue;
            Shop shop = shopMapper.selectById(order.getShopId());
            out.add(RiderReviewVO.builder()
                    .orderId(order.getId())
                    .orderNo(order.getOrderNo())
                    .shopName(shop == null ? "" : shop.getName())
                    .score(review.getScore())
                    .content(review.getContent())
                    .imageUrls(reviewService.parseImages(review.getImageUrlsRaw()))
                    .createdAt(review.getCreatedAt())
                    .build());
        }
        return out;
    }

    public RiderAccount reportLocation(RiderLocationRequest req) {
        RiderAccount rider = requireRider();
        if (req.getLat() == null || req.getLng() == null) {
            throw new BizException("定位坐标无效");
        }
        rider.setLat(req.getLat());
        rider.setLng(req.getLng());
        rider.setLocationAt(LocalDateTime.now());
        riderAccountMapper.updateById(rider);
        return rider;
    }

    private OrderEntity requireMineDelivering(Long orderId, Long riderId) {
        OrderEntity order = orderMapper.selectById(orderId);
        if (order == null || !riderId.equals(order.getRiderId())) {
            throw new BizException("订单不存在");
        }
        if (!OrderStatus.DELIVERING.equals(order.getStatus())) {
            throw new BizException("当前状态不可操作");
        }
        return order;
    }

    private OrderEntity requireOwnOrPool(Long orderId) {
        RiderAccount rider = requireRider();
        OrderEntity order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException("订单不存在");
        }
        boolean mine = rider.getId().equals(order.getRiderId());
        boolean pool = OrderStatus.ACCEPTED.equals(order.getStatus())
                && "PLATFORM".equals(order.getDeliveryType())
                && order.getRiderId() == null;
        boolean cancelledMine = mine && (OrderStatus.CANCELLED.equals(order.getStatus())
                || OrderStatus.REFUNDED.equals(order.getStatus()));
        if (!mine && !pool && !cancelledMine) {
            throw new BizException("无权查看该订单");
        }
        return order;
    }

    private OrderDetailVO buildDetail(OrderEntity order) {
        Shop shop = shopMapper.selectById(order.getShopId());
        Map<String, Object> address = parseAddress(order.getAddressSnapshot());
        Map<String, Object> shopMap = new HashMap<>();
        if (shop != null) {
            shopMap.put("id", shop.getId());
            shopMap.put("name", shop.getName());
            shopMap.put("address", shop.getAddress());
            shopMap.put("phone", shop.getPhone());
            shopMap.put("lat", shop.getLat());
            shopMap.put("lng", shop.getLng());
        }
        List<OrderStatusLog> logs = orderStatusLogMapper.selectList(new LambdaQueryWrapper<OrderStatusLog>()
                .eq(OrderStatusLog::getOrderId, order.getId())
                .orderByAsc(OrderStatusLog::getId));
        boolean pickedUp = order.getPickedUpAt() != null;
        return OrderDetailVO.builder()
                .order(order)
                .items(toItemVOs(itemsOf(order.getId())))
                .shop(shopMap)
                .address(address)
                .pickedUp(pickedUp)
                .phase(resolvePhase(order))
                .distanceMeters(distanceMeters(shop, address))
                .logs(logs)
                .build();
    }

    private RiderOrderVO toCard(OrderEntity order, String phase) {
        Shop shop = shopMapper.selectById(order.getShopId());
        Map<String, Object> address = parseAddress(order.getAddressSnapshot());
        long itemCount = orderItemMapper.selectCount(new LambdaQueryWrapper<OrderItemEntity>()
                .eq(OrderItemEntity::getOrderId, order.getId()));
        Integer wait = null;
        if (order.getAcceptedAt() != null && OrderStatus.ACCEPTED.equals(order.getStatus())) {
            wait = (int) Math.max(0, Duration.between(order.getAcceptedAt(), LocalDateTime.now()).toMinutes());
        }
        return RiderOrderVO.builder()
                .order(order)
                .shopName(shop == null ? "" : shop.getName())
                .shopAddress(shop == null ? "" : shop.getAddress())
                .shopPhone(shop == null ? "" : shop.getPhone())
                .shopLat(shop == null ? null : shop.getLat())
                .shopLng(shop == null ? null : shop.getLng())
                .address(address)
                .itemCount((int) itemCount)
                .waitMinutes(wait)
                .distanceMeters(distanceMeters(shop, address))
                .income(order.getDeliveryFee() == null ? BigDecimal.ZERO : order.getDeliveryFee())
                .pickedUp(order.getPickedUpAt() != null)
                .phase(phase)
                .build();
    }

    private String resolvePhase(OrderEntity order) {
        if (OrderStatus.COMPLETED.equals(order.getStatus())) return "COMPLETED";
        if (OrderStatus.CANCELLED.equals(order.getStatus()) || OrderStatus.REFUNDED.equals(order.getStatus())) {
            return "CANCELLED";
        }
        if (OrderStatus.ACCEPTED.equals(order.getStatus()) && order.getRiderId() == null) return "POOL";
        if (OrderStatus.DELIVERING.equals(order.getStatus())) {
            return order.getPickedUpAt() == null ? "WAIT_PICKUP" : "ON_WAY";
        }
        return order.getStatus();
    }

    private Integer distanceMeters(Shop shop, Map<String, Object> address) {
        if (shop == null || shop.getLat() == null || shop.getLng() == null || address == null) return null;
        Double lat = toDouble(address.get("lat"));
        Double lng = toDouble(address.get("lng"));
        if (lat == null || lng == null) return null;
        return (int) Math.round(haversineMeters(
                shop.getLat().doubleValue(), shop.getLng().doubleValue(), lat, lng));
    }

    private static Double toDouble(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.doubleValue();
        try {
            return Double.parseDouble(String.valueOf(v));
        } catch (Exception e) {
            return null;
        }
    }

    private static double haversineMeters(double lat1, double lng1, double lat2, double lng2) {
        double r = 6371000d;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 2 * r * Math.asin(Math.sqrt(a));
    }

    private Map<String, Object> parseAddress(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }

    private List<OrderItemEntity> itemsOf(Long orderId) {
        return orderItemMapper.selectList(new LambdaQueryWrapper<OrderItemEntity>()
                .eq(OrderItemEntity::getOrderId, orderId));
    }

    private List<OrderItemVO> toItemVOs(List<OrderItemEntity> items) {
        return items.stream().map(it -> {
            String cover = "";
            if (it.getGoodsId() != null) {
                Goods g = goodsMapper.selectById(it.getGoodsId());
                if (g != null && g.getCoverUrl() != null && !g.getCoverUrl().isBlank()) {
                    cover = g.getCoverUrl();
                } else {
                    cover = "https://picsum.photos/seed/g" + it.getGoodsId() + "/400";
                }
            }
            return OrderItemVO.builder()
                    .id(it.getId())
                    .orderId(it.getOrderId())
                    .goodsId(it.getGoodsId())
                    .skuId(it.getSkuId())
                    .goodsName(it.getGoodsName())
                    .skuName(it.getSkuName())
                    .price(it.getPrice())
                    .quantity(it.getQuantity())
                    .coverUrl(cover)
                    .build();
        }).toList();
    }

    private void logStatus(Long orderId, String from, String to, Long riderId, String remark) {
        OrderStatusLog log = new OrderStatusLog();
        log.setOrderId(orderId);
        log.setFromStatus(from == null ? "" : from);
        log.setToStatus(to);
        log.setOperatorType(Roles.RIDER);
        log.setOperatorId(riderId);
        log.setRemark(remark);
        orderStatusLogMapper.insert(log);
    }
}
