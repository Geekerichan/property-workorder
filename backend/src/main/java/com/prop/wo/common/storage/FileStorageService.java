package com.prop.wo.common.storage;

import java.nio.file.Path;

/**
 * 文件存储抽象：默认本地磁盘实现，二期可切换 MinIO
 */
public interface FileStorageService {

    /** 保存字节流，返回相对路径（如 photo/202611/xxx.jpg） */
    String save(byte[] data, String bizDir, String ext);

    /** 相对路径转绝对路径 */
    Path resolve(String relativePath);

    byte[] read(String relativePath);

    boolean exists(String relativePath);

    void delete(String relativePath);

    String rootPath();
}
