package com.qs.takeout.modules.finance.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class MerchantStatsVO {

    private Integer pendingCount;
    private Integer acceptedCount;
    private Integer refundingCount;
    private Integer todayOrders;
    private BigDecimal todayPayAmount;
    private BigDecimal todayIncome;
    private Integer onSaleGoods;
    private BigDecimal withdrawableBalance;
}
