package com.qs.takeout.modules.user.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProfileUpdateRequest {

    @Size(max = 32)
    private String nickname;

    @Size(max = 512)
    private String avatarUrl;
}
