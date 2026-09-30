package com.qs.takeout.modules.pay.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PrepayRequest {

    @NotNull(message = "orderId 不能为空")
    private Long orderId;

    @NotBlank(message = "channel 不能为空")
    private String channel;

    /** MP_WEIXIN / APP / H5 */
    @NotBlank(message = "client 不能为空")
    private String client;
}
