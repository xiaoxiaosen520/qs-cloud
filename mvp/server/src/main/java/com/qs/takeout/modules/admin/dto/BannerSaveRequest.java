package com.qs.takeout.modules.admin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BannerSaveRequest {

    private String title;

    @NotBlank(message = "图片不能为空")
    private String imageUrl;

    private String linkUrl;
    private Integer sort;
    /** 1 上架 / 0 下架 */
    private Integer status;
}
