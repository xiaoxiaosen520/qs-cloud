package com.qs.takeout.modules.admin;

import com.qs.takeout.common.result.ApiResult;
import com.qs.takeout.modules.auth.AuthService;
import com.qs.takeout.modules.auth.dto.AdminLoginRequest;
import com.qs.takeout.modules.auth.dto.LoginResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ApiResult<LoginResponse> login(@Valid @RequestBody AdminLoginRequest request) {
        return ApiResult.ok(authService.adminLogin(request));
    }
}
