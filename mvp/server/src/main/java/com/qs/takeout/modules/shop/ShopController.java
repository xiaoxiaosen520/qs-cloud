package com.qs.takeout.modules.shop;

import com.qs.takeout.common.result.ApiResult;
import com.qs.takeout.modules.goods.UserGoodsService;
import com.qs.takeout.modules.goods.dto.GoodsDetailVO;
import com.qs.takeout.modules.goods.entity.Goods;
import com.qs.takeout.modules.goods.entity.ShopCategory;
import com.qs.takeout.modules.order.ReviewService;
import com.qs.takeout.modules.order.entity.OrderReview;
import com.qs.takeout.modules.shop.dto.NearbyShopVO;
import com.qs.takeout.modules.shop.entity.Shop;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/shops")
@RequiredArgsConstructor
public class ShopController {

    private final ShopQueryService shopQueryService;
    private final UserGoodsService userGoodsService;
    private final ReviewService reviewService;

    @GetMapping("/nearby")
    public ApiResult<List<NearbyShopVO>> nearby(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(required = false) String type) {
        return ApiResult.ok(shopQueryService.nearby(lat, lng, type));
    }

    @GetMapping("/{id}")
    public ApiResult<Shop> detail(@PathVariable Long id) {
        return ApiResult.ok(shopQueryService.detail(id));
    }

    @GetMapping("/{id}/reviews")
    public ApiResult<List<OrderReview>> reviews(
            @PathVariable Long id,
            @RequestParam(defaultValue = "20") int limit) {
        return ApiResult.ok(reviewService.listByShop(id, limit));
    }

    @GetMapping("/{id}/categories")
    public ApiResult<List<ShopCategory>> categories(@PathVariable Long id) {
        return ApiResult.ok(userGoodsService.categories(id));
    }

    @GetMapping("/{id}/goods")
    public ApiResult<List<Goods>> goods(
            @PathVariable Long id,
            @RequestParam(required = false) Long categoryId) {
        return ApiResult.ok(userGoodsService.goodsList(id, categoryId));
    }

    @GetMapping("/goods/{goodsId}")
    public ApiResult<GoodsDetailVO> goodsDetail(@PathVariable Long goodsId) {
        return ApiResult.ok(userGoodsService.goodsDetail(goodsId));
    }
}
