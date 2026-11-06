package com.prop.wo.modules.system.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.prop.wo.common.context.LoginUser;
import com.prop.wo.common.context.LoginUserHolder;
import com.prop.wo.common.exception.BizException;
import com.prop.wo.modules.system.dto.SystemDtos;
import com.prop.wo.modules.system.entity.SysUser;
import com.prop.wo.modules.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 认证：PC 管理端与 H5 作业端统一账号体系，业主端单独走验证码登录
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserMapper userMapper;
    private final OperLogService operLogService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public Map<String, Object> login(SystemDtos.LoginReq req, boolean pc) {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, req.username()).last("limit 1"));
        if (user == null || !passwordEncoder.matches(req.password(), user.getPassword())) {
            operLogService.recordLogin(req.username(), false, "账号或密码错误");
            throw BizException.of("账号或密码错误");
        }
        if (!"ENABLED".equals(user.getStatus())) {
            operLogService.recordLogin(req.username(), false, "账号已停用");
            throw BizException.of("账号已停用，请联系管理员");
        }
        if ("OWNER".equals(user.getRoleCode())) {
            throw BizException.of("业主请使用业主端登录");
        }
        if (!pc && "ADMIN".equals(user.getRoleCode())) {
            throw BizException.of("管理员账号请使用管理端登录");
        }
        StpUtil.login(user.getId());
        LoginUser loginUser = new LoginUser();
        loginUser.setId(user.getId());
        loginUser.setUsername(user.getUsername());
        loginUser.setRealName(user.getRealName());
        loginUser.setRole(user.getRoleCode());
        loginUser.setPhone(user.getPhone());
        loginUser.setCompanyId(user.getCompanyId());
        loginUser.setPersonnelId(user.getPersonnelId());
        LoginUserHolder.put(loginUser);

        operLogService.recordLogin(req.username(), true, "登录成功");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("token", StpUtil.getTokenValue());
        result.put("tokenName", StpUtil.getTokenName());
        result.put("user", loginUser);
        return result;
    }

    public void logout() {
        StpUtil.logout();
    }

    public LoginUser me() {
        return LoginUserHolder.require();
    }

    public void changePassword(SystemDtos.PasswordReq req) {
        LoginUser loginUser = LoginUserHolder.require();
        SysUser user = userMapper.selectById(loginUser.getId());
        if (user == null) {
            throw BizException.of("账号不存在");
        }
        if (!passwordEncoder.matches(req.oldPassword(), user.getPassword())) {
            throw BizException.of("原密码不正确");
        }
        user.setPassword(passwordEncoder.encode(req.newPassword()));
        userMapper.updateById(user);
        StpUtil.logout();
    }
}
