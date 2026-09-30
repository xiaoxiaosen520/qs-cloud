package com.qs.takeout.modules.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("rider_account")
public class RiderAccount {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String phone;
    private String name;
    private String avatarUrl;
    private String realName;
    private BigDecimal withdrawableBalance;
    private BigDecimal frozenBalance;
    private String bankCard;
    private String alipayAccount;
    private String wechatAccount;
    private BigDecimal lat;
    private BigDecimal lng;
    private LocalDateTime locationAt;
    private Integer status;
    private Integer online;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
