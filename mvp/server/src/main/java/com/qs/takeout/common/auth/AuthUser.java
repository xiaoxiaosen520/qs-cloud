package com.qs.takeout.common.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthUser {

    private Long id;
    private String role;
    private Long shopId;
    private String phone;
}
