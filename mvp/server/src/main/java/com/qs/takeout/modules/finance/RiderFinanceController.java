package com.qs.takeout.modules.finance;

import com.qs.takeout.common.auth.RequireRole;
import com.qs.takeout.common.auth.Roles;
import com.qs.takeout.common.result.ApiResult;
import com.qs.takeout.modules.finance.dto.RiderWalletVO;
import com.qs.takeout.modules.finance.dto.SettlementUpdateRequest;
import com.qs.takeout.modules.finance.dto.WithdrawApplyRequest;
import com.qs.takeout.modules.finance.entity.RiderBillingRecord;
import com.qs.takeout.modules.finance.entity.RiderWithdrawRecord;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/rider")
@RequireRole({Roles.RIDER})
@RequiredArgsConstructor
public class RiderFinanceController {

    private final RiderFinanceService financeService;

    @GetMapping("/wallet")
    public ApiResult<RiderWalletVO> wallet() {
        return ApiResult.ok(financeService.wallet());
    }

    @PutMapping("/wallet/settlement")
    public ApiResult<RiderWalletVO> updateSettlement(@RequestBody SettlementUpdateRequest request) {
        return ApiResult.ok(financeService.updateSettlement(request));
    }

    @GetMapping("/billings")
    public ApiResult<List<RiderBillingRecord>> billings() {
        return ApiResult.ok(financeService.billings());
    }

    @GetMapping("/withdraws")
    public ApiResult<List<RiderWithdrawRecord>> withdraws() {
        return ApiResult.ok(financeService.withdraws());
    }

    @PostMapping("/withdraws")
    public ApiResult<RiderWithdrawRecord> applyWithdraw(@Valid @RequestBody WithdrawApplyRequest request) {
        return ApiResult.ok(financeService.applyWithdraw(request));
    }
}
