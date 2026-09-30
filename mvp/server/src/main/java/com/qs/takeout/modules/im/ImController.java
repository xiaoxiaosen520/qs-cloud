package com.qs.takeout.modules.im;

import com.qs.takeout.common.auth.RequireRole;
import com.qs.takeout.common.auth.Roles;
import com.qs.takeout.common.result.ApiResult;
import com.qs.takeout.modules.im.dto.ImOpenRequest;
import com.qs.takeout.modules.im.dto.ImSendRequest;
import com.qs.takeout.modules.im.dto.ImSessionVO;
import com.qs.takeout.modules.im.entity.ImMessage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/im")
@RequireRole({Roles.USER, Roles.MERCHANT, Roles.RIDER})
@RequiredArgsConstructor
public class ImController {

    private final ImService imService;

    @GetMapping("/sessions")
    public ApiResult<List<com.qs.takeout.modules.im.dto.ImSessionCardVO>> list() {
        return ApiResult.ok(imService.listMine());
    }

    @GetMapping("/unread")
    public ApiResult<com.qs.takeout.modules.im.dto.ImUnreadVO> unread() {
        return ApiResult.ok(imService.unreadSummary());
    }

    @PostMapping("/sessions/open")
    public ApiResult<ImSessionVO> open(@Valid @RequestBody ImOpenRequest request) {
        return ApiResult.ok(imService.open(request));
    }

    @PostMapping("/sessions/{id}/read")
    public ApiResult<ImSessionVO> markRead(
            @PathVariable Long id,
            @RequestBody(required = false) com.qs.takeout.modules.im.dto.ImReadRequest request) {
        return ApiResult.ok(imService.markRead(id, request));
    }

    @GetMapping("/sessions/{id}")
    public ApiResult<ImSessionVO> detail(
            @PathVariable Long id,
            @RequestParam(required = false) Long afterId) {
        return ApiResult.ok(imService.detail(id, afterId));
    }

    @GetMapping("/sessions/{id}/messages")
    public ApiResult<List<ImMessage>> messages(
            @PathVariable Long id,
            @RequestParam(required = false) Long afterId) {
        return ApiResult.ok(imService.messages(id, afterId));
    }

    @PostMapping("/sessions/{id}/messages")
    public ApiResult<ImMessage> send(
            @PathVariable Long id,
            @Valid @RequestBody ImSendRequest request) {
        return ApiResult.ok(imService.send(id, request));
    }
}
