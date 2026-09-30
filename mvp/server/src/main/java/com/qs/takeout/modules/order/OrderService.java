package com.qs.takeout.modules.order;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qs.takeout.common.auth.AuthContext;
import com.qs.takeout.common.auth.AuthUser;
import com.qs.takeout.common.auth.Roles;
import com.qs.takeout.common.exception.BizException;
import com.qs.takeout.modules.auth.entity.RiderAccount;
import com.qs.takeout.modules.auth.mapper.RiderAccountMapper;
import com.qs.takeout.modules.cart.CartService;
import com.qs.takeout.modules.cart.dto.CartAddRequest;
import com.qs.takeout.modules.cart.entity.CartItem;
import com.qs.takeout.modules.goods.entity.Goods;
import com.qs.takeout.modules.goods.entity.GoodsSku;
import com.qs.takeout.modules.goods.mapper.GoodsMapper;
import com.qs.takeout.modules.goods.mapper.GoodsSkuMapper;
import com.qs.takeout.modules.goods.mapper.GoodsSkuStockMapper;
import com.qs.takeout.modules.order.dto.CreateOrderRequest;
import com.qs.takeout.modules.order.dto.MerchantOrderCardVO;
import com.qs.takeout.modules.order.dto.OrderDetailVO;
import com.qs.takeout.modules.order.dto.OrderItemVO;
import com.qs.takeout.modules.order.dto.UserOrderCardVO;
import com.qs.takeout.modules.order.entity.OrderEntity;
import com.qs.takeout.modules.order.entity.OrderItemEntity;
import com.qs.takeout.modules.order.entity.OrderReview;
import com.qs.takeout.modules.order.entity.OrderStatusLog;
import com.qs.takeout.modules.order.mapper.OrderItemMapper;
import com.qs.takeout.modules.order.mapper.OrderMapper;
import com.qs.takeout.modules.order.mapper.OrderStatusLogMapper;
import com.qs.takeout.modules.pay.PayChannels;
import com.qs.takeout.modules.pay.PayService;
import com.qs.takeout.modules.pay.entity.PaymentRecord;
import com.qs.takeout.modules.pay.mapper.PaymentRecordMapper;
import com.qs.takeout.modules.promo.CouponService;
import com.qs.takeout.modules.delivery.fengniao.FengNiaoDispatchService;
import com.qs.takeout.modules.finance.MerchantSettleService;
import com.qs.takeout.modules.finance.RiderSettleService;
import com.qs.takeout.modules.shop.MerchantShopSupport;
import com.qs.takeout.modules.shop.entity.Shop;
import com.qs.takeout.modules.shop.mapper.ShopMapper;
import com.qs.takeout.modules.user.AddressService;
import com.qs.takeout.modules.user.entity.UserAddress;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderStatusLogMapper orderStatusLogMapper;
    private final PaymentRecordMapper paymentRecordMapper;
    private final PayService payService;
    private final CartService cartService;
    private final AddressService addressService;
    private final ShopMapper shopMapper;
    private final GoodsMapper goodsMapper;
    private final GoodsSkuMapper goodsSkuMapper;
    private final GoodsSkuStockMapper goodsSkuStockMapper;
    private final MerchantShopSupport merchantShopSupport;
    private final CouponService couponService;
    private final ReviewService reviewService;
    private final MerchantSettleService merchantSettleService;
    private final RiderSettleService riderSettleService;
    private final OrderDeadlineSupport orderDeadlineSupport;
    private final FengNiaoDispatchService fengNiaoDispatchService;
    private final RiderAccountMapper riderAccountMapper;
    private final ObjectMapper objectMapper;

    @Transactional
    public OrderDetailVO create(CreateOrderRequest req) {
        AuthUser user = AuthContext.require();
        if (!Roles.USER.equals(user.getRole())) {
            throw new BizException(403, "仅用户可下单");
        }
        Shop shop = shopMapper.selectById(req.getShopId());
        if (shop == null || shop.getStatus() == null || shop.getStatus() == 0) {
            throw new BizException("店铺不可用");
        }
        if (shop.getOpenStatus() == null || shop.getOpenStatus() == 0) {
            throw new BizException("店铺未营业");
        }

        UserAddress address = addressService.requireOwn(req.getAddressId());
        List<CartItem> cartItems = cartService.listByShop(user.getId(), req.getShopId());
        if (cartItems.isEmpty()) {
            throw new BizException("购物车为空");
        }

        BigDecimal goodsAmount = BigDecimal.ZERO;
        List<OrderItemEntity> lines = new ArrayList<>();
        for (CartItem cartItem : cartItems) {
            GoodsSku sku = goodsSkuMapper.selectById(cartItem.getSkuId());
            Goods goods = goodsMapper.selectById(cartItem.getGoodsId());
            if (sku == null || goods == null || goods.getStatus() == 0 || sku.getStatus() == 0) {
                throw new BizException("存在已下架商品，请刷新购物车");
            }
            int updated = goodsSkuStockMapper.deduct(sku.getId(), cartItem.getQuantity());
            if (updated != 1) {
                throw new BizException("「" + goods.getName() + "」库存不足");
            }
            OrderItemEntity line = new OrderItemEntity();
            line.setGoodsId(goods.getId());
            line.setSkuId(sku.getId());
            line.setGoodsName(goods.getName());
            line.setSkuName(sku.getName());
            line.setPrice(sku.getPrice());
            line.setQuantity(cartItem.getQuantity());
            lines.add(line);
            goodsAmount = goodsAmount.add(sku.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
        }

        if (goodsAmount.compareTo(shop.getMinOrderAmount()) < 0) {
            // 回滚库存
            restoreLines(lines);
            throw new BizException("未满起送价 " + shop.getMinOrderAmount());
        }

        BigDecimal packing = shop.getPackingFee() == null ? BigDecimal.ZERO : shop.getPackingFee();
        BigDecimal delivery = shop.getDeliveryFee() == null ? BigDecimal.ZERO : shop.getDeliveryFee();
        BigDecimal discount = couponService.previewDiscount(req.getUserCouponId(), shop.getId(), goodsAmount);
        if (discount.compareTo(goodsAmount.add(packing).add(delivery)) > 0) {
            discount = goodsAmount.add(packing).add(delivery);
        }
        BigDecimal payAmount = goodsAmount.add(packing).add(delivery).subtract(discount);
        if (payAmount.compareTo(BigDecimal.ZERO) < 0) {
            payAmount = BigDecimal.ZERO;
        }

        OrderEntity order = new OrderEntity();
        order.setOrderNo(nextOrderNo());
        order.setUserId(user.getId());
        order.setShopId(shop.getId());
        order.setStatus(OrderStatus.PENDING_PAY);
        order.setDeliveryType(StringUtils.hasText(req.getDeliveryType()) ? req.getDeliveryType() : "PLATFORM");
        order.setGoodsAmount(goodsAmount);
        order.setPackingFee(packing);
        order.setDeliveryFee(delivery);
        order.setDiscountAmount(discount);
        order.setUserCouponId(req.getUserCouponId());
        order.setPayAmount(payAmount);
        order.setRemark(req.getRemark() == null ? "" : req.getRemark());
        order.setAddressSnapshot(toAddressJson(address));
        order.setPayDeadlineAt(orderDeadlineSupport.payDeadlineFromNow());
        order.setCancelReason("");
        order.setRefundReason("");
        orderMapper.insert(order);

        for (OrderItemEntity line : lines) {
            line.setOrderId(order.getId());
            orderItemMapper.insert(line);
        }
        logStatus(order.getId(), "", OrderStatus.PENDING_PAY, Roles.USER, user.getId(), "创建订单");
        couponService.markUsed(req.getUserCouponId(), order.getId());

        payService.createPendingPayment(order, resolvePayChannel(req.getPayChannel()));

        cartService.clearShop(user.getId(), shop.getId());

        if (Boolean.TRUE.equals(req.getMockPay())) {
            payService.confirmMockPay(order.getId());
            return detailForUser(order.getId());
        }
        return detailForUser(order.getId());
    }

    private String resolvePayChannel(String channel) {
        if (!StringUtils.hasText(channel)) {
            return PayChannels.MOCK;
        }
        return channel.trim().toUpperCase();
    }

    @Transactional
    public OrderDetailVO mockPay(Long orderId) {
        AuthUser user = AuthContext.require();
        OrderEntity order = orderMapper.selectById(orderId);
        if (order == null || !user.getId().equals(order.getUserId())) {
            throw new BizException("订单不存在");
        }
        payService.confirmMockPay(orderId);
        return detailForUser(orderId);
    }

    @Transactional
    public OrderDetailVO cancel(Long orderId, String reason) {
        AuthUser user = AuthContext.require();
        OrderEntity order = orderMapper.selectById(orderId);
        if (order == null || !user.getId().equals(order.getUserId())) {
            throw new BizException("订单不存在");
        }
        if (!OrderStatus.PENDING_PAY.equals(order.getStatus()) && !OrderStatus.PAID.equals(order.getStatus())) {
            throw new BizException("当前状态不可取消");
        }
        List<OrderItemEntity> items = itemsOf(orderId);
        restoreLines(items);
        String from = order.getStatus();
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelReason(StringUtils.hasText(reason) ? reason : "用户取消");
        order.setPayDeadlineAt(null);
        order.setAcceptDeadlineAt(null);
        order.setAutoCompleteAt(null);
        orderMapper.updateById(order);
        logStatus(orderId, from, OrderStatus.CANCELLED, Roles.USER, user.getId(), order.getCancelReason());
        couponService.restore(order.getUserCouponId());

        if (OrderStatus.PAID.equals(from)) {
            markRefund(orderId);
        } else {
            closePendingPayment(orderId);
        }
        return detailForUser(orderId);
    }

    /**
     * 待支付超时：CAS 取消 + 回库存 + 关支付单。
     */
    @Transactional
    public void closePayTimeout(Long orderId) {
        int n = orderMapper.casCancelPayTimeout(orderId, "超时未支付自动取消");
        if (n != 1) {
            return;
        }
        OrderEntity order = orderMapper.selectById(orderId);
        if (order == null) {
            return;
        }
        restoreLines(itemsOf(orderId));
        couponService.restore(order.getUserCouponId());
        closePendingPayment(orderId);
        logStatus(orderId, OrderStatus.PENDING_PAY, OrderStatus.CANCELLED, "SYSTEM", null, "超时未支付自动取消");
        log.info("order pay timeout cancelled id={} no={}", orderId, order.getOrderNo());
    }

    /**
     * 待接单超时：CAS 退款态 + 回库存 + 标记退款。
     */
    @Transactional
    public void closeAcceptTimeout(Long orderId) {
        int n = orderMapper.casAcceptTimeoutRefund(orderId, "商家超时未接单自动退款");
        if (n != 1) {
            return;
        }
        OrderEntity order = orderMapper.selectById(orderId);
        if (order == null) {
            return;
        }
        restoreLines(itemsOf(orderId));
        couponService.restore(order.getUserCouponId());
        markRefund(orderId);
        logStatus(orderId, OrderStatus.PAID, OrderStatus.REFUNDED, "SYSTEM", null, "商家超时未接单自动退款");
        log.info("order accept timeout refunded id={} no={}", orderId, order.getOrderNo());
    }

    /**
     * 履约超时自动完成（平台配送中 / 商家自配已接单）。
     */
    @Transactional
    public void closeAutoComplete(Long orderId) {
        OrderEntity before = orderMapper.selectById(orderId);
        if (before == null) {
            return;
        }
        String from = before.getStatus();
        int n = orderMapper.casAutoComplete(orderId);
        if (n != 1) {
            return;
        }
        OrderEntity order = orderMapper.selectById(orderId);
        if (order == null) {
            return;
        }
        logStatus(orderId, from, OrderStatus.COMPLETED, "SYSTEM", null, "履约超时系统自动完成");
        merchantSettleService.settleOrderIncome(order);
        if (order.getRiderId() != null) {
            riderSettleService.settleDeliveryIncome(order);
        }
        log.info("order auto completed id={} no={} from={}", orderId, order.getOrderNo(), from);
    }

    @Transactional
    public OrderDetailVO applyRefund(Long orderId, String reason) {
        AuthUser user = AuthContext.require();
        OrderEntity order = orderMapper.selectById(orderId);
        if (order == null || !user.getId().equals(order.getUserId())) {
            throw new BizException("订单不存在");
        }
        if (!OrderStatus.PAID.equals(order.getStatus())
                && !OrderStatus.ACCEPTED.equals(order.getStatus())
                && !OrderStatus.DELIVERING.equals(order.getStatus())) {
            throw new BizException("当前状态不可申请退款");
        }
        String from = order.getStatus();
        order.setStatus(OrderStatus.REFUNDING);
        order.setRefundReason(StringUtils.hasText(reason) ? reason : "用户申请退款");
        orderMapper.updateById(order);
        logStatus(orderId, from, OrderStatus.REFUNDING, Roles.USER, user.getId(), order.getRefundReason());
        return detailForUser(orderId);
    }

    @Transactional
    public OrderDetailVO approveRefund(Long orderId) {
        OrderEntity order = requireMerchantOrder(orderId);
        if (!OrderStatus.REFUNDING.equals(order.getStatus())) {
            throw new BizException("仅退款中订单可同意");
        }
        restoreLines(itemsOf(orderId));
        String from = order.getStatus();
        order.setStatus(OrderStatus.REFUNDED);
        order.setPayDeadlineAt(null);
        order.setAcceptDeadlineAt(null);
        order.setAutoCompleteAt(null);
        orderMapper.updateById(order);
        logStatus(orderId, from, OrderStatus.REFUNDED, Roles.MERCHANT, AuthContext.require().getId(), "同意退款");
        markRefund(orderId);
        couponService.restore(order.getUserCouponId());
        merchantSettleService.reverseOrderIncome(order);
        fengNiaoDispatchService.cancelIfActive(orderId, "商家同意退款");
        return detailForMerchant(orderId);
    }

    @Transactional
    public OrderDetailVO rejectRefund(Long orderId, String reason) {
        OrderEntity order = requireMerchantOrder(orderId);
        if (!OrderStatus.REFUNDING.equals(order.getStatus())) {
            throw new BizException("仅退款中订单可拒绝");
        }
        // 退回商家已接单，便于继续履约
        String from = order.getStatus();
        order.setStatus(OrderStatus.ACCEPTED);
        if (StringUtils.hasText(reason)) {
            order.setRefundReason(order.getRefundReason() + "｜拒绝：" + reason);
        }
        if ("SELF".equalsIgnoreCase(order.getDeliveryType()) && order.getAutoCompleteAt() == null) {
            order.setAutoCompleteAt(orderDeadlineSupport.autoCompleteFromNow());
        }
        orderMapper.updateById(order);
        logStatus(orderId, from, OrderStatus.ACCEPTED, Roles.MERCHANT, AuthContext.require().getId(),
                StringUtils.hasText(reason) ? reason : "拒绝退款");
        return detailForMerchant(orderId);
    }

    public List<UserOrderCardVO> listMine() {
        Long userId = AuthContext.require().getId();
        List<OrderEntity> orders = orderMapper.selectList(new LambdaQueryWrapper<OrderEntity>()
                .eq(OrderEntity::getUserId, userId)
                .orderByDesc(OrderEntity::getId));
        if (orders.isEmpty()) {
            return List.of();
        }
        List<Long> orderIds = orders.stream().map(OrderEntity::getId).toList();
        List<OrderItemEntity> allItems = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItemEntity>()
                .in(OrderItemEntity::getOrderId, orderIds));
        Map<Long, List<OrderItemEntity>> byOrder = allItems.stream()
                .collect(Collectors.groupingBy(OrderItemEntity::getOrderId));

        Set<Long> shopIds = orders.stream().map(OrderEntity::getShopId).collect(Collectors.toSet());
        Map<Long, Shop> shopMap = new HashMap<>();
        for (Long sid : shopIds) {
            Shop shop = shopMapper.selectById(sid);
            if (shop != null) {
                shopMap.put(sid, shop);
            }
        }

        Set<Long> goodsIds = allItems.stream()
                .map(OrderItemEntity::getGoodsId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        Map<Long, String> coverMap = new HashMap<>();
        for (Long gid : goodsIds) {
            Goods g = goodsMapper.selectById(gid);
            if (g != null && g.getCoverUrl() != null && !g.getCoverUrl().isBlank()) {
                coverMap.put(gid, g.getCoverUrl());
            }
        }

        List<UserOrderCardVO> cards = new ArrayList<>();
        for (OrderEntity order : orders) {
            List<OrderItemEntity> items = byOrder.getOrDefault(order.getId(), List.of());
            int count = items.stream().mapToInt(i -> i.getQuantity() == null ? 0 : i.getQuantity()).sum();
            String summary = items.stream()
                    .limit(3)
                    .map(i -> i.getGoodsName() + "×" + i.getQuantity())
                    .collect(Collectors.joining("、"));
            if (items.size() > 3) {
                summary = summary + " 等";
            }
            List<String> covers = items.stream()
                    .map(i -> {
                        String url = coverMap.get(i.getGoodsId());
                        if (url == null || url.isBlank()) {
                            return "https://picsum.photos/seed/g" + i.getGoodsId() + "/400";
                        }
                        return url;
                    })
                    .distinct()
                    .limit(4)
                    .toList();
            Shop shop = shopMap.get(order.getShopId());
            OrderReview review = reviewService.ofOrder(order.getId());
            cards.add(UserOrderCardVO.builder()
                    .order(order)
                    .shopId(order.getShopId())
                    .shopName(shop == null ? "店铺" : shop.getName())
                    .shopLogoUrl(shop == null ? "" : (shop.getLogoUrl() == null ? "" : shop.getLogoUrl()))
                    .goodsCoverUrls(covers)
                    .itemCount(count)
                    .itemSummary(summary.isEmpty() ? "查看商品明细" : summary)
                    .reviewed(review != null)
                    .build());
        }
        return cards;
    }

    public OrderDetailVO detailForUser(Long orderId) {
        AuthUser user = AuthContext.require();
        OrderEntity order = orderMapper.selectById(orderId);
        if (order == null || !user.getId().equals(order.getUserId())) {
            throw new BizException("订单不存在");
        }
        List<OrderStatusLog> logs = orderStatusLogMapper.selectList(new LambdaQueryWrapper<OrderStatusLog>()
                .eq(OrderStatusLog::getOrderId, orderId)
                .orderByAsc(OrderStatusLog::getId));
        OrderReview review = reviewService.ofOrder(orderId);
        Shop shop = shopMapper.selectById(order.getShopId());
        Map<String, Object> shopInfo = new HashMap<>();
        if (shop != null) {
            shopInfo.put("id", shop.getId());
            shopInfo.put("name", shop.getName());
            shopInfo.put("phone", shop.getPhone());
            shopInfo.put("address", shop.getAddress());
            shopInfo.put("lat", shop.getLat());
            shopInfo.put("lng", shop.getLng());
        }
        Map<String, Object> addressInfo = parseAddressSnapshot(order.getAddressSnapshot());
        Map<String, Object> dispatch = fengNiaoDispatchService.toVo(orderId);
        return OrderDetailVO.builder()
                .order(order)
                .items(toItemVOs(itemsOf(orderId)))
                .logs(logs)
                .shop(shopInfo)
                .address(addressInfo)
                .rider(riderInfoOf(order, dispatch))
                .dispatch(dispatch)
                .reviewed(review != null)
                .review(review)
                .build();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseAddressSnapshot(String snapshot) {
        if (!StringUtils.hasText(snapshot)) {
            return null;
        }
        try {
            return objectMapper.readValue(snapshot, Map.class);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    @Transactional
    public Map<String, Object> reorder(Long orderId) {
        AuthUser user = AuthContext.require();
        OrderEntity order = orderMapper.selectById(orderId);
        if (order == null || !user.getId().equals(order.getUserId())) {
            throw new BizException("订单不存在");
        }
        List<OrderItemEntity> items = itemsOf(orderId);
        if (items.isEmpty()) {
            throw new BizException("订单无商品");
        }
        cartService.clear();
        for (OrderItemEntity item : items) {
            CartAddRequest req = new CartAddRequest();
            req.setSkuId(item.getSkuId());
            req.setQuantity(item.getQuantity());
            try {
                cartService.add(req);
            } catch (BizException e) {
                throw new BizException("「" + item.getGoodsName() + "」无法加购：" + e.getMessage());
            }
        }
        return Map.of("shopId", order.getShopId(), "itemCount", items.size());
    }

    public List<MerchantOrderCardVO> listForMerchant(String status) {
        Long shopId = merchantShopSupport.requireShopId();
        LambdaQueryWrapper<OrderEntity> q = new LambdaQueryWrapper<OrderEntity>()
                .eq(OrderEntity::getShopId, shopId)
                .orderByDesc(OrderEntity::getId);
        if (StringUtils.hasText(status)) {
            q.eq(OrderEntity::getStatus, status);
        } else {
            q.in(OrderEntity::getStatus, OrderStatus.PAID, OrderStatus.ACCEPTED, OrderStatus.DELIVERING, OrderStatus.REFUNDING);
        }
        List<OrderEntity> orders = orderMapper.selectList(q);
        if (orders.isEmpty()) {
            return List.of();
        }
        List<Long> orderIds = orders.stream().map(OrderEntity::getId).toList();
        List<OrderItemEntity> allItems = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItemEntity>()
                .in(OrderItemEntity::getOrderId, orderIds));
        Map<Long, List<OrderItemEntity>> byOrder = allItems.stream()
                .collect(Collectors.groupingBy(OrderItemEntity::getOrderId));
        List<MerchantOrderCardVO> cards = new ArrayList<>();
        for (OrderEntity order : orders) {
            List<OrderItemEntity> items = byOrder.getOrDefault(order.getId(), List.of());
            int count = items.stream().mapToInt(i -> i.getQuantity() == null ? 0 : i.getQuantity()).sum();
            String summary = items.stream()
                    .limit(3)
                    .map(i -> i.getGoodsName() + "x" + i.getQuantity())
                    .collect(Collectors.joining("、"));
            if (items.size() > 3) {
                summary = summary + " 等";
            }
            cards.add(MerchantOrderCardVO.builder()
                    .order(order)
                    .itemCount(count)
                    .itemSummary(summary)
                    .build());
        }
        return cards;
    }

    public OrderDetailVO detailForMerchant(Long orderId) {
        OrderEntity order = requireMerchantOrder(orderId);
        List<OrderStatusLog> logs = orderStatusLogMapper.selectList(new LambdaQueryWrapper<OrderStatusLog>()
                .eq(OrderStatusLog::getOrderId, orderId)
                .orderByAsc(OrderStatusLog::getId));
        Map<String, Object> dispatch = fengNiaoDispatchService.toVo(orderId);
        return OrderDetailVO.builder()
                .order(order)
                .items(toItemVOs(itemsOf(orderId)))
                .logs(logs)
                .rider(riderInfoOf(order, dispatch))
                .dispatch(dispatch)
                .build();
    }

    @Transactional
    public OrderDetailVO accept(Long orderId) {
        AuthUser user = AuthContext.require();
        OrderEntity order = requireMerchantOrder(orderId);
        if (!OrderStatus.PAID.equals(order.getStatus())) {
            throw new BizException("仅已支付订单可接单");
        }
        String from = order.getStatus();
        order.setStatus(OrderStatus.ACCEPTED);
        order.setAcceptedAt(LocalDateTime.now());
        order.setAcceptDeadlineAt(null);
        if ("SELF".equalsIgnoreCase(order.getDeliveryType())) {
            order.setAutoCompleteAt(orderDeadlineSupport.autoCompleteFromNow());
        }
        orderMapper.updateById(order);
        logStatus(orderId, from, OrderStatus.ACCEPTED, Roles.MERCHANT, user.getId(), "商家接单");
        if (!"SELF".equalsIgnoreCase(order.getDeliveryType())) {
            fengNiaoDispatchService.dispatch(orderId);
        }
        return detailForMerchant(orderId);
    }

    @Transactional
    public OrderDetailVO redispatchFengNiao(Long orderId) {
        OrderEntity order = requireMerchantOrder(orderId);
        if (!OrderStatus.ACCEPTED.equals(order.getStatus()) && !OrderStatus.DELIVERING.equals(order.getStatus())) {
            throw new BizException("当前状态不可重新呼叫蜂鸟");
        }
        if ("SELF".equalsIgnoreCase(order.getDeliveryType())) {
            throw new BizException("商家自配无需呼叫蜂鸟");
        }
        fengNiaoDispatchService.dispatch(orderId);
        return detailForMerchant(orderId);
    }

    @Transactional
    public OrderDetailVO reject(Long orderId, String reason) {
        AuthUser user = AuthContext.require();
        OrderEntity order = requireMerchantOrder(orderId);
        if (!OrderStatus.PAID.equals(order.getStatus())) {
            throw new BizException("仅已支付订单可拒单");
        }
        restoreLines(itemsOf(orderId));
        String from = order.getStatus();
        order.setStatus(OrderStatus.REFUNDED);
        order.setCancelReason(StringUtils.hasText(reason) ? reason : "商家拒单");
        order.setAcceptDeadlineAt(null);
        order.setPayDeadlineAt(null);
        order.setAutoCompleteAt(null);
        orderMapper.updateById(order);
        logStatus(orderId, from, OrderStatus.REFUNDED, Roles.MERCHANT, user.getId(), order.getCancelReason());
        markRefund(orderId);
        couponService.restore(order.getUserCouponId());
        return detailForMerchant(orderId);
    }

    @Transactional
    public OrderDetailVO complete(Long orderId) {
        AuthUser user = AuthContext.require();
        OrderEntity order = requireMerchantOrder(orderId);
        if (!OrderStatus.ACCEPTED.equals(order.getStatus()) && !OrderStatus.DELIVERING.equals(order.getStatus())) {
            throw new BizException("当前状态不可完成");
        }
        String from = order.getStatus();
        order.setStatus(OrderStatus.COMPLETED);
        order.setCompletedAt(LocalDateTime.now());
        order.setDeliveredAt(LocalDateTime.now());
        order.setAutoCompleteAt(null);
        orderMapper.updateById(order);
        logStatus(orderId, from, OrderStatus.COMPLETED, Roles.MERCHANT, user.getId(), "商家确认送达");
        merchantSettleService.settleOrderIncome(order);
        return detailForMerchant(orderId);
    }

    public Page<OrderEntity> pageForAdmin(String status, String orderNo, Long shopId, long page, long size) {
        LambdaQueryWrapper<OrderEntity> q = new LambdaQueryWrapper<OrderEntity>().orderByDesc(OrderEntity::getId);
        if (StringUtils.hasText(status)) {
            q.eq(OrderEntity::getStatus, status);
        }
        if (StringUtils.hasText(orderNo)) {
            q.like(OrderEntity::getOrderNo, orderNo.trim());
        }
        if (shopId != null) {
            q.eq(OrderEntity::getShopId, shopId);
        }
        return orderMapper.selectPage(new Page<>(page, size), q);
    }

    public OrderDetailVO detailForAdmin(Long orderId) {
        OrderEntity order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException("订单不存在");
        }
        Shop shop = shopMapper.selectById(order.getShopId());
        Map<String, Object> shopMap = null;
        if (shop != null) {
            shopMap = new HashMap<>();
            shopMap.put("id", shop.getId());
            shopMap.put("name", shop.getName());
            shopMap.put("phone", shop.getPhone());
            shopMap.put("address", shop.getAddress());
        }
        Map<String, Object> dispatch = fengNiaoDispatchService.toVo(orderId);
        return OrderDetailVO.builder()
                .order(order)
                .items(toItemVOs(itemsOf(orderId)))
                .logs(logsOf(orderId))
                .shop(shopMap)
                .rider(riderInfoOf(order, dispatch))
                .dispatch(dispatch)
                .build();
    }

    /**
     * 客服强制取消：未完成订单可取消；已支付则标记退款并冲正入账。
     */
    @Transactional
    public OrderDetailVO adminForceCancel(Long orderId, String reason) {
        AuthUser admin = AuthContext.require();
        OrderEntity order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException("订单不存在");
        }
        String from = order.getStatus();
        if (OrderStatus.CANCELLED.equals(from)
                || OrderStatus.REFUNDED.equals(from)
                || OrderStatus.COMPLETED.equals(from)) {
            throw new BizException("当前状态不可强制取消");
        }
        restoreLines(itemsOf(orderId));
        boolean paidLike = !OrderStatus.PENDING_PAY.equals(from);
        order.setStatus(paidLike ? OrderStatus.REFUNDED : OrderStatus.CANCELLED);
        order.setCancelReason(StringUtils.hasText(reason) ? reason : "平台强制取消");
        order.setPayDeadlineAt(null);
        order.setAcceptDeadlineAt(null);
        order.setAutoCompleteAt(null);
        orderMapper.updateById(order);
        logStatus(orderId, from, order.getStatus(), Roles.ADMIN, admin.getId(), order.getCancelReason());
        couponService.restore(order.getUserCouponId());
        if (paidLike) {
            markRefund(orderId);
            merchantSettleService.reverseOrderIncome(order);
        } else {
            closePendingPayment(orderId);
        }
        fengNiaoDispatchService.cancelIfActive(orderId, order.getCancelReason());
        return detailForAdmin(orderId);
    }

    private Map<String, Object> riderInfoOf(OrderEntity order, Map<String, Object> dispatch) {
        if (order.getRiderId() != null) {
            RiderAccount rider = riderAccountMapper.selectById(order.getRiderId());
            if (rider != null) {
                Map<String, Object> riderInfo = new HashMap<>();
                riderInfo.put("id", rider.getId());
                riderInfo.put("name", rider.getName());
                riderInfo.put("phone", rider.getPhone());
                riderInfo.put("lat", rider.getLat());
                riderInfo.put("lng", rider.getLng());
                riderInfo.put("locationAt", rider.getLocationAt());
                riderInfo.put("channel", "SELF_RIDER");
                return riderInfo;
            }
        }
        if (dispatch != null && dispatch.get("riderPhone") != null
                && StringUtils.hasText(String.valueOf(dispatch.get("riderPhone")))) {
            Map<String, Object> riderInfo = new HashMap<>();
            riderInfo.put("name", dispatch.get("riderName"));
            riderInfo.put("phone", dispatch.get("riderPhone"));
            riderInfo.put("channel", "FENG_NIAO");
            return riderInfo;
        }
        return null;
    }

    private OrderEntity requireMerchantOrder(Long orderId) {
        Long shopId = merchantShopSupport.requireShopId();
        OrderEntity order = orderMapper.selectById(orderId);
        if (order == null || !shopId.equals(order.getShopId())) {
            throw new BizException("订单不存在");
        }
        return order;
    }

    private List<OrderItemEntity> itemsOf(Long orderId) {
        return orderItemMapper.selectList(new LambdaQueryWrapper<OrderItemEntity>()
                .eq(OrderItemEntity::getOrderId, orderId));
    }

    private List<OrderItemVO> toItemVOs(List<OrderItemEntity> items) {
        List<OrderItemVO> list = new ArrayList<>();
        for (OrderItemEntity it : items) {
            String cover = "";
            if (it.getGoodsId() != null) {
                Goods g = goodsMapper.selectById(it.getGoodsId());
                if (g != null && g.getCoverUrl() != null && !g.getCoverUrl().isBlank()) {
                    cover = g.getCoverUrl();
                } else {
                    cover = "https://picsum.photos/seed/g" + it.getGoodsId() + "/400";
                }
            }
            list.add(OrderItemVO.builder()
                    .id(it.getId())
                    .orderId(it.getOrderId())
                    .goodsId(it.getGoodsId())
                    .skuId(it.getSkuId())
                    .goodsName(it.getGoodsName())
                    .skuName(it.getSkuName())
                    .price(it.getPrice())
                    .quantity(it.getQuantity())
                    .coverUrl(cover)
                    .build());
        }
        return list;
    }

    private List<OrderStatusLog> logsOf(Long orderId) {
        return orderStatusLogMapper.selectList(new LambdaQueryWrapper<OrderStatusLog>()
                .eq(OrderStatusLog::getOrderId, orderId)
                .orderByAsc(OrderStatusLog::getId));
    }

    private void restoreLines(List<OrderItemEntity> lines) {
        for (OrderItemEntity line : lines) {
            goodsSkuStockMapper.restore(line.getSkuId(), line.getQuantity());
        }
    }

    private void markRefund(Long orderId) {
        PaymentRecord pay = paymentRecordMapper.selectOne(new LambdaQueryWrapper<PaymentRecord>()
                .eq(PaymentRecord::getOrderId, orderId)
                .orderByDesc(PaymentRecord::getId)
                .last("limit 1"));
        if (pay != null) {
            pay.setStatus("REFUND");
            paymentRecordMapper.updateById(pay);
        }
    }

    private void closePendingPayment(Long orderId) {
        PaymentRecord pay = paymentRecordMapper.selectOne(new LambdaQueryWrapper<PaymentRecord>()
                .eq(PaymentRecord::getOrderId, orderId)
                .orderByDesc(PaymentRecord::getId)
                .last("limit 1"));
        if (pay != null && "PENDING".equals(pay.getStatus())) {
            pay.setStatus("CLOSED");
            paymentRecordMapper.updateById(pay);
        }
    }

    private void logStatus(Long orderId, String from, String to, String opType, Long opId, String remark) {
        OrderStatusLog log = new OrderStatusLog();
        log.setOrderId(orderId);
        log.setFromStatus(from == null ? "" : from);
        log.setToStatus(to);
        log.setOperatorType(opType);
        log.setOperatorId(opId);
        log.setRemark(remark == null ? "" : remark);
        orderStatusLogMapper.insert(log);
    }

    private String nextOrderNo() {
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int r = ThreadLocalRandom.current().nextInt(1000, 9999);
        return "O" + ts + r;
    }

    private String toAddressJson(UserAddress address) {
        Map<String, Object> map = new HashMap<>();
        map.put("contactName", address.getContactName());
        map.put("contactPhone", address.getContactPhone());
        map.put("detail", address.getDetail());
        map.put("lat", address.getLat());
        map.put("lng", address.getLng());
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            throw new BizException("地址序列化失败");
        }
    }
}
