package com.qs.takeout.modules.admin;

import com.qs.takeout.common.auth.RequireRole;
import com.qs.takeout.common.auth.Roles;
import com.qs.takeout.common.result.ApiResult;
import com.qs.takeout.modules.finance.MerchantFinanceService;
import com.qs.takeout.modules.finance.RiderFinanceService;
import com.qs.takeout.modules.finance.dto.WithdrawReviewRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/withdraws")
@RequireRole({Roles.ADMIN})
@RequiredArgsConstructor
public class AdminWithdrawController {

    private final MerchantFinanceService merchantFinanceService;
    private final RiderFinanceService riderFinanceService;

    @GetMapping
    public ApiResult<?> list(@RequestParam(required = false) String status,
                             @RequestParam(required = false, defaultValue = "MERCHANT") String role) {
        if ("RIDER".equalsIgnoreCase(role)) {
            return ApiResult.ok(riderFinanceService.adminListWithdraws(status));
        }
        return ApiResult.ok(merchantFinanceService.adminListWithdraws(status));
    }

    @PostMapping("/{id}/review")
    public ApiResult<?> review(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "MERCHANT") String role,
            @RequestBody WithdrawReviewRequest request) {
        if ("RIDER".equalsIgnoreCase(role)) {
            return ApiResult.ok(riderFinanceService.reviewWithdraw(id, request));
        }
        return ApiResult.ok(merchantFinanceService.reviewWithdraw(id, request));
    }
}
