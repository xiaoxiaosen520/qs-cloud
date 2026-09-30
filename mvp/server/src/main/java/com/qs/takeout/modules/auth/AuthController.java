package com.qs.takeout.modules.auth;

import com.qs.takeout.common.auth.RequireRole;
import com.qs.takeout.common.auth.Roles;
import com.qs.takeout.common.result.ApiResult;
import com.qs.takeout.modules.auth.dto.AdminLoginRequest;
import com.qs.takeout.modules.auth.dto.LoginResponse;
import com.qs.takeout.modules.auth.dto.SmsLoginRequest;
import com.qs.takeout.modules.auth.dto.SmsSendRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/sms/send")
    public ApiResult<Void> sendSms(@Valid @RequestBody SmsSendRequest request) {
        authService.sendSms(request);
        return ApiResult.ok();
    }

    @PostMapping("/sms/login")
    public ApiResult<LoginResponse> smsLogin(@Valid @RequestBody SmsLoginRequest request) {
        return ApiResult.ok(authService.smsLogin(request));
    }

    @GetMapping("/me")
    @RequireRole({Roles.USER, Roles.MERCHANT, Roles.RIDER, Roles.ADMIN})
    public ApiResult<Map<String, Object>> me() {
        return ApiResult.ok(authService.me());
    }
}
