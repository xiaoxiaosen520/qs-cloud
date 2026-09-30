package com.qs.takeout.modules.finance.dto;

import lombok.Data;

@Data
public class WithdrawReviewRequest {

    private boolean approved;
    private String reason;
}
