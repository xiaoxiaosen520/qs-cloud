package com.qs.takeout.modules.delivery.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class RiderDailyStatVO {

    private String date;
    private Integer completed;
    private BigDecimal income;
}
