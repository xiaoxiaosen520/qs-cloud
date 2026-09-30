package com.qs.takeout.common.config;

import com.qs.takeout.common.result.ApiResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthController {

    @GetMapping({"/", "/api/health"})
    public ApiResult<Map<String, String>> health() {
        return ApiResult.ok(Map.of(
                "service", "qs-takeout-server",
                "status", "up",
                "hint", "请调用 /api/** 接口，联调见 docs/auth-apply.md"
        ));
    }
}
