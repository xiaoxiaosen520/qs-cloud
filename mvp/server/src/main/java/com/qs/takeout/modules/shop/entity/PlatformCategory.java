package com.qs.takeout.modules.shop.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("platform_category")
public class PlatformCategory {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String iconUrl;
    private String shopType;
    private Integer sort;
    private Integer status;
    private LocalDateTime createdAt;
}
