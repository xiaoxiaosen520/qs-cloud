package com.qs.takeout.modules.order;

import com.qs.takeout.common.auth.RequireRole;
import com.qs.takeout.common.auth.Roles;
import com.qs.takeout.common.result.ApiResult;
import com.qs.takeout.modules.order.dto.MerchantOrderCardVO;
import com.qs.takeout.modules.order.dto.OrderDetailVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/merchant/orders")
@RequireRole({Roles.MERCHANT})
@RequiredArgsConstructor
public class MerchantOrderController {

    private final OrderService orderService;

    @GetMapping
    public ApiResult<List<MerchantOrderCardVO>> list(@RequestParam(required = false) String status) {
        return ApiResult.ok(orderService.listForMerchant(status));
    }

    @GetMapping("/{id}")
    public ApiResult<OrderDetailVO> detail(@PathVariable Long id) {
        return ApiResult.ok(orderService.detailForMerchant(id));
    }

    @PostMapping("/{id}/accept")
    public ApiResult<OrderDetailVO> accept(@PathVariable Long id) {
        return ApiResult.ok(orderService.accept(id));
    }

    @PostMapping("/{id}/fengniao/dispatch")
    public ApiResult<OrderDetailVO> redispatch(@PathVariable Long id) {
        return ApiResult.ok(orderService.redispatchFengNiao(id));
    }

    @PostMapping("/{id}/reject")
    public ApiResult<OrderDetailVO> reject(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = body == null ? null : body.get("reason");
        return ApiResult.ok(orderService.reject(id, reason));
    }

    @PostMapping("/{id}/complete")
    public ApiResult<OrderDetailVO> complete(@PathVariable Long id) {
        return ApiResult.ok(orderService.complete(id));
    }

    @PostMapping("/{id}/refund/approve")
    public ApiResult<OrderDetailVO> approveRefund(@PathVariable Long id) {
        return ApiResult.ok(orderService.approveRefund(id));
    }

    @PostMapping("/{id}/refund/reject")
    public ApiResult<OrderDetailVO> rejectRefund(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = body == null ? null : body.get("reason");
        return ApiResult.ok(orderService.rejectRefund(id, reason));
    }
}
