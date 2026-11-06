package com.prop.wo.common.storage;

import com.prop.wo.common.exception.BizException;
import com.prop.wo.config.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 本地磁盘存储实现：文件统一落在源码目录 data/uploads 下
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LocalFileStorageService implements FileStorageService {

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyyMM");
    private final AppProperties appProperties;

    @Override
    public String save(byte[] data, String bizDir, String ext) {
        String month = LocalDate.now().format(MONTH_FMT);
        String fileName = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        String relative = bizDir + "/" + month + "/" + fileName;
        Path target = resolve(relative);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, data);
            return relative;
        } catch (IOException e) {
            log.error("文件写入失败 path={}", target, e);
            throw new BizException("文件保存失败，请稍后重试");
        }
    }

    @Override
    public Path resolve(String relativePath) {
        Path root = Paths.get(appProperties.getStorageRoot()).toAbsolutePath().normalize();
        Path target = root.resolve(relativePath == null ? "" : relativePath).normalize();
        if (!target.startsWith(root)) {
            throw new BizException("非法的文件路径");
        }
        return target;
    }

    @Override
    public byte[] read(String relativePath) {
        try {
            return Files.readAllBytes(resolve(relativePath));
        } catch (IOException e) {
            throw new BizException("文件不存在或无法读取");
        }
    }

    @Override
    public boolean exists(String relativePath) {
        return Files.exists(resolve(relativePath));
    }

    @Override
    public void delete(String relativePath) {
        try {
            Files.deleteIfExists(resolve(relativePath));
        } catch (IOException e) {
            log.warn("文件删除失败 {}", relativePath);
        }
    }

    @Override
    public String rootPath() {
        return Paths.get(appProperties.getStorageRoot()).toAbsolutePath().normalize().toString();
    }
}
