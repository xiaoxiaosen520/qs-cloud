package com.qs.takeout.modules.order;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qs.takeout.common.auth.AuthContext;
import com.qs.takeout.common.auth.AuthUser;
import com.qs.takeout.common.exception.BizException;
import com.qs.takeout.modules.order.dto.ReviewRequest;
import com.qs.takeout.modules.order.entity.OrderEntity;
import com.qs.takeout.modules.order.entity.OrderReview;
import com.qs.takeout.modules.order.mapper.OrderMapper;
import com.qs.takeout.modules.order.mapper.OrderReviewMapper;
import com.qs.takeout.modules.shop.entity.Shop;
import com.qs.takeout.modules.shop.mapper.ShopMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private static final int MAX_IMAGES = 9;

    private final OrderReviewMapper orderReviewMapper;
    private final OrderMapper orderMapper;
    private final ShopMapper shopMapper;
    private final ObjectMapper objectMapper;

    @Transactional
    public OrderReview create(Long orderId, ReviewRequest req) {
        AuthUser user = AuthContext.require();
        OrderEntity order = orderMapper.selectById(orderId);
        if (order == null || !user.getId().equals(order.getUserId())) {
            throw new BizException("订单不存在");
        }
        if (!OrderStatus.COMPLETED.equals(order.getStatus())) {
            throw new BizException("仅已完成订单可评价");
        }
        Long exists = orderReviewMapper.selectCount(new LambdaQueryWrapper<OrderReview>()
                .eq(OrderReview::getOrderId, orderId));
        if (exists > 0) {
            throw new BizException("订单已评价");
        }
        List<String> images = sanitizeImages(req.getImageUrls());
        OrderReview review = new OrderReview();
        review.setOrderId(orderId);
        review.setUserId(user.getId());
        review.setShopId(order.getShopId());
        review.setScore(req.getScore());
        review.setContent(StringUtils.hasText(req.getContent()) ? req.getContent().trim() : "");
        review.setImageUrlsRaw(writeImages(images));
        review.setImageUrls(images);
        orderReviewMapper.insert(review);
        refreshShopScore(order.getShopId());
        return hydrate(review);
    }

    public OrderReview ofOrder(Long orderId) {
        return hydrate(orderReviewMapper.selectOne(new LambdaQueryWrapper<OrderReview>()
                .eq(OrderReview::getOrderId, orderId)
                .last("limit 1")));
    }

    public List<OrderReview> listByShop(Long shopId, int limit) {
        int size = Math.min(Math.max(limit, 1), 50);
        List<OrderReview> list = orderReviewMapper.selectList(new LambdaQueryWrapper<OrderReview>()
                .eq(OrderReview::getShopId, shopId)
                .orderByDesc(OrderReview::getId)
                .last("limit " + size));
        list.forEach(this::hydrate);
        return list;
    }

    private void refreshShopScore(Long shopId) {
        List<OrderReview> all = orderReviewMapper.selectList(new LambdaQueryWrapper<OrderReview>()
                .eq(OrderReview::getShopId, shopId));
        if (all.isEmpty()) return;
        double avg = all.stream().mapToInt(OrderReview::getScore).average().orElse(5.0);
        Shop shop = shopMapper.selectById(shopId);
        if (shop == null) return;
        shop.setScore(BigDecimal.valueOf(avg).setScale(1, RoundingMode.HALF_UP));
        shopMapper.updateById(shop);
    }

    public List<String> parseImages(String raw) {
        if (!StringUtils.hasText(raw)) {
            return List.of();
        }
        try {
            List<String> list = objectMapper.readValue(raw, new TypeReference<List<String>>() { });
            return list == null ? List.of() : list.stream().filter(StringUtils::hasText).toList();
        } catch (Exception e) {
            return List.of();
        }
    }

    private OrderReview hydrate(OrderReview review) {
        if (review == null) {
            return null;
        }
        review.setImageUrls(parseImages(review.getImageUrlsRaw()));
        return review;
    }

    private List<String> sanitizeImages(List<String> input) {
        if (input == null || input.isEmpty()) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (String raw : input) {
            if (!StringUtils.hasText(raw)) {
                continue;
            }
            String url = raw.trim();
            if (!(url.startsWith("/uploads/") || url.startsWith("http://") || url.startsWith("https://"))) {
                throw new BizException("评价图片地址无效");
            }
            out.add(url);
            if (out.size() >= MAX_IMAGES) {
                break;
            }
        }
        return out;
    }

    private String writeImages(List<String> images) {
        if (images == null || images.isEmpty()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(images);
        } catch (Exception e) {
            return "[]";
        }
    }
}
