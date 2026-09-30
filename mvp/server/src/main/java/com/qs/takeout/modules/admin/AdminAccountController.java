package com.qs.takeout.modules.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qs.takeout.common.auth.RequireRole;
import com.qs.takeout.common.auth.Roles;
import com.qs.takeout.common.result.ApiResult;
import com.qs.takeout.modules.admin.dto.StatusUpdateRequest;
import com.qs.takeout.modules.auth.entity.MerchantAccount;
import com.qs.takeout.modules.auth.entity.RiderAccount;
import com.qs.takeout.modules.auth.entity.UserAccount;
import jakarta.validation.Valid;
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
public class AdminAccountController {

    private final AdminPlatformService platformService;

    @GetMapping("/users")
    public ApiResult<Page<UserAccount>> users(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size) {
        return ApiResult.ok(platformService.pageUsers(keyword, status, page, size));
    }

    @PutMapping("/users/{id}/status")
    public ApiResult<UserAccount> updateUserStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusUpdateRequest request) {
        return ApiResult.ok(platformService.updateUserStatus(id, request.getStatus()));
    }

    @GetMapping("/merchants")
    public ApiResult<Page<MerchantAccount>> merchants(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size) {
        return ApiResult.ok(platformService.pageMerchants(keyword, status, page, size));
    }

    @PutMapping("/merchants/{id}/status")
    public ApiResult<MerchantAccount> updateMerchantStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusUpdateRequest request) {
        return ApiResult.ok(platformService.updateMerchantStatus(id, request.getStatus()));
    }

    @GetMapping("/riders")
    public ApiResult<Page<RiderAccount>> riders(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size) {
        return ApiResult.ok(platformService.pageRiders(keyword, status, page, size));
    }

    @PutMapping("/riders/{id}/status")
    public ApiResult<RiderAccount> updateRiderStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusUpdateRequest request) {
        return ApiResult.ok(platformService.updateRiderStatus(id, request.getStatus()));
    }
}
