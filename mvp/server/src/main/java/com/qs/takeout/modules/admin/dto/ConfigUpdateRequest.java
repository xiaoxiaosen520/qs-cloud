package com.qs.takeout.modules.admin.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.Map;

@Data
public class ConfigUpdateRequest {

    @NotEmpty(message = "配置不能为空")
    private Map<String, String> configs;
}
