package com.qs.takeout.modules.finance.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class MerchantWalletVO {

    private BigDecimal withdrawableBalance;
    private BigDecimal frozenBalance;
    private BigDecimal totalBalance;
    private String realName;
    private String bankCard;
    private String alipayAccount;
    private String wechatAccount;
    private BigDecimal withdrawFee;
    private BigDecimal commissionRate;
}
