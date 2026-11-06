package com.prop.wo.common.util;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * 字体注册：把 classpath 内置字体释放为临时文件，供 Java2D 水印与 PDF 使用
 */
public final class FontRegistry {

    public static final String SANS = "PropWoSans.ttf";
    public static final String SERIF = "NotoSerifSC-SemiBold.otf";

    private static final Path TEMP_DIR = Path.of(System.getProperty("java.io.tmpdir"), "prop-wo-fonts");

    private FontRegistry() {
    }

    /** 返回字体文件，找不到内置字体时返回 null（调用方回退系统字体） */
    public static Path resolve(String fileName) {
        try {
            Path target = TEMP_DIR.resolve(fileName);
            if (Files.exists(target) && Files.size(target) > 0) {
                return target;
            }
            try (InputStream in = FontRegistry.class.getResourceAsStream("/fonts/" + fileName)) {
                if (in == null) {
                    return null;
                }
                Files.createDirectories(TEMP_DIR);
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
                return target;
            }
        } catch (Exception e) {
            return null;
        }
    }
}
