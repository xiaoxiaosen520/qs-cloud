package com.qs.takeout.modules.finance;

import com.qs.takeout.common.auth.RequireRole;
import com.qs.takeout.common.auth.Roles;
import com.qs.takeout.common.result.ApiResult;
import com.qs.takeout.modules.finance.dto.MerchantStatsVO;
import com.qs.takeout.modules.finance.dto.MerchantWalletVO;
import com.qs.takeout.modules.finance.dto.SettlementUpdateRequest;
import com.qs.takeout.modules.finance.dto.WithdrawApplyRequest;
import com.qs.takeout.modules.finance.entity.MerchantBillingRecord;
import com.qs.takeout.modules.finance.entity.MerchantWithdrawRecord;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/merchant")
@RequireRole({Roles.MERCHANT})
@RequiredArgsConstructor
public class MerchantFinanceController {

    private final MerchantFinanceService financeService;
    private final UploadService uploadService;

    @GetMapping("/wallet")
    public ApiResult<MerchantWalletVO> wallet() {
        return ApiResult.ok(financeService.wallet());
    }

    @PutMapping("/wallet/settlement")
    public ApiResult<MerchantWalletVO> updateSettlement(@RequestBody SettlementUpdateRequest request) {
        return ApiResult.ok(financeService.updateSettlement(request));
    }

    @GetMapping("/billings")
    public ApiResult<List<MerchantBillingRecord>> billings() {
        return ApiResult.ok(financeService.billings());
    }

    @GetMapping("/withdraws")
    public ApiResult<List<MerchantWithdrawRecord>> withdraws() {
        return ApiResult.ok(financeService.withdraws());
    }

    @PostMapping("/withdraws")
    public ApiResult<MerchantWithdrawRecord> applyWithdraw(@Valid @RequestBody WithdrawApplyRequest request) {
        return ApiResult.ok(financeService.applyWithdraw(request));
    }

    @GetMapping("/stats/today")
    public ApiResult<MerchantStatsVO> todayStats() {
        return ApiResult.ok(financeService.todayStats());
    }

    @PostMapping("/upload")
    public ApiResult<Map<String, String>> upload(@RequestPart("file") MultipartFile file) {
        return ApiResult.ok(uploadService.uploadImage(file));
    }
}
