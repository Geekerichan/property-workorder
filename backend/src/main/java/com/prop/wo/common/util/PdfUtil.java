package com.prop.wo.common.util;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * HTML 转 PDF（内置中文字体，避免 Linux 环境缺字）
 */
public final class PdfUtil {

    public static final String FONT_FAMILY = "PropWoSans";

    private PdfUtil() {
    }

    public static byte[] render(String bodyHtml) {
        String html = wrap(bodyHtml);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            Path font = FontRegistry.resolve(FontRegistry.SANS);
            if (font != null) {
                builder.useFont(font.toFile(), FONT_FAMILY);
            }
            builder.withHtmlContent(html, null);
            builder.toStream(out);
            builder.run();
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("PDF 生成失败：" + e.getMessage(), e);
        }
    }

    public static void renderTo(String bodyHtml, String absolutePath) {
        try {
            byte[] bytes = render(bodyHtml);
            Path path = Path.of(absolutePath);
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            Files.write(path, bytes);
        } catch (Exception e) {
            throw new IllegalStateException("PDF 写入失败：" + e.getMessage(), e);
        }
    }

    private static String wrap(String bodyHtml) {
        StringBuilder css = new StringBuilder();
        css.append("body{font-family:'").append(FONT_FAMILY).append("',sans-serif;font-size:12px;color:#22201B;}")
                .append("h1{font-size:20px;font-weight:600;text-align:center;letter-spacing:2px;}")
                .append("h2{font-size:14px;font-weight:600;border-left:3px solid #D9480F;padding-left:8px;margin-top:18px;}")
                .append("table{width:100%;border-collapse:collapse;margin-top:8px;}")
                .append("td,th{border:1px solid #C9C4B8;padding:6px 8px;font-size:11px;vertical-align:top;}")
                .append("th{background:#F1EEE7;text-align:left;font-weight:500;}")
                .append(".mono{font-family:'Courier New',monospace;}")
                .append(".muted{color:#6B675E;font-size:10px;}")
                .append(".sign{margin-top:36px;width:100%;}")
                .append(".sign td{border:none;font-size:12px;}");
        return "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"/><style>" + css
                + "</style></head><body>" + bodyHtml + "</body></html>";
    }

    public static String escape(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("\n", "<br/>");
    }

    public static String text(String text) {
        return new String(escape(text).getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
    }
}
