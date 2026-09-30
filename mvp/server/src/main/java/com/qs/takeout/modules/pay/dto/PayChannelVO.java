package com.qs.takeout.modules.pay.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PayChannelVO {

    private String code;
    private String name;
    private boolean enabled;
    private String desc;
}
