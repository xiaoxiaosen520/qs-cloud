package com.qs.takeout.modules.delivery.fengniao;

import com.qs.takeout.modules.delivery.fengniao.entity.DeliveryDispatch;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/** Mock 模式下推进蜂鸟运单：接单 → 到店 → 取餐 → 送达。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FengNiaoMockProgressJob {

    private final FengNiaoProperties properties;
    private final FengNiaoDispatchService dispatchService;

    @Scheduled(fixedDelayString = "${qs.fengniao.mock-step-ms:20000}")
    public void tick() {
        if (!properties.isMock()) {
            return;
        }
        List<DeliveryDispatch> due = dispatchService.dueMock(LocalDateTime.now(), 30);
        for (DeliveryDispatch d : due) {
            try {
                int next = dispatchService.nextMockAnubis(d.getStatus());
                dispatchService.applyStatus(d, next, d.getRiderName(), d.getRiderPhone(), "{\"mock\":true}");
            } catch (Exception e) {
                log.warn("fengniao mock progress failed id={}: {}", d.getId(), e.getMessage());
            }
        }
    }
}
