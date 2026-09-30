package com.qs.takeout.modules.delivery.dto;

import lombok.Data;

@Data
public class RiderProfileUpdateRequest {

    private String name;
    private String avatarUrl;
    private String realName;
}
