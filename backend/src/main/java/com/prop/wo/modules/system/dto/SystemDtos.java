package com.prop.wo.modules.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 系统模块入参
 */
public class SystemDtos {

    public record LoginReq(
            @NotBlank(message = "请输入账号") String username,
            @NotBlank(message = "请输入密码") String password) {
    }

    public record PasswordReq(
            @NotBlank(message = "请输入原密码") String oldPassword,
            @NotBlank(message = "请输入新密码") @Size(min = 6, max = 32, message = "新密码长度需 6-32 位") String newPassword) {
    }

    public record UserSaveReq(
            Long id,
            @NotBlank(message = "请输入登录账号") String username,
            String password,
            @NotBlank(message = "请输入姓名") String realName,
            @Pattern(regexp = "^$|^1\\d{10}$", message = "手机号需为 11 位") String phone,
            @NotBlank(message = "请选择角色") String roleCode,
            Long companyId,
            Long personnelId,
            String status,
            String remark) {
    }

    public record DictSaveReq(Long id, @NotBlank(message = "请输入字典类型") String dictType,
                              @NotBlank(message = "请输入字典名称") String dictName, String status, String remark) {
    }

    public record DictItemSaveReq(Long id, @NotBlank(message = "缺少字典类型") String dictType,
                                  @NotBlank(message = "请输入显示名称") String itemLabel,
                                  @NotBlank(message = "请输入字典值") String itemValue,
                                  Integer sortNo, String status, String remark) {
    }

    public record ConfigSaveReq(@NotBlank(message = "请输入参数键") String configKey,
                                @NotBlank(message = "请输入参数值") String configValue,
                                @NotBlank(message = "请输入参数名称") String configName, String remark) {
    }
}
