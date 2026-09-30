package com.qs.takeout.modules.finance;

import com.qs.takeout.common.exception.BizException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class UploadService {

    private static final Set<String> ALLOWED = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif"
    );

    @Value("${qs.upload.dir:uploads}")
    private String uploadDir;

    public Map<String, String> uploadImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException("请选择图片");
        }
        if (file.getSize() > 5 * 1024 * 1024L) {
            throw new BizException("图片不能超过 5MB");
        }
        String contentType = file.getContentType();
        if (!StringUtils.hasText(contentType) || !ALLOWED.contains(contentType)) {
            throw new BizException("仅支持 jpg/png/webp/gif");
        }
        String ext = switch (contentType) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            default -> ".jpg";
        };
        String day = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String name = UUID.randomUUID().toString().replace("-", "") + ext;
        Path dir = Paths.get(uploadDir, day).toAbsolutePath().normalize();
        try {
            Files.createDirectories(dir);
            Path target = dir.resolve(name);
            file.transferTo(target);
        } catch (IOException e) {
            throw new BizException("上传失败");
        }
        String url = "/uploads/" + day + "/" + name;
        return Map.of("url", url);
    }
}
