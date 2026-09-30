package com.qs.takeout.modules.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qs.takeout.common.auth.RequireRole;
import com.qs.takeout.common.auth.Roles;
import com.qs.takeout.common.result.ApiResult;
import com.qs.takeout.modules.admin.dto.AdminOrderCancelRequest;
import com.qs.takeout.modules.order.OrderService;
import com.qs.takeout.modules.order.dto.OrderDetailVO;
import com.qs.takeout.modules.order.entity.OrderEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/orders")
@RequireRole({Roles.ADMIN})
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderService orderService;

    @GetMapping
    public ApiResult<Page<OrderEntity>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) Long shopId,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size) {
        return ApiResult.ok(orderService.pageForAdmin(status, orderNo, shopId, page, size));
    }

    @GetMapping("/{id}")
    public ApiResult<OrderDetailVO> detail(@PathVariable Long id) {
        return ApiResult.ok(orderService.detailForAdmin(id));
    }

    @PostMapping("/{id}/cancel")
    public ApiResult<OrderDetailVO> cancel(
            @PathVariable Long id,
            @RequestBody(required = false) AdminOrderCancelRequest request) {
        String reason = request == null ? null : request.getReason();
        return ApiResult.ok(orderService.adminForceCancel(id, reason));
    }
}
