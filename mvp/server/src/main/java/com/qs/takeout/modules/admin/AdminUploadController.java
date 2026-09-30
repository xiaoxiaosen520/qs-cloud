package com.qs.takeout.modules.admin;

import com.qs.takeout.common.auth.RequireRole;
import com.qs.takeout.common.auth.Roles;
import com.qs.takeout.common.result.ApiResult;
import com.qs.takeout.modules.finance.UploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequireRole({Roles.ADMIN})
@RequiredArgsConstructor
public class AdminUploadController {

    private final UploadService uploadService;

    @PostMapping("/upload")
    public ApiResult<Map<String, String>> upload(@RequestPart("file") MultipartFile file) {
        return ApiResult.ok(uploadService.uploadImage(file));
    }
}
