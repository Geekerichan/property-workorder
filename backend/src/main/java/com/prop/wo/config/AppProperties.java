package com.prop.wo.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 业务参数配置（application.yml 中 app.*）
 */
@Data
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private String storageRoot = "./data/uploads";
    private String qrBaseUrl = "http://localhost:5174/h5/equip";
    private String qrSecret = "prop-wo-qr-2026";
    private int responsibilityDefaultDays = 45;
    private int responsibilityMinDays = 45;
    private int responsibilityMaxDays = 60;
    private int responsibilityRemindBeforeDays = 7;
    private int uploadMaxSizeMb = 5;
    private String uploadAllowExt = "jpg,jpeg,png,webp";
    private int checkinRadiusMeter = 200;
    /** 演示模式：true 跳过真实定位距离校验，接受任意位置打卡；生产环境置 false */
    private boolean checkinSkipDistance = false;
    private boolean demoInitPhotos = true;
    private String smsFixedCode = "123456";
    private String annualRemindDays = "30,15,7";
    private String urgeScanCron = "0 */10 * * * ?";
}
