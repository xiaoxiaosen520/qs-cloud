package com.qs.takeout.modules.delivery.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class RiderLocationRequest {

    private BigDecimal lat;
    private BigDecimal lng;
}
