package com.qs.takeout.modules.admin.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class DashboardVO {

    private long todayOrderCount;
    private BigDecimal todayPayAmount;
    private long pendingApplyCount;
    private long pendingWithdrawCount;
    private long shopCount;
    private long userCount;
    private long riderCount;
    private long onlineRiderCount;
}
