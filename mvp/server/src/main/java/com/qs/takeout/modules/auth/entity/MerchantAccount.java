package com.qs.takeout.modules.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("merchant_account")
public class MerchantAccount {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String phone;
    private String passwordHash;
    private Long shopId;
    private BigDecimal withdrawableBalance;
    private BigDecimal frozenBalance;
    private String realName;
    private String bankCard;
    private String alipayAccount;
    private String wechatAccount;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
