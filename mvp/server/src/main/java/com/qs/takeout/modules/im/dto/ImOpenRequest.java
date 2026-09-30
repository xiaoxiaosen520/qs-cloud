package com.qs.takeout.modules.im.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ImOpenRequest {

    @NotNull(message = "orderId 不能为空")
    private Long orderId;

    /** USER_MERCHANT / USER_RIDER */
    @NotBlank(message = "type 不能为空")
    private String type;
}
