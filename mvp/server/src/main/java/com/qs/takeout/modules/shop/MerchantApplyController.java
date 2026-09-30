package com.qs.takeout.modules.shop;

import com.qs.takeout.common.auth.RequireRole;
import com.qs.takeout.common.auth.Roles;
import com.qs.takeout.common.result.ApiResult;
import com.qs.takeout.modules.shop.dto.ShopApplyRequest;
import com.qs.takeout.modules.shop.entity.ShopApply;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/merchant")
@RequireRole({Roles.MERCHANT})
@RequiredArgsConstructor
public class MerchantApplyController {

    private final ShopApplyService shopApplyService;

    @PostMapping("/apply")
    public ApiResult<ShopApply> apply(@Valid @RequestBody ShopApplyRequest request) {
        return ApiResult.ok(shopApplyService.submit(request));
    }

    @GetMapping("/apply/status")
    public ApiResult<ShopApply> status() {
        return ApiResult.ok(shopApplyService.latestStatus());
    }
}
