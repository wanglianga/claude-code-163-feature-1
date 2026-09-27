package com.city.manhole.controller;

import com.city.manhole.common.ApiResponse;
import com.city.manhole.common.BizException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/files")
public class FileController {
    private static final Set<String> ALLOWED = Set.of("jpg", "jpeg", "png", "gif", "webp", "bmp");

    @Value("${app.upload-dir}")
    private String uploadDir;

    @PostMapping
    public ApiResponse<String> upload(@RequestParam("file") MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new BizException("文件为空");
        }
        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String ext = original.contains(".")
                ? original.substring(original.lastIndexOf('.') + 1).toLowerCase() : "";
        if (!ALLOWED.contains(ext)) {
            throw new BizException("仅支持图片文件（jpg/png/gif/webp/bmp）");
        }
        File dir = new File(uploadDir).getAbsoluteFile();
        if (!dir.exists() && !dir.mkdirs()) {
            throw new BizException("上传目录创建失败");
        }
        String name = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        file.transferTo(new File(dir, name).getAbsoluteFile());
        return ApiResponse.ok("/uploads/" + name);
    }
}
