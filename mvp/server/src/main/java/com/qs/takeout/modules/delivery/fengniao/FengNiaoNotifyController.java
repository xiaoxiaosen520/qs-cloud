package com.qs.takeout.modules.delivery.fengniao;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** 蜂鸟状态回调，无需登录。 */
@RestController
@RequestMapping("/api/delivery/fengniao")
@RequiredArgsConstructor
public class FengNiaoNotifyController {

    private final FengNiaoDispatchService dispatchService;

    @PostMapping("/notify")
    public Map<String, Object> notify(@RequestBody(required = false) Map<String, Object> body) {
        dispatchService.handleNotify(body == null ? Map.of() : body);
        return Map.of("code", 200, "msg", "success");
    }
}
