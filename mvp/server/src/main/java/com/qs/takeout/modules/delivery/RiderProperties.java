package com.qs.takeout.modules.delivery;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "qs.rider")
public class RiderProperties {

    /** 自有骑手抢单；暂未自招骑手时关闭，后期再开 */
    private boolean selfEnabled = false;
}
