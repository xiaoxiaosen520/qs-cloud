package com.qs.takeout.modules.admin;

import com.qs.takeout.common.auth.RequireRole;
import com.qs.takeout.common.auth.Roles;
import com.qs.takeout.common.result.ApiResult;
import com.qs.takeout.modules.admin.dto.BannerSaveRequest;
import com.qs.takeout.modules.admin.dto.CategorySaveRequest;
import com.qs.takeout.modules.admin.dto.ConfigUpdateRequest;
import com.qs.takeout.modules.finance.entity.SysConfig;
import com.qs.takeout.modules.shop.entity.Banner;
import com.qs.takeout.modules.shop.entity.PlatformCategory;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequireRole({Roles.ADMIN})
@RequiredArgsConstructor
public class AdminCatalogController {

    private final AdminPlatformService platformService;

    @GetMapping("/categories")
    public ApiResult<List<PlatformCategory>> categories() {
        return ApiResult.ok(platformService.listCategories());
    }

    @PostMapping("/categories")
    public ApiResult<PlatformCategory> createCategory(@Valid @RequestBody CategorySaveRequest request) {
        return ApiResult.ok(platformService.createCategory(request));
    }

    @PutMapping("/categories/{id}")
    public ApiResult<PlatformCategory> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CategorySaveRequest request) {
        return ApiResult.ok(platformService.updateCategory(id, request));
    }

    @DeleteMapping("/categories/{id}")
    public ApiResult<Void> deleteCategory(@PathVariable Long id) {
        platformService.deleteCategory(id);
        return ApiResult.ok();
    }

    @GetMapping("/banners")
    public ApiResult<List<Banner>> banners() {
        return ApiResult.ok(platformService.listBanners());
    }

    @PostMapping("/banners")
    public ApiResult<Banner> createBanner(@Valid @RequestBody BannerSaveRequest request) {
        return ApiResult.ok(platformService.createBanner(request));
    }

    @PutMapping("/banners/{id}")
    public ApiResult<Banner> updateBanner(
            @PathVariable Long id,
            @Valid @RequestBody BannerSaveRequest request) {
        return ApiResult.ok(platformService.updateBanner(id, request));
    }

    @DeleteMapping("/banners/{id}")
    public ApiResult<Void> deleteBanner(@PathVariable Long id) {
        platformService.deleteBanner(id);
        return ApiResult.ok();
    }

    @GetMapping("/configs")
    public ApiResult<List<SysConfig>> configs() {
        return ApiResult.ok(platformService.listConfigs());
    }

    @PutMapping("/configs")
    public ApiResult<List<SysConfig>> updateConfigs(@Valid @RequestBody ConfigUpdateRequest request) {
        return ApiResult.ok(platformService.updateConfigs(request.getConfigs()));
    }
}
