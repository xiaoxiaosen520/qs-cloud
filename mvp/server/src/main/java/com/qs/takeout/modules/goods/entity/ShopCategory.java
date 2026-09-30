package com.qs.takeout.modules.goods.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("shop_category")
public class ShopCategory {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long shopId;
    private String name;
    private Integer sort;
    private Integer status;
    private LocalDateTime createdAt;
}
