package com.prop.wo.modules.basedata.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.prop.wo.common.exception.BizException;
import com.prop.wo.modules.basedata.entity.Company;
import com.prop.wo.modules.basedata.entity.CompanyQualification;
import com.prop.wo.modules.basedata.mapper.CompanyMapper;
import com.prop.wo.modules.basedata.mapper.CompanyQualificationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 维保公司与资质台账
 */
@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyMapper companyMapper;
    private final CompanyQualificationMapper qualificationMapper;

    public List<Map<String, Object>> list(Long onlyCompanyId, String keyword) {
        LambdaQueryWrapper<Company> wrapper = new LambdaQueryWrapper<Company>()
                .eq(onlyCompanyId != null, Company::getId, onlyCompanyId)
                .like(StringUtils.hasText(keyword), Company::getName, keyword)
                .orderByAsc(Company::getId);
        List<Company> companies = companyMapper.selectList(wrapper);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Company company : companies) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", company.getId());
            row.put("name", company.getName());
            row.put("creditCode", company.getCreditCode());
            row.put("contactName", company.getContactName());
            row.put("contactPhone", company.getContactPhone());
            row.put("address", company.getAddress());
            row.put("status", company.getStatus());
            row.put("remark", company.getRemark());
            row.put("qualifications", qualifications(company.getId()));
            rows.add(row);
        }
        return rows;
    }

    public Company save(Company company) {
        if (!StringUtils.hasText(company.getName())) {
            throw new BizException("请输入公司名称");
        }
        if (company.getId() == null) {
            company.setStatus(company.getStatus() == null ? "ENABLED" : company.getStatus());
            companyMapper.insert(company);
        } else {
            companyMapper.updateById(company);
        }
        return company;
    }

    public void delete(Long id) {
        companyMapper.deleteById(id);
    }

    public List<CompanyQualification> qualifications(Long companyId) {
        return qualificationMapper.selectList(new LambdaQueryWrapper<CompanyQualification>()
                .eq(CompanyQualification::getCompanyId, companyId)
                .orderByAsc(CompanyQualification::getExpireDate));
    }

    public CompanyQualification saveQualification(CompanyQualification qualification) {
        if (qualification.getCompanyId() == null || !StringUtils.hasText(qualification.getQualType())) {
            throw new BizException("请选择公司并填写资质类型");
        }
        if (qualification.getId() == null) {
            qualificationMapper.insert(qualification);
        } else {
            qualificationMapper.updateById(qualification);
        }
        return qualification;
    }

    public void deleteQualification(Long id) {
        qualificationMapper.deleteById(id);
    }

    /** 资质临期（30 天内）与过期清单 */
    public List<Map<String, Object>> expiringQualifications() {
        List<CompanyQualification> list = qualificationMapper.selectList(
                new LambdaQueryWrapper<CompanyQualification>().isNotNull(CompanyQualification::getExpireDate)
                        .le(CompanyQualification::getExpireDate, java.time.LocalDate.now().plusDays(30))
                        .orderByAsc(CompanyQualification::getExpireDate));
        Map<Long, String> names = new LinkedHashMap<>();
        companyMapper.selectList(null).forEach(c -> names.put(c.getId(), c.getName()));
        List<Map<String, Object>> rows = new ArrayList<>();
        for (CompanyQualification qualification : list) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", qualification.getId());
            row.put("companyId", qualification.getCompanyId());
            row.put("companyName", names.get(qualification.getCompanyId()));
            row.put("qualType", qualification.getQualType());
            row.put("certNo", qualification.getCertNo());
            row.put("expireDate", qualification.getExpireDate());
            row.put("daysLeft", java.time.temporal.ChronoUnit.DAYS.between(
                    java.time.LocalDate.now(), qualification.getExpireDate()));
            rows.add(row);
        }
        return rows;
    }
}
