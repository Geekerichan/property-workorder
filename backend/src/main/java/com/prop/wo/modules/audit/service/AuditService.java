package com.prop.wo.modules.audit.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.prop.wo.common.entity.PageQuery;
import com.prop.wo.common.result.PageResult;
import com.prop.wo.modules.audit.entity.AcceptanceRecord;
import com.prop.wo.modules.audit.entity.CreditRecord;
import com.prop.wo.modules.audit.mapper.AcceptanceRecordMapper;
import com.prop.wo.modules.audit.mapper.CreditRecordMapper;
import com.prop.wo.modules.basedata.entity.Company;
import com.prop.wo.modules.basedata.entity.Personnel;
import com.prop.wo.modules.basedata.mapper.CompanyMapper;
import com.prop.wo.modules.basedata.mapper.PersonnelMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 验收记录与信用记录查询（写入在工单流转中完成）
 */
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AcceptanceRecordMapper acceptanceMapper;
    private final CreditRecordMapper creditMapper;
    private final PersonnelMapper personnelMapper;
    private final CompanyMapper companyMapper;

    public PageResult<AcceptanceRecord> acceptancePage(String result, PageQuery query) {
        Page<AcceptanceRecord> page = acceptanceMapper.selectPage(
                new Page<>(query.safePageNum(), query.safePageSize()),
                new LambdaQueryWrapper<AcceptanceRecord>()
                        .eq(StringUtils.hasText(result), AcceptanceRecord::getResult, result)
                        .orderByDesc(AcceptanceRecord::getId));
        return PageResult.of(page);
    }

    public PageResult<Map<String, Object>> creditPage(String targetType, Long targetId, PageQuery query) {
        Page<CreditRecord> page = creditMapper.selectPage(new Page<>(query.safePageNum(), query.safePageSize()),
                new LambdaQueryWrapper<CreditRecord>()
                        .eq(StringUtils.hasText(targetType), CreditRecord::getTargetType, targetType)
                        .eq(targetId != null, CreditRecord::getTargetId, targetId)
                        .orderByDesc(CreditRecord::getId));
        List<Map<String, Object>> rows = new ArrayList<>();
        for (CreditRecord record : page.getRecords()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", record.getId());
            row.put("targetType", record.getTargetType());
            row.put("targetId", record.getTargetId());
            row.put("targetName", record.getTargetName());
            row.put("orderId", record.getOrderId());
            row.put("creditType", record.getCreditType());
            row.put("score", record.getScore());
            row.put("reason", record.getReason());
            row.put("operatorName", record.getOperatorName());
            row.put("createTime", record.getCreateTime());
            rows.add(row);
        }
        return PageResult.of(page.getTotal(), rows);
    }

    /** 信用汇总：按人员/公司聚合评价均分与扣分总数 */
    public List<Map<String, Object>> creditSummary(String targetType) {
        List<CreditRecord> records = creditMapper.selectList(new LambdaQueryWrapper<CreditRecord>()
                .eq(StringUtils.hasText(targetType), CreditRecord::getTargetType, targetType));
        Map<String, Map<String, Object>> grouped = new LinkedHashMap<>();
        for (CreditRecord record : records) {
            String key = record.getTargetType() + ":" + record.getTargetId();
            Map<String, Object> row = grouped.computeIfAbsent(key, k -> {
                Map<String, Object> node = new LinkedHashMap<>();
                node.put("targetType", record.getTargetType());
                node.put("targetId", record.getTargetId());
                node.put("targetName", resolveName(record));
                node.put("evaluateCount", 0);
                node.put("evaluateSum", 0);
                node.put("deductScore", 0);
                node.put("deductCount", 0);
                return node;
            });
            if ("DEDUCT".equals(record.getCreditType()) || record.getScore() < 0) {
                row.put("deductScore", (int) row.get("deductScore") + Math.abs(record.getScore()));
                row.put("deductCount", (int) row.get("deductCount") + 1);
            } else {
                row.put("evaluateCount", (int) row.get("evaluateCount") + 1);
                row.put("evaluateSum", (int) row.get("evaluateSum") + record.getScore());
            }
        }
        List<Map<String, Object>> rows = new ArrayList<>(grouped.values());
        for (Map<String, Object> row : rows) {
            int count = (int) row.get("evaluateCount");
            int sum = (int) row.get("evaluateSum");
            row.put("avgScore", count == 0 ? 0 : Math.round(sum * 10.0 / count) / 10.0);
        }
        rows.sort((a, b) -> Integer.compare((int) b.get("deductScore"), (int) a.get("deductScore")));
        return rows;
    }

    private String resolveName(CreditRecord record) {
        if (StringUtils.hasText(record.getTargetName())) {
            return record.getTargetName();
        }
        if ("PERSONNEL".equals(record.getTargetType())) {
            Personnel personnel = personnelMapper.selectById(record.getTargetId());
            return personnel == null ? "—" : personnel.getName();
        }
        Company company = companyMapper.selectById(record.getTargetId());
        return company == null ? "—" : company.getName();
    }
}
