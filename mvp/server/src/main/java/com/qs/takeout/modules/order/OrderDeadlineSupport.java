package com.qs.takeout.modules.order;

import com.qs.takeout.modules.finance.entity.SysConfig;
import com.qs.takeout.modules.finance.mapper.SysConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 订单各阶段 deadline 计算；超时时刻在状态变更时落库，扫任务只比时刻。
 */
@Component
@RequiredArgsConstructor
public class OrderDeadlineSupport {

    public static final String KEY_PAY = "pay_timeout_minutes";
    public static final String KEY_ACCEPT = "accept_timeout_minutes";
    public static final String KEY_AUTO_COMPLETE = "auto_complete_minutes";

    private final SysConfigMapper sysConfigMapper;

    public LocalDateTime payDeadlineFromNow() {
        return LocalDateTime.now().plusMinutes(minutes(KEY_PAY, 15));
    }

    public LocalDateTime acceptDeadlineFromNow() {
        return LocalDateTime.now().plusMinutes(minutes(KEY_ACCEPT, 5));
    }

    public LocalDateTime autoCompleteFromNow() {
        return LocalDateTime.now().plusMinutes(minutes(KEY_AUTO_COMPLETE, 240));
    }

    public int minutes(String key, int defaultMinutes) {
        SysConfig cfg = sysConfigMapper.selectById(key);
        if (cfg == null || !StringUtils.hasText(cfg.getConfigValue())) {
            return defaultMinutes;
        }
        try {
            int v = Integer.parseInt(cfg.getConfigValue().trim());
            return v > 0 ? v : defaultMinutes;
        } catch (Exception e) {
            return defaultMinutes;
        }
    }
}
