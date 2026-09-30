package com.qs.takeout.modules.delivery.fengniao;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qs.takeout.modules.delivery.RiderProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({FengNiaoProperties.class, RiderProperties.class})
public class FengNiaoConfig {

    @Bean
    public FengNiaoClient fengNiaoClient(FengNiaoProperties properties, ObjectMapper objectMapper) {
        if (properties.isMock()) {
            return new MockFengNiaoClient();
        }
        return new AnubisFengNiaoClient(properties, objectMapper);
    }
}
