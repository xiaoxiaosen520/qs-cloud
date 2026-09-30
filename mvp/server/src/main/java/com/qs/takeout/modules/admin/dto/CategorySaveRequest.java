package com.qs.takeout.modules.admin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CategorySaveRequest {

    @NotBlank(message = "类目名称不能为空")
    private String name;

    private String iconUrl;

    /** FOOD / CONVENIENCE / ALL */
    @NotBlank(message = "店铺类型不能为空")
    private String shopType;

    private Integer sort;
    private Integer status;
}
