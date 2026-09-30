package com.qs.takeout.modules.shop.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ShopApplyReviewRequest {

    @NotNull(message = "approved 不能为空")
    private Boolean approved;

    private String rejectReason;
}
