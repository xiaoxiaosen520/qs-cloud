package com.qs.takeout.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class SmsSendRequest {

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1\\d{10}$", message = "手机号格式错误")
    private String phone;

    /** LOGIN_USER / LOGIN_MERCHANT / LOGIN_RIDER */
    @NotBlank(message = "scene 不能为空")
    private String scene;
}
