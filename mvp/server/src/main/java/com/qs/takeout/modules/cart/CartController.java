package com.qs.takeout.modules.cart;

import com.qs.takeout.common.auth.RequireRole;
import com.qs.takeout.common.auth.Roles;
import com.qs.takeout.common.result.ApiResult;
import com.qs.takeout.modules.cart.dto.CartAddRequest;
import com.qs.takeout.modules.cart.dto.CartUpdateRequest;
import com.qs.takeout.modules.cart.dto.CartViewVO;
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

@RestController
@RequestMapping("/api/cart")
@RequireRole({Roles.USER})
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ApiResult<CartViewVO> view() {
        return ApiResult.ok(cartService.view());
    }

    @PostMapping("/items")
    public ApiResult<CartViewVO> add(@Valid @RequestBody CartAddRequest request) {
        return ApiResult.ok(cartService.add(request));
    }

    @PutMapping("/items/{id}")
    public ApiResult<CartViewVO> update(@PathVariable Long id, @Valid @RequestBody CartUpdateRequest request) {
        return ApiResult.ok(cartService.update(id, request));
    }

    @DeleteMapping("/items/{id}")
    public ApiResult<CartViewVO> remove(@PathVariable Long id) {
        return ApiResult.ok(cartService.remove(id));
    }

    @DeleteMapping
    public ApiResult<Void> clear() {
        cartService.clear();
        return ApiResult.ok();
    }
}
