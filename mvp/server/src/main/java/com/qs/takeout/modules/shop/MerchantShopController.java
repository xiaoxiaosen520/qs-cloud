package com.qs.takeout.modules.shop;

import com.qs.takeout.common.auth.RequireRole;
import com.qs.takeout.common.auth.Roles;
import com.qs.takeout.common.result.ApiResult;
import com.qs.takeout.modules.shop.dto.ShopUpdateRequest;
import com.qs.takeout.modules.shop.entity.Shop;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/merchant")
@RequireRole({Roles.MERCHANT})
@RequiredArgsConstructor
public class MerchantShopController {

    private final ShopQueryService shopQueryService;

    @GetMapping("/shop")
    public ApiResult<Shop> myShop() {
        return ApiResult.ok(shopQueryService.merchantShop());
    }

    @PutMapping("/shop")
    public ApiResult<Shop> update(@Valid @RequestBody ShopUpdateRequest request) {
        return ApiResult.ok(shopQueryService.updateMerchantShop(request));
    }
}
