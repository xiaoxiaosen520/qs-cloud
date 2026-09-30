package com.qs.takeout.modules.shop;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qs.takeout.modules.shop.dto.NearbyShopVO;
import com.qs.takeout.modules.shop.dto.ShopUpdateRequest;
import com.qs.takeout.modules.shop.entity.Shop;
import com.qs.takeout.modules.shop.mapper.ShopMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ShopQueryService {

    private static final double MAX_KM = 10.0;

    private final ShopMapper shopMapper;
    private final MerchantShopSupport merchantShopSupport;

    public Shop merchantShop() {
        return merchantShopSupport.requireShop();
    }

    public Shop updateMerchantShop(ShopUpdateRequest req) {
        Shop shop = merchantShopSupport.requireShop();
        if (req.getNotice() != null) {
            shop.setNotice(req.getNotice());
        }
        if (StringUtils.hasText(req.getBusinessHours())) {
            shop.setBusinessHours(req.getBusinessHours());
        }
        if (req.getLogoUrl() != null) {
            shop.setLogoUrl(req.getLogoUrl().trim());
        }
        if (req.getMinOrderAmount() != null) {
            shop.setMinOrderAmount(req.getMinOrderAmount());
        }
        if (req.getDeliveryFee() != null) {
            shop.setDeliveryFee(req.getDeliveryFee());
        }
        if (req.getPackingFee() != null) {
            shop.setPackingFee(req.getPackingFee());
        }
        shop.setOpenStatus(req.getOpenStatus());
        shopMapper.updateById(shop);
        return shop;
    }

    public List<NearbyShopVO> nearby(double lat, double lng, String type) {
        LambdaQueryWrapper<Shop> q = new LambdaQueryWrapper<Shop>()
                .eq(Shop::getStatus, 1)
                .eq(Shop::getOpenStatus, 1);
        if (StringUtils.hasText(type)) {
            q.eq(Shop::getShopType, type.trim().toUpperCase());
        }
        List<Shop> shops = shopMapper.selectList(q);
        return shops.stream()
                .map(shop -> {
                    double meters = haversineMeters(lat, lng,
                            shop.getLat().doubleValue(), shop.getLng().doubleValue());
                    return NearbyShopVO.builder()
                            .shop(shop)
                            .distanceMeters((int) Math.round(meters))
                            .build();
                })
                .filter(vo -> vo.getDistanceMeters() <= MAX_KM * 1000)
                .sorted(Comparator.comparingInt(NearbyShopVO::getDistanceMeters))
                .limit(50)
                .toList();
    }

    public Shop detail(Long id) {
        Shop shop = shopMapper.selectById(id);
        if (shop == null || shop.getStatus() == null || shop.getStatus() == 0) {
            throw new com.qs.takeout.common.exception.BizException("店铺不存在");
        }
        return shop;
    }

    /** Haversine 距离（米） */
    static double haversineMeters(double lat1, double lng1, double lat2, double lng2) {
        double r = 6371000;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 2 * r * Math.asin(Math.sqrt(a));
    }
}
