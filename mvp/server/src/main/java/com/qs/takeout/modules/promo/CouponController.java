package com.qs.takeout.modules.promo;

import com.qs.takeout.common.auth.RequireRole;
import com.qs.takeout.common.auth.Roles;
import com.qs.takeout.common.result.ApiResult;
import com.qs.takeout.modules.promo.dto.UserCouponVO;
import com.qs.takeout.modules.promo.entity.Coupon;
import com.qs.takeout.modules.promo.entity.UserCoupon;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @GetMapping("/available")
    public ApiResult<List<Coupon>> available(@RequestParam(required = false) Long shopId) {
        return ApiResult.ok(couponService.available(shopId));
    }

    @PostMapping("/{id}/claim")
    @RequireRole({Roles.USER})
    public ApiResult<UserCoupon> claim(@PathVariable Long id) {
        return ApiResult.ok(couponService.claim(id));
    }

    @GetMapping("/mine")
    @RequireRole({Roles.USER})
    public ApiResult<List<UserCouponVO>> mine(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long shopId,
            @RequestParam(required = false) BigDecimal goodsAmount) {
        return ApiResult.ok(couponService.mine(status, shopId, goodsAmount));
    }
}
