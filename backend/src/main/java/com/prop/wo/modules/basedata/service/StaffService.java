package com.prop.wo.modules.basedata.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.prop.wo.common.constant.BizConst;
import com.prop.wo.common.entity.PageQuery;
import com.prop.wo.common.exception.BizException;
import com.prop.wo.common.result.PageResult;
import com.prop.wo.modules.basedata.entity.Personnel;
import com.prop.wo.modules.basedata.entity.Team;
import com.prop.wo.modules.basedata.mapper.PersonnelMapper;
import com.prop.wo.modules.basedata.mapper.TeamMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 班组与人员：在岗状态、资质有效期、可接单校验
 */
@Service
@RequiredArgsConstructor
public class StaffService {

    private final TeamMapper teamMapper;
    private final PersonnelMapper personnelMapper;

    public List<Team> teams() {
        return teamMapper.selectList(new LambdaQueryWrapper<Team>().orderByAsc(Team::getId));
    }

    public Team saveTeam(Team team) {
        if (!StringUtils.hasText(team.getName()) || !StringUtils.hasText(team.getTeamType())) {
            throw new BizException("请填写班组名称与维修类型");
        }
        if (team.getId() == null) {
            teamMapper.insert(team);
        } else {
            teamMapper.updateById(team);
        }
        return team;
    }

    public void deleteTeam(Long id) {
        Long count = personnelMapper.selectCount(new LambdaQueryWrapper<Personnel>().eq(Personnel::getTeamId, id));
        if (count != null && count > 0) {
            throw new BizException("该班组下存在人员，禁止删除");
        }
        teamMapper.deleteById(id);
    }

    public PageResult<Map<String, Object>> personnelPage(String keyword, Long teamId, Long companyId,
                                                         String dutyStatus, PageQuery query) {
        LambdaQueryWrapper<Personnel> wrapper = new LambdaQueryWrapper<Personnel>()
                .eq(teamId != null, Personnel::getTeamId, teamId)
                .eq(companyId != null, Personnel::getCompanyId, companyId)
                .eq(StringUtils.hasText(dutyStatus), Personnel::getDutyStatus, dutyStatus)
                .and(StringUtils.hasText(keyword), w -> w.like(Personnel::getName, keyword)
                        .or().like(Personnel::getPhone, keyword))
                .orderByAsc(Personnel::getDutyStatus).orderByAsc(Personnel::getId);
        Page<Personnel> page = personnelMapper.selectPage(new Page<>(query.safePageNum(), query.safePageSize()), wrapper);
        Map<Long, String> teamNames = new LinkedHashMap<>();
        teams().forEach(team -> teamNames.put(team.getId(), team.getName()));

        List<Map<String, Object>> rows = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (Personnel personnel : page.getRecords()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", personnel.getId());
            row.put("name", personnel.getName());
            row.put("phone", personnel.getPhone());
            row.put("teamId", personnel.getTeamId());
            row.put("teamName", teamNames.get(personnel.getTeamId()));
            row.put("companyId", personnel.getCompanyId());
            row.put("skills", personnel.getSkills());
            row.put("certName", personnel.getCertName());
            row.put("certNo", personnel.getCertNo());
            row.put("certExpireDate", personnel.getCertExpireDate());
            row.put("certExpired", personnel.getCertExpireDate() != null
                    && personnel.getCertExpireDate().isBefore(today));
            row.put("certMissing", personnel.getCertExpireDate() == null);
            row.put("dutyStatus", personnel.getDutyStatus());
            row.put("remark", personnel.getRemark());
            rows.add(row);
        }
        return PageResult.of(page.getTotal(), rows);
    }

