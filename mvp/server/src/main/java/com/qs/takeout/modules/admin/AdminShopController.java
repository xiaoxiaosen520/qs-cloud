package com.qs.takeout.modules.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qs.takeout.common.auth.RequireRole;
import com.qs.takeout.common.auth.Roles;
import com.qs.takeout.common.result.ApiResult;
import com.qs.takeout.modules.admin.dto.AdminShopUpdateRequest;
import com.qs.takeout.modules.admin.dto.DashboardVO;
import com.qs.takeout.modules.shop.entity.Shop;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequireRole({Roles.ADMIN})
@RequiredArgsConstructor
public class AdminShopController {

    private final AdminPlatformService platformService;

    @GetMapping("/dashboard")
    public ApiResult<DashboardVO> dashboard() {
        return ApiResult.ok(platformService.dashboard());
    }

    @GetMapping("/shops")
    public ApiResult<Page<Shop>> shops(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String shopType,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size) {
        return ApiResult.ok(platformService.pageShops(keyword, shopType, status, page, size));
    }

    @GetMapping("/shops/{id}")
    public ApiResult<Shop> shop(@PathVariable Long id) {
        return ApiResult.ok(platformService.getShop(id));
    }

    @PutMapping("/shops/{id}")
    public ApiResult<Shop> updateShop(@PathVariable Long id, @RequestBody AdminShopUpdateRequest request) {
        return ApiResult.ok(platformService.updateShop(id, request));
    }
}
