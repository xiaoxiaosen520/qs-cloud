package com.qs.takeout.modules.delivery;

import com.qs.takeout.common.auth.RequireRole;
import com.qs.takeout.common.auth.Roles;
import com.qs.takeout.common.result.ApiResult;
import com.qs.takeout.modules.auth.entity.RiderAccount;
import com.qs.takeout.modules.delivery.dto.RiderChangePhoneRequest;
import com.qs.takeout.modules.delivery.dto.RiderDailyStatVO;
import com.qs.takeout.modules.delivery.dto.RiderLocationRequest;
import com.qs.takeout.modules.delivery.dto.RiderOrderVO;
import com.qs.takeout.modules.delivery.dto.RiderProfileUpdateRequest;
import com.qs.takeout.modules.delivery.dto.RiderReviewVO;
import com.qs.takeout.modules.delivery.dto.RiderStatsVO;
import com.qs.takeout.modules.finance.UploadService;
import com.qs.takeout.modules.order.dto.OrderDetailVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rider")
@RequireRole({Roles.RIDER})
@RequiredArgsConstructor
public class RiderController {

    private final RiderService riderService;
    private final UploadService uploadService;
    private final RiderProperties riderProperties;

    @GetMapping("/profile")
    public ApiResult<RiderAccount> profile() {
        return ApiResult.ok(riderService.profile());
    }

    @PutMapping("/profile")
    public ApiResult<RiderAccount> updateProfile(@RequestBody RiderProfileUpdateRequest request) {
        return ApiResult.ok(riderService.updateProfile(request));
    }

    @PutMapping("/profile/phone")
    public ApiResult<RiderAccount> changePhone(@RequestBody RiderChangePhoneRequest request) {
        return ApiResult.ok(riderService.changePhone(request));
    }

    @GetMapping("/stats")
    public ApiResult<RiderStatsVO> stats() {
        return ApiResult.ok(riderService.stats());
    }

    @GetMapping("/stats/daily")
    public ApiResult<List<RiderDailyStatVO>> dailyStats(@RequestParam(required = false) Integer days) {
        return ApiResult.ok(riderService.dailyStats(days));
    }

    @GetMapping("/reviews")
    public ApiResult<List<RiderReviewVO>> reviews(@RequestParam(required = false) Integer limit) {
        return ApiResult.ok(riderService.reviews(limit));
    }

    @PostMapping("/online")
    public ApiResult<RiderAccount> online(@RequestBody Map<String, Boolean> body) {
        boolean online = body != null && Boolean.TRUE.equals(body.get("online"));
        return ApiResult.ok(riderService.setOnline(online));
    }

    @PostMapping("/location")
    public ApiResult<RiderAccount> location(@RequestBody RiderLocationRequest request) {
        return ApiResult.ok(riderService.reportLocation(request));
    }

    @GetMapping("/capability")
    public ApiResult<Map<String, Object>> capability() {
        boolean self = riderProperties.isSelfEnabled();
        return ApiResult.ok(Map.of(
                "selfEnabled", self,
                "deliveryChannel", self ? "SELF_RIDER" : "FENG_NIAO",
                "message", self ? "自有骑手可抢单" : "当前由蜂鸟众包配送，自有骑手暂未开放"
        ));
    }

    @GetMapping("/orders/pool")
    public ApiResult<List<RiderOrderVO>> pool() {
        return ApiResult.ok(riderService.pool());
    }

    @GetMapping("/orders/cancelled")
    public ApiResult<List<RiderOrderVO>> cancelled(@RequestParam(required = false) Integer minutes) {
        return ApiResult.ok(riderService.cancelledRecent(minutes));
    }

    @GetMapping("/orders/mine")
    public ApiResult<List<RiderOrderVO>> mine(@RequestParam(required = false) String status,
                                              @RequestParam(required = false) String phase) {
        String p = phase;
        if ((p == null || p.isBlank()) && status != null && !status.isBlank()) {
            p = status;
        }
        return ApiResult.ok(riderService.mine(p));
    }

    @GetMapping("/orders/{id}")
    public ApiResult<OrderDetailVO> detail(@PathVariable Long id) {
        return ApiResult.ok(riderService.detail(id));
    }

    @PostMapping("/orders/{id}/grab")
    public ApiResult<OrderDetailVO> grab(@PathVariable Long id) {
        return ApiResult.ok(riderService.grab(id));
    }

    @PostMapping("/orders/{id}/pickup")
    public ApiResult<OrderDetailVO> pickup(@PathVariable Long id) {
        return ApiResult.ok(riderService.pickup(id));
    }

    @PostMapping("/orders/{id}/deliver")
    public ApiResult<OrderDetailVO> deliver(@PathVariable Long id) {
        return ApiResult.ok(riderService.deliver(id));
    }

    @PostMapping("/upload")
    public ApiResult<Map<String, String>> upload(@RequestPart("file") MultipartFile file) {
        return ApiResult.ok(uploadService.uploadImage(file));
    }
}
