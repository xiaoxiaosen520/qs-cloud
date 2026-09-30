package com.qs.takeout.modules.pay;

import com.qs.takeout.common.auth.RequireRole;
import com.qs.takeout.common.auth.Roles;
import com.qs.takeout.common.result.ApiResult;
import com.qs.takeout.modules.order.dto.OrderDetailVO;
import com.qs.takeout.modules.order.OrderService;
import com.qs.takeout.modules.pay.dto.PayChannelVO;
import com.qs.takeout.modules.pay.dto.PayParamsVO;
import com.qs.takeout.modules.pay.dto.PrepayRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pay")
@RequiredArgsConstructor
public class PayController {

    private final PayService payService;
    private final OrderService orderService;

    @GetMapping("/channels")
    @RequireRole({Roles.USER})
    public ApiResult<List<PayChannelVO>> channels() {
        return ApiResult.ok(payService.listChannels());
    }

    @PostMapping("/prepay")
    @RequireRole({Roles.USER})
    public ApiResult<PayParamsVO> prepay(@Valid @RequestBody PrepayRequest request) {
        return ApiResult.ok(payService.prepay(request));
    }

    /** 模拟支付确认（MOCK / 微信·支付宝模拟流程） */
    @PostMapping("/confirm/{orderId}")
    @RequireRole({Roles.USER})
    public ApiResult<OrderDetailVO> confirm(@PathVariable Long orderId) {
        payService.confirmMockPay(orderId);
        return ApiResult.ok(orderService.detailForUser(orderId));
    }

    /** 微信支付回调（无需登录），须使用原始 body 验签 */
    @PostMapping("/notify/wechat")
    public ResponseEntity<String> wechatNotify(HttpServletRequest request) throws Exception {
        String body = StreamUtils.copyToString(request.getInputStream(), StandardCharsets.UTF_8);
        String timestamp = request.getHeader("Wechatpay-Timestamp");
        String nonce = request.getHeader("Wechatpay-Nonce");
        String signature = request.getHeader("Wechatpay-Signature");
        String serial = request.getHeader("Wechatpay-Serial");
        String result = payService.handleWechatNotify(body, timestamp, nonce, signature, serial);
        if ("SUCCESS".equals(result)) {
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"code\":\"SUCCESS\",\"message\":\"成功\"}");
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"code\":\"FAIL\",\"message\":\"失败\"}");
    }

    /**
     * 支付宝异步通知（无需登录）。
     * 支付宝以 application/x-www-form-urlencoded POST，不能用 JSON body。
     */
    @PostMapping("/notify/alipay")
    public String alipayNotify(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        Map<String, String[]> requestParams = request.getParameterMap();
        for (Map.Entry<String, String[]> entry : requestParams.entrySet()) {
            String[] values = entry.getValue();
            StringBuilder valueStr = new StringBuilder();
            for (int i = 0; i < values.length; i++) {
                valueStr.append(i == values.length - 1 ? values[i] : values[i] + ",");
            }
            params.put(entry.getKey(), valueStr.toString());
        }
        return payService.handleAlipayNotify(params);
    }
}
