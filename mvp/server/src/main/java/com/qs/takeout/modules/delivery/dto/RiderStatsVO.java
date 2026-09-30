package com.qs.takeout.modules.delivery.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class RiderStatsVO {

    private Integer online;
    private Integer waitPickupCount;
    private Integer onWayCount;
    private Integer todayCompleted;
    private BigDecimal todayIncome;
    private BigDecimal weekIncome;
    private BigDecimal monthIncome;
    private Integer totalCompleted;
    private BigDecimal withdrawableBalance;
}
