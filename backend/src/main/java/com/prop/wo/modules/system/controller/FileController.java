package com.prop.wo.modules.system.controller;

import com.prop.wo.common.exception.BizException;
import com.prop.wo.common.storage.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 文件访问：留痕照片、合同 PDF、导出文件（需登录，路径经存储层校验防目录穿越）
 */
@Tag(name = "文件")
@RestController
@RequestMapping("/api/file")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileStorageService;

    @Operation(summary = "查看文件")
    @GetMapping("/view")
    public ResponseEntity<Resource> view(@RequestParam String path) {
        if (!fileStorageService.exists(path)) {
            throw new BizException("文件不存在");
        }
        byte[] bytes = fileStorageService.read(path);
        MediaType mediaType = mediaType(path);
        return ResponseEntity.ok().contentType(mediaType)
                .header(HttpHeaders.CACHE_CONTROL, "max-age=3600")
                .body(new ByteArrayResource(bytes));
    }

    @Operation(summary = "下载文件")
    @GetMapping("/download")
    public ResponseEntity<Resource> download(@RequestParam String path,
                                             @RequestParam(required = false) String fileName) {
        if (!fileStorageService.exists(path)) {
            throw new BizException("文件不存在");
        }
        byte[] bytes = fileStorageService.read(path);
        String name = fileName == null || fileName.isBlank()
                ? path.substring(path.lastIndexOf('/') + 1) : fileName;
        String encoded = URLEncoder.encode(name, StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new ByteArrayResource(bytes));
    }

    private MediaType mediaType(String path) {
        String lower = path.toLowerCase();
        if (lower.endsWith(".png")) {
            return MediaType.IMAGE_PNG;
        }
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return MediaType.IMAGE_JPEG;
        }
        if (lower.endsWith(".webp")) {
            return MediaType.parseMediaType("image/webp");
        }
        if (lower.endsWith(".pdf")) {
            return MediaType.APPLICATION_PDF;
        }
        return MediaType.APPLICATION_OCTET_STREAM;
    }
}
