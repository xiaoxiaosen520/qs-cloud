package com.qs.takeout.modules.auth.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginResponse {

    private String token;
    private String role;
    private Long userId;
    private Long shopId;
    private String phone;
    private String nickname;
    private String avatarUrl;
}
