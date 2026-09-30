package com.qs.takeout.common.jwt;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "qs.jwt")
public class JwtProperties {

    private String secret;
    private long expireHours = 720;
}
