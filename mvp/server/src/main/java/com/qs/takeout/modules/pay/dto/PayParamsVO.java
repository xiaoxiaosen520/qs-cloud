package com.qs.takeout.modules.pay.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
public class PayParamsVO {

  /** MOCK | WECHAT_MP | WECHAT_APP | WECHAT_H5 | ALIPAY_APP | ALIPAY_H5 | SIMULATE */
  private String mode;
  private Long orderId;
  private String orderNo;
  private BigDecimal amount;
  private String channel;
  private String client;
  /** 微信/支付宝调起参数，或模拟说明 */
  private Map<String, String> params;
  private String hint;
}
