package com.prop.wo.common.util;

import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 服务端影像水印：时间、作业人、设备名称、经纬度，禁止前端传入已加水印图片
 */
public final class WatermarkUtil {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int MAX_WIDTH = 1600;

    private WatermarkUtil() {
    }

    public static byte[] mark(byte[] original, LocalDateTime shootTime, String operator,
                              String equipmentName, String geoText) {
        try {
            BufferedImage src = ImageIO.read(new ByteArrayInputStream(original));
            if (src == null) {
                throw new IllegalArgumentException("无法识别的图片格式");
            }
            BufferedImage canvas = toRgb(src);

            List<String> lines = new ArrayList<>();
            lines.add(TIME_FMT.format(shootTime));
            lines.add("作业人：" + safe(operator) + "    设备：" + safe(equipmentName));
            lines.add("坐标：" + safe(geoText));

            Graphics2D g = canvas.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int fontSize = Math.max(16, canvas.getWidth() / 46);
            g.setFont(chineseFont(fontSize));
            FontMetrics fm = g.getFontMetrics();
            int lineHeight = fm.getHeight() + 4;
            int pad = Math.max(12, fontSize / 2);
            int bandHeight = lines.size() * lineHeight + pad * 2;

            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.62f));
            g.setColor(Color.BLACK);
            g.fillRect(0, canvas.getHeight() - bandHeight, canvas.getWidth(), bandHeight);
            g.setComposite(AlphaComposite.SrcOver);
            g.setColor(Color.WHITE);

            int y = canvas.getHeight() - bandHeight + pad + fm.getAscent();
            for (String line : lines) {
                g.drawString(line, pad, y);
                y += lineHeight;
            }
            g.dispose();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(canvas, "jpg", out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("图片水印处理失败：" + e.getMessage(), e);
        }
    }

    private static BufferedImage toRgb(BufferedImage src) {
        int width = Math.min(src.getWidth(), MAX_WIDTH);
        int height = (int) (src.getHeight() * (width / (double) src.getWidth()));
        BufferedImage rgb = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = rgb.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);
        g.drawImage(src.getScaledInstance(width, height, java.awt.Image.SCALE_SMOOTH), 0, 0, null);
        g.dispose();
        return rgb;
    }

    private static Font chineseFont(int size) {
        Path path = FontRegistry.resolve(FontRegistry.SANS);
        if (path != null) {
            try {
                return Font.createFont(Font.TRUETYPE_FONT, path.toFile()).deriveFont(Font.PLAIN, (float) size);
            } catch (Exception ignored) {
                // 回退系统字体
            }
        }
        return new Font("Microsoft YaHei", Font.PLAIN, size);
    }

    private static String safe(String text) {
        return text == null || text.isBlank() ? "—" : text;
    }
}
