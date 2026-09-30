package com.qs.takeout.modules.shop.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ShopApplyRequest {

    @NotBlank(message = "联系人不能为空")
    private String contactName;

    /** 可不传，默认取登录手机号 */
    private String contactPhone;

    @NotBlank(message = "店铺名称不能为空")
    private String shopName;

    @NotBlank(message = "店铺类型不能为空")
    @Pattern(regexp = "FOOD|CONVENIENCE", message = "店铺类型必须是 FOOD 或 CONVENIENCE")
    private String shopType;

    private Long categoryId;
    private String notice;
    private String logoUrl;
    private String withinUrl;

    @NotBlank(message = "请上传营业执照")
    private String licenseUrl;

    @NotBlank(message = "请上传身份证正面")
    private String idCardFrontUrl;

    @NotBlank(message = "请上传身份证反面")
    private String idCardBackUrl;

    @NotBlank(message = "地址不能为空")
    private String address;

    private String houseNumber;

    @NotNull(message = "纬度不能为空")
    private BigDecimal lat;

    @NotNull(message = "经度不能为空")
    private BigDecimal lng;
}
