package com.qs.takeout.modules.finance.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class WithdrawApplyRequest {

    @NotNull(message = "提现金额不能为空")
    @DecimalMin(value = "1.00", message = "最低提现 1 元")
    private BigDecimal amount;

    @NotBlank(message = "收款方式不能为空")
    private String paymentMode;
}
