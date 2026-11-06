package com.prop.wo.modules.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.prop.wo.common.entity.PageQuery;
import com.prop.wo.common.exception.BizException;
import com.prop.wo.common.result.PageResult;
import com.prop.wo.modules.system.dto.SystemDtos;
import com.prop.wo.modules.system.entity.SysUser;
import com.prop.wo.modules.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 用户管理：账号与四类角色分配
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final SysUserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public PageResult<SysUser> page(String keyword, String roleCode, PageQuery query) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .eq(roleCode != null && !roleCode.isBlank(), SysUser::getRoleCode, roleCode)
                .and(keyword != null && !keyword.isBlank(), w -> w
                        .like(SysUser::getUsername, keyword).or().like(SysUser::getRealName, keyword))
                .orderByDesc(SysUser::getId);
        Page<SysUser> page = userMapper.selectPage(new Page<>(query.safePageNum(), query.safePageSize()), wrapper);
        page.getRecords().forEach(user -> user.setPassword(null));
        return PageResult.of(page);
    }

    public List<SysUser> listByRole(String roleCode) {
        List<SysUser> list = userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getRoleCode, roleCode).eq(SysUser::getStatus, "ENABLED"));
        list.forEach(user -> user.setPassword(null));
        return list;
    }

    @Transactional(rollbackFor = Exception.class)
    public SysUser save(SystemDtos.UserSaveReq req) {
        SysUser user = req.id() == null ? new SysUser() : userMapper.selectById(req.id());
        if (user == null) {
            throw BizException.of("用户不存在");
        }
        Long same = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, req.username())
                .ne(req.id() != null, SysUser::getId, req.id()));
        if (same != null && same > 0) {
            throw BizException.of("登录账号已存在");
        }
        user.setUsername(req.username());
        user.setRealName(req.realName());
        user.setPhone(req.phone());
        user.setRoleCode(req.roleCode());
        user.setCompanyId(req.companyId());
        user.setPersonnelId(req.personnelId());
        user.setStatus(req.status() == null ? "ENABLED" : req.status());
        user.setRemark(req.remark());
        if (req.password() != null && !req.password().isBlank()) {
            user.setPassword(passwordEncoder.encode(req.password()));
        } else if (user.getId() == null) {
            throw BizException.of("新建用户必须设置初始密码");
        }
        if (user.getId() == null) {
            userMapper.insert(user);
        } else {
            userMapper.updateById(user);
        }
        user.setPassword(null);
        return user;
    }

    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(Long id, String newPassword) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw BizException.of("用户不存在");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
    }

    public void delete(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            return;
        }
        if ("admin".equals(user.getUsername())) {
            throw BizException.of("内置管理员账号不可删除");
        }
        userMapper.deleteById(id);
    }
}
