package com.qs.takeout.modules.order;

import com.qs.takeout.common.auth.RequireRole;
import com.qs.takeout.common.auth.Roles;
import com.qs.takeout.common.result.ApiResult;
import com.qs.takeout.modules.order.dto.CreateOrderRequest;
import com.qs.takeout.modules.order.dto.OrderDetailVO;
import com.qs.takeout.modules.order.dto.ReviewRequest;
import com.qs.takeout.modules.order.dto.UserOrderCardVO;
import com.qs.takeout.modules.order.entity.OrderReview;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@RequireRole({Roles.USER})
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final ReviewService reviewService;

    @PostMapping
    public ApiResult<OrderDetailVO> create(@Valid @RequestBody CreateOrderRequest request) {
        return ApiResult.ok(orderService.create(request));
    }

    @GetMapping
    public ApiResult<List<UserOrderCardVO>> list() {
        return ApiResult.ok(orderService.listMine());
    }

    @GetMapping("/{id}")
    public ApiResult<OrderDetailVO> detail(@PathVariable Long id) {
        return ApiResult.ok(orderService.detailForUser(id));
    }

    @PostMapping("/{id}/mock-pay")
    public ApiResult<OrderDetailVO> mockPay(@PathVariable Long id) {
        return ApiResult.ok(orderService.mockPay(id));
    }

    @PostMapping("/{id}/cancel")
    public ApiResult<OrderDetailVO> cancel(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = body == null ? null : body.get("reason");
        return ApiResult.ok(orderService.cancel(id, reason));
    }

    @PostMapping("/{id}/refund")
    public ApiResult<OrderDetailVO> refund(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = body == null ? null : body.get("reason");
        return ApiResult.ok(orderService.applyRefund(id, reason));
    }

    @PostMapping("/{id}/review")
    public ApiResult<OrderReview> review(
            @PathVariable Long id,
            @Valid @RequestBody ReviewRequest request) {
        return ApiResult.ok(reviewService.create(id, request));
    }

    @PostMapping("/{id}/reorder")
    public ApiResult<Map<String, Object>> reorder(@PathVariable Long id) {
        return ApiResult.ok(orderService.reorder(id));
    }
}
