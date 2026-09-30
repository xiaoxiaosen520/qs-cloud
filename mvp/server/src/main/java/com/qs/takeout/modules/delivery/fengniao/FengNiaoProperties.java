package com.qs.takeout.modules.delivery.fengniao;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "qs.fengniao")
public class FengNiaoProperties {

    /** true：本地 Mock 状态机；有资质后改 false 走 Anubis */
    private boolean mock = true;
    /** Mock 推进间隔毫秒 */
    private long mockStepMs = 20000;
    private String appId = "";
    private String secret = "";
    private boolean sandbox = true;
    private String notifyUrl = "http://127.0.0.1:8080/api/delivery/fengniao/notify";
    /** 1腾讯 2百度 3高德 */
    private int positionSource = 3;
}