    /** 派单/抢单可选人员：在岗且资质有效 */
    public List<Map<String, Object>> availablePersonnel(Long teamId) {
        List<Personnel> list = personnelMapper.selectList(new LambdaQueryWrapper<Personnel>()
                .eq(Personnel::getDutyStatus, BizConst.DUTY_ON)
                .eq(teamId != null, Personnel::getTeamId, teamId)
                .orderByAsc(Personnel::getTeamId));
        List<Map<String, Object>> rows = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (Personnel personnel : list) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", personnel.getId());
            row.put("name", personnel.getName());
            row.put("teamId", personnel.getTeamId());
            row.put("companyId", personnel.getCompanyId());
            row.put("skills", personnel.getSkills());
            row.put("certValid", personnel.getCertExpireDate() != null
                    && !personnel.getCertExpireDate().isBefore(today));
            rows.add(row);
        }
        return rows;
    }

    public Personnel savePersonnel(Personnel personnel) {
        if (!StringUtils.hasText(personnel.getName())) {
            throw new BizException("请输入人员姓名");
        }
        if (personnel.getPhone() == null || !personnel.getPhone().matches("^1\\d{10}$")) {
            throw new BizException("手机号需为 11 位");
        }
        if (personnel.getDutyStatus() == null) {
            personnel.setDutyStatus(BizConst.DUTY_ON);
        }
        if (personnel.getId() == null) {
            personnelMapper.insert(personnel);
        } else {
            personnelMapper.updateById(personnel);
        }
        return personnel;
    }

    @Transactional(rollbackFor = Exception.class)
    public void changeDutyStatus(Long id, String dutyStatus) {
        Personnel personnel = personnelMapper.selectById(id);
        if (personnel == null) {
            throw new BizException("人员不存在");
        }
        if (!List.of(BizConst.DUTY_ON, BizConst.DUTY_LEAVE, BizConst.DUTY_RESIGNED).contains(dutyStatus)) {
            throw new BizException("在岗状态不合法");
        }
        personnel.setDutyStatus(dutyStatus);
        personnelMapper.updateById(personnel);
    }

    public Personnel get(Long id) {
        return personnelMapper.selectById(id);
    }

    /**
     * 接单前校验：必须在岗、资质已登记且未过期
     */
    public Personnel checkCanAccept(Long personnelId) {
        Personnel personnel = personnelId == null ? null : personnelMapper.selectById(personnelId);
        if (personnel == null) {
            throw new BizException("人员档案不存在");
        }
        if (!BizConst.DUTY_ON.equals(personnel.getDutyStatus())) {
            String label = BizConst.DUTY_LEAVE.equals(personnel.getDutyStatus()) ? "请假" : "离职";
            throw new BizException(personnel.getName() + " 当前处于" + label + "状态，无法接单");
        }
        if (personnel.getCertExpireDate() == null) {
            throw new BizException(personnel.getName() + " 未登记资质证件，无法接单");
        }
        if (personnel.getCertExpireDate().isBefore(LocalDate.now())) {
            throw new BizException(personnel.getName() + " 的资质证件已于 "
                    + personnel.getCertExpireDate() + " 过期，无法接单");
        }
        return personnel;
    }

    /** 资质临期清单（30 天内到期） */
    public List<Map<String, Object>> expiringCertificates() {
        LocalDate today = LocalDate.now();
        List<Personnel> list = personnelMapper.selectList(new LambdaQueryWrapper<Personnel>()
                .isNotNull(Personnel::getCertExpireDate)
                .le(Personnel::getCertExpireDate, today.plusDays(30))
                .orderByAsc(Personnel::getCertExpireDate));
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Personnel personnel : list) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("personnelId", personnel.getId());
            row.put("name", personnel.getName());
            row.put("certName", personnel.getCertName());
            row.put("certExpireDate", personnel.getCertExpireDate());
            row.put("daysLeft", java.time.temporal.ChronoUnit.DAYS.between(today, personnel.getCertExpireDate()));
            rows.add(row);
        }
        return rows;
    }

    public void deletePersonnel(Long id) {
        personnelMapper.deleteById(id);
    }
}
