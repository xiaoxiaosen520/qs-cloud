package com.qs.takeout.modules.user;

import com.qs.takeout.common.auth.RequireRole;
import com.qs.takeout.common.auth.Roles;
import com.qs.takeout.common.result.ApiResult;
import com.qs.takeout.modules.shop.entity.Shop;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user/favorites")
@RequireRole({Roles.USER})
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    @GetMapping
    public ApiResult<List<Shop>> list() {
        return ApiResult.ok(favoriteService.list());
    }

    @GetMapping("/ids")
    public ApiResult<List<Long>> ids() {
        return ApiResult.ok(favoriteService.ids());
    }

    @PostMapping("/{shopId}/toggle")
    public ApiResult<Map<String, Object>> toggle(@PathVariable Long shopId) {
        return ApiResult.ok(favoriteService.toggle(shopId));
    }
}
