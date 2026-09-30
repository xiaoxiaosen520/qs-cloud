package com.qs.takeout.modules.finance.dto;

import lombok.Data;

@Data
public class SettlementUpdateRequest {

    private String realName;
    private String bankCard;
    private String alipayAccount;
    private String wechatAccount;
}
