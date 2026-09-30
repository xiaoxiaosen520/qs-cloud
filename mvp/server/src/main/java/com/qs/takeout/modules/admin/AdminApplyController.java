package com.qs.takeout.modules.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qs.takeout.common.auth.RequireRole;
import com.qs.takeout.common.auth.Roles;
import com.qs.takeout.common.result.ApiResult;
import com.qs.takeout.modules.shop.ShopApplyService;
import com.qs.takeout.modules.shop.dto.ShopApplyReviewRequest;
import com.qs.takeout.modules.shop.entity.ShopApply;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequireRole({Roles.ADMIN})
@RequiredArgsConstructor
public class AdminApplyController {

    private final ShopApplyService shopApplyService;

    @GetMapping("/applies")
    public ApiResult<Page<ShopApply>> list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size) {
        return ApiResult.ok(shopApplyService.pageForAdmin(status, page, size));
    }

    @PostMapping("/applies/{id}/review")
    public ApiResult<ShopApply> review(
            @PathVariable Long id,
            @Valid @RequestBody ShopApplyReviewRequest request) {
        return ApiResult.ok(shopApplyService.review(id, request));
    }
}
