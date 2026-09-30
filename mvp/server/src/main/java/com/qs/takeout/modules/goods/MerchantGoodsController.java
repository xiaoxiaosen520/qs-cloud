package com.qs.takeout.modules.goods;

import com.qs.takeout.common.auth.RequireRole;
import com.qs.takeout.common.auth.Roles;
import com.qs.takeout.common.result.ApiResult;
import com.qs.takeout.modules.goods.dto.CategorySaveRequest;
import com.qs.takeout.modules.goods.dto.GoodsDetailVO;
import com.qs.takeout.modules.goods.dto.GoodsSaveRequest;
import com.qs.takeout.modules.goods.dto.MerchantGoodsItemVO;
import com.qs.takeout.modules.goods.entity.ShopCategory;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/merchant")
@RequireRole({Roles.MERCHANT})
@RequiredArgsConstructor
public class MerchantGoodsController {

    private final MerchantGoodsService merchantGoodsService;

    @GetMapping("/categories")
    public ApiResult<List<ShopCategory>> categories() {
        return ApiResult.ok(merchantGoodsService.listCategories());
    }

    @PostMapping("/categories")
    public ApiResult<ShopCategory> createCategory(@Valid @RequestBody CategorySaveRequest request) {
        return ApiResult.ok(merchantGoodsService.createCategory(request));
    }

    @PutMapping("/categories/{id}")
    public ApiResult<ShopCategory> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CategorySaveRequest request) {
        return ApiResult.ok(merchantGoodsService.updateCategory(id, request));
    }

    @DeleteMapping("/categories/{id}")
    public ApiResult<Void> deleteCategory(@PathVariable Long id) {
        merchantGoodsService.deleteCategory(id);
        return ApiResult.ok();
    }

    @GetMapping("/goods")
    public ApiResult<List<MerchantGoodsItemVO>> goods(@RequestParam(required = false) Long categoryId) {
        return ApiResult.ok(merchantGoodsService.listGoods(categoryId));
    }

    @GetMapping("/goods/{id}")
    public ApiResult<GoodsDetailVO> goodsDetail(@PathVariable Long id) {
        return ApiResult.ok(merchantGoodsService.goodsDetail(id));
    }

    @PostMapping("/goods")
    public ApiResult<GoodsDetailVO> createGoods(@Valid @RequestBody GoodsSaveRequest request) {
        return ApiResult.ok(merchantGoodsService.createGoods(request));
    }

    @PutMapping("/goods/{id}")
    public ApiResult<GoodsDetailVO> updateGoods(
            @PathVariable Long id,
            @Valid @RequestBody GoodsSaveRequest request) {
        return ApiResult.ok(merchantGoodsService.updateGoods(id, request));
    }

    @PostMapping("/goods/{id}/offline")
    public ApiResult<Void> offline(@PathVariable Long id) {
        merchantGoodsService.offlineGoods(id);
        return ApiResult.ok();
    }

    @PostMapping("/goods/{id}/online")
    public ApiResult<Void> online(@PathVariable Long id) {
        merchantGoodsService.onlineGoods(id);
        return ApiResult.ok();
    }
}
