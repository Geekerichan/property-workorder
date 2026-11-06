package com.prop.wo.modules.contract.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.prop.wo.common.constant.BizConst;
import com.prop.wo.common.context.LoginUserHolder;
import com.prop.wo.common.entity.PageQuery;
import com.prop.wo.common.exception.BizException;
import com.prop.wo.common.result.PageResult;
import com.prop.wo.common.storage.FileStorageService;
import com.prop.wo.common.util.OrderNoGenerator;
import com.prop.wo.common.util.PdfUtil;
import com.prop.wo.config.AppProperties;
import com.prop.wo.modules.basedata.entity.Company;
import com.prop.wo.modules.basedata.entity.Personnel;
import com.prop.wo.modules.basedata.mapper.CompanyMapper;
import com.prop.wo.modules.basedata.mapper.PersonnelMapper;
import com.prop.wo.modules.contract.entity.Contract;
import com.prop.wo.modules.contract.entity.ContractConfirm;
import com.prop.wo.modules.contract.entity.ResponsibilityLink;
import com.prop.wo.modules.contract.mapper.ContractConfirmMapper;
import com.prop.wo.modules.contract.mapper.ContractMapper;
import com.prop.wo.modules.contract.mapper.ResponsibilityLinkMapper;
import com.prop.wo.modules.system.entity.SysUser;
import com.prop.wo.modules.system.mapper.SysUserMapper;
import com.prop.wo.modules.system.service.MessageService;
import com.prop.wo.modules.workorder.entity.WorkOrder;
import com.prop.wo.modules.workorder.mapper.WorkOrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 电子合同与责任周期：生成、双方确认、责任链、到期提醒、PDF
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContractService {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ContractMapper contractMapper;
    private final ContractConfirmMapper confirmMapper;
    private final ResponsibilityLinkMapper linkMapper;
    private final OrderNoGenerator orderNoGenerator;
    private final CompanyMapper companyMapper;
    private final PersonnelMapper personnelMapper;
    private final SysUserMapper sysUserMapper;
    private final WorkOrderMapper workOrderMapper;
    private final MessageService messageService;
    private final FileStorageService fileStorageService;
    private final AppProperties appProperties;

    /**
     * 接单成功后自动生成单次电子合同（责任周期自工单完成时间起算）
     */
    @Transactional(rollbackFor = Exception.class)
    public Contract createForOrder(WorkOrder order, Personnel personnel) {
        Contract exists = contractMapper.selectOne(new LambdaQueryWrapper<Contract>()
                .eq(Contract::getOrderId, order.getId()).last("limit 1"));
        if (exists != null) {
            return exists;
        }
        Contract contract = new Contract();
        contract.setContractNo(orderNoGenerator.nextContractNo());
        contract.setOrderId(order.getId());
        contract.setEquipmentId(order.getEquipmentId());
        contract.setEquipmentName(order.getTitle());
        contract.setPartyA("物业服务中心");
        Long companyId = personnel == null ? null : personnel.getCompanyId();
        if (companyId == null) {
            companyId = order.getTeamId() == null ? null : order.getTeamId();
        }
        contract.setCompanyId(personnel == null ? null : personnel.getCompanyId());
        String partyB = "物业内部维修班组";
        if (personnel != null && personnel.getCompanyId() != null) {
            Company company = companyMapper.selectById(personnel.getCompanyId());
            partyB = company == null ? "维保服务单位" : company.getName();
        }
        contract.setPartyB(partyB);
        contract.setServiceContent("设备：" + order.getTitle() + "；工单号：" + order.getOrderNo()
                + "；服务内容：" + (order.getDescription() == null ? "按作业清单执行维保" : order.getDescription()));
        contract.setResponsibilityDays(appProperties.getResponsibilityDefaultDays());
        contract.setStatus(BizConst.CONTRACT_PENDING);
        contract.setRemindFlag(0);
        contractMapper.insert(contract);

        // 责任链：同一设备上一份合同与当前合同关联
        if (order.getEquipmentId() != null) {
            Contract prev = contractMapper.selectOne(new LambdaQueryWrapper<Contract>()
                    .eq(Contract::getEquipmentId, order.getEquipmentId())
                    .ne(Contract::getId, contract.getId())
                    .orderByDesc(Contract::getId).last("limit 1"));
            if (prev != null) {
                ResponsibilityLink link = new ResponsibilityLink();
                link.setEquipmentId(order.getEquipmentId());
                link.setPrevContractId(prev.getId());
                link.setNextContractId(contract.getId());
                link.setCreateTime(LocalDateTime.now());
                linkMapper.insert(link);
            }
        }
        order.setContractId(contract.getId());
        workOrderMapper.updateById(order);

        // 通知乙方确认
        Long workerUserId = personnel == null ? null : personnel.getUserId();
        if (workerUserId != null) {
            messageService.send(workerUserId, "CONTRACT", "待确认电子合同",
                    "工单 " + order.getOrderNo() + " 已生成电子合同，请确认责任周期条款", "CONTRACT", contract.getId());
        }
        return contract;
    }

    /** 工单完成：写入责任周期起止时间 */
    @Transactional(rollbackFor = Exception.class)
    public void onOrderFinished(WorkOrder order) {
        Contract contract = contractMapper.selectOne(new LambdaQueryWrapper<Contract>()
                .eq(Contract::getOrderId, order.getId()).last("limit 1"));
        if (contract == null) {
            return;
        }
        int days = contract.getResponsibilityDays() == null
                ? appProperties.getResponsibilityDefaultDays() : contract.getResponsibilityDays();
        LocalDateTime start = order.getFinishTime() == null ? LocalDateTime.now() : order.getFinishTime();
        contract.setRespStart(start);
        contract.setRespEnd(start.plusDays(days));
        contract.setRemindFlag(0);
        contractMapper.updateById(contract);

        order.setResponsibilityStart(contract.getRespStart());
        order.setResponsibilityEnd(contract.getRespEnd());
        workOrderMapper.updateById(order);
    }

    /** 双方平台内确认：A 物业方，B 接单方 */
    @Transactional(rollbackFor = Exception.class)
    public Contract confirm(Long contractId, String party, String remark) {
        Contract contract = contractMapper.selectById(contractId);
        if (contract == null) {
            throw new BizException("合同不存在");
        }
        String role = LoginUserHolder.role();
        if ("B".equals(party) && !("ADMIN".equals(role) || "WORKER".equals(role) || "COMPANY".equals(role))) {
            throw BizException.forbidden("当前账号无权确认合同");
        }
        if ("A".equals(party) && !"ADMIN".equals(role)) {
            throw BizException.forbidden("物业方合同确认仅管理员可操作");
        }
        if ("COMPANY".equals(role) && contract.getCompanyId() != null
                && !contract.getCompanyId().equals(LoginUserHolder.companyId())) {
            throw BizException.forbidden("无法确认其他公司的合同");
        }
        Long exists = confirmMapper.selectCount(new LambdaQueryWrapper<ContractConfirm>()
                .eq(ContractConfirm::getContractId, contractId).eq(ContractConfirm::getParty, party));
        if (exists != null && exists > 0) {
            throw new BizException("该方已确认，无需重复确认");
        }
        ContractConfirm confirm = new ContractConfirm();
        confirm.setContractId(contractId);
        confirm.setParty(party);
        confirm.setOperatorId(LoginUserHolder.userId());
        confirm.setOperatorName(LoginUserHolder.realName());
        confirm.setConfirmTime(LocalDateTime.now());
        confirm.setRemark(remark);
        confirmMapper.insert(confirm);

        if ("A".equals(party)) {
            contract.setSignTimeA(LocalDateTime.now());
            contract.setSignUserA(LoginUserHolder.realName());
        } else {
            contract.setSignTimeB(LocalDateTime.now());
            contract.setSignUserB(LoginUserHolder.realName());
        }
        if (contract.getSignTimeA() != null && contract.getSignTimeB() != null) {
            contract.setStatus(BizConst.CONTRACT_ACTIVE);
        }
        contractMapper.updateById(contract);
        return contract;
    }

    /** 强制刷新责任周期（工单完成但合同已在完成前确认的场景） */
    @Transactional(rollbackFor = Exception.class)
    public void refreshPeriod(Long contractId) {
        Contract contract = contractMapper.selectById(contractId);
        if (contract == null) {
            return;
        }
        WorkOrder order = workOrderMapper.selectById(contract.getOrderId());
        if (order != null && order.getFinishTime() != null) {
            int days = contract.getResponsibilityDays() == null
                    ? appProperties.getResponsibilityDefaultDays() : contract.getResponsibilityDays();
            contract.setRespStart(order.getFinishTime());
            contract.setRespEnd(order.getFinishTime().plusDays(days));
            contractMapper.updateById(contract);
            order.setResponsibilityStart(contract.getRespStart());
            order.setResponsibilityEnd(contract.getRespEnd());
            workOrderMapper.updateById(order);
        }
    }

    public PageResult<Map<String, Object>> page(String keyword, String status, Long companyId, PageQuery query) {
        LambdaQueryWrapper<Contract> wrapper = new LambdaQueryWrapper<Contract>()
                .eq(StringUtils.hasText(status), Contract::getStatus, status)
                .eq(companyId != null, Contract::getCompanyId, companyId)
                .and(StringUtils.hasText(keyword), w -> w.like(Contract::getContractNo, keyword)
                        .or().like(Contract::getPartyB, keyword).or().like(Contract::getEquipmentName, keyword))
                .orderByDesc(Contract::getId);
        Page<Contract> page = contractMapper.selectPage(new Page<>(query.safePageNum(), query.safePageSize()), wrapper);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Contract contract : page.getRecords()) {
            Map<String, Object> row = contractRow(contract);
            WorkOrder order = workOrderMapper.selectById(contract.getOrderId());
            row.put("orderNo", order == null ? null : order.getOrderNo());
            rows.add(row);
        }
        return PageResult.of(page.getTotal(), rows);
    }

    private Map<String, Object> contractRow(Contract contract) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", contract.getId());
        row.put("contractNo", contract.getContractNo());
        row.put("orderId", contract.getOrderId());
        row.put("equipmentId", contract.getEquipmentId());
        row.put("equipmentName", contract.getEquipmentName());
        row.put("partyA", contract.getPartyA());
        row.put("companyId", contract.getCompanyId());
        row.put("partyB", contract.getPartyB());
        row.put("serviceContent", contract.getServiceContent());
        row.put("responsibilityDays", contract.getResponsibilityDays());
        row.put("respStart", contract.getRespStart());
        row.put("respEnd", contract.getRespEnd());
        row.put("status", contract.getStatus());
        row.put("statusLabel", switch (contract.getStatus()) {
            case BizConst.CONTRACT_ACTIVE -> "已生效";
            case BizConst.CONTRACT_EXPIRED -> "已到期";
            default -> "待确认";
        });
        row.put("signTimeA", contract.getSignTimeA());
        row.put("signUserA", contract.getSignUserA());
        row.put("signTimeB", contract.getSignTimeB());
        row.put("signUserB", contract.getSignUserB());
        row.put("pdfPath", contract.getPdfPath());
        row.put("createTime", contract.getCreateTime());
        return row;
    }

    public Map<String, Object> detail(Long id) {
        Contract contract = contractMapper.selectById(id);
        if (contract == null) {
            throw new BizException("合同不存在");
        }
        if ("COMPANY".equals(LoginUserHolder.role()) && contract.getCompanyId() != null
                && !contract.getCompanyId().equals(LoginUserHolder.companyId())) {
            throw BizException.forbidden("无权查看其他公司的合同");
        }
        Map<String, Object> row = contractRow(contract);
        WorkOrder order = workOrderMapper.selectById(contract.getOrderId());
        row.put("order", order);
        row.put("confirms", confirmMapper.selectList(new LambdaQueryWrapper<ContractConfirm>()
                .eq(ContractConfirm::getContractId, id).orderByAsc(ContractConfirm::getId)));
        row.put("chain", responsibilityChain(contract.getEquipmentId()));
        return row;
    }

    /** 责任链视图：同一设备相邻合同 */
    public List<Map<String, Object>> responsibilityChain(Long equipmentId) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (equipmentId == null) {
            return result;
        }
        List<Contract> contracts = contractMapper.selectList(new LambdaQueryWrapper<Contract>()
                .eq(Contract::getEquipmentId, equipmentId).orderByAsc(Contract::getId));
        Contract prev = null;
        for (Contract contract : contracts) {
            Map<String, Object> node = contractRow(contract);
            node.put("prevContractId", prev == null ? null : prev.getId());
            node.put("prevPartyB", prev == null ? null : prev.getPartyB());
            result.add(node);
            prev = contract;
        }
        return result;
    }

    /** 生成合同 PDF（含责任周期条款与双方确认时间戳） */
    @Transactional(rollbackFor = Exception.class)
    public Contract generatePdf(Long id) {
        Contract contract = contractMapper.selectById(id);
        if (contract == null) {
            throw new BizException("合同不存在");
        }
        String html = buildHtml(contract);
        byte[] pdf = PdfUtil.render(html);
        String path = fileStorageService.save(pdf, "contract", "pdf");
        contract.setPdfPath(path);
        contractMapper.updateById(contract);
        return contract;
    }

    private String buildHtml(Contract contract) {
        WorkOrder order = workOrderMapper.selectById(contract.getOrderId());
        String equipmentName = contract.getEquipmentName();
        StringBuilder sb = new StringBuilder();
        sb.append("<h1>设 备 维 保 服 务 电 子 合 同</h1>");
        sb.append("<p class=\"muted\" style=\"text-align:center\">合同编号：<span class=\"mono\">")
                .append(PdfUtil.escape(contract.getContractNo())).append("</span></p>");
        sb.append("<h2>一、合同双方</h2><table>")
                .append("<tr><th style=\"width:22%\">甲方（物业方）</th><td>").append(PdfUtil.escape(contract.getPartyA())).append("</td></tr>")
                .append("<tr><th>乙方（服务方）</th><td>").append(PdfUtil.escape(contract.getPartyB())).append("</td></tr>")
                .append("</table>");
        sb.append("<h2>二、服务内容</h2><table>")
                .append("<tr><th style=\"width:22%\">关联工单</th><td class=\"mono\">")
                .append(PdfUtil.escape(order == null ? "" : order.getOrderNo())).append("</td></tr>")
                .append("<tr><th>设备</th><td>").append(PdfUtil.escape(equipmentName)).append("</td></tr>")
                .append("<tr><th>服务说明</th><td>").append(PdfUtil.escape(contract.getServiceContent())).append("</td></tr>")
                .append("</table>");
        sb.append("<h2>三、责任周期条款</h2><table>")
                .append("<tr><th style=\"width:22%\">责任周期</th><td>").append(contract.getResponsibilityDays()).append(" 天</td></tr>")
                .append("<tr><th>起算时间</th><td class=\"mono\">")
                .append(contract.getRespStart() == null ? "工单完成时间起算" : contract.getRespStart().format(TIME_FMT))
                .append("</td></tr>")
                .append("<tr><th>届满时间</th><td class=\"mono\">")
                .append(contract.getRespEnd() == null ? "待工单完成后确定" : contract.getRespEnd().format(TIME_FMT))
                .append("</td></tr>")
                .append("<tr><th>条款</th><td>责任周期内因本次维保质量导致的设备故障，由乙方承担免费返修与相应责任；"
                        + "同一设备相邻维保合同相互关联，形成责任链。</td></tr>")
                .append("</table>");
        sb.append("<h2>四、双方确认</h2><table>")
                .append("<tr><th style=\"width:22%\">甲方确认</th><td>")
                .append(contract.getSignTimeA() == null ? "待确认" : PdfUtil.escape(contract.getSignUserA()) + "　"
                        + contract.getSignTimeA().format(TIME_FMT)).append("</td></tr>")
                .append("<tr><th>乙方确认</th><td>")
                .append(contract.getSignTimeB() == null ? "待确认" : PdfUtil.escape(contract.getSignUserB()) + "　"
                        + contract.getSignTimeB().format(TIME_FMT)).append("</td></tr>")
                .append("</table>");
        sb.append("<table class=\"sign\"><tr>")
                .append("<td>甲方（签章）：__________________</td>")
                .append("<td>乙方（签章）：__________________</td></tr>")
                .append("<tr><td>生成时间：").append(LocalDateTime.now().format(TIME_FMT)).append("</td>")
                .append("<td class=\"muted\">本文件由物业运维工单管理平台生成</td></tr></table>");
        return sb.toString();
    }

    public byte[] pdfBytes(Long id) {
        Contract contract = contractMapper.selectById(id);
        if (contract == null) {
            throw new BizException("合同不存在");
        }
        if (StringUtils.hasText(contract.getPdfPath()) && fileStorageService.exists(contract.getPdfPath())) {
            return fileStorageService.read(contract.getPdfPath());
        }
        Contract generated = generatePdf(id);
        return fileStorageService.read(generated.getPdfPath());
    }

    /** 到期提醒：届满前 7 天通知双方，届满后置为已到期 */
    @Scheduled(cron = "0 15 1 * * ?")
    public void expireScan() {
        LocalDateTime now = LocalDateTime.now();
        List<Contract> contracts = contractMapper.selectList(new LambdaQueryWrapper<Contract>()
                .isNotNull(Contract::getRespEnd)
                .in(Contract::getStatus, BizConst.CONTRACT_PENDING, BizConst.CONTRACT_ACTIVE));
        int remindDays = appProperties.getResponsibilityRemindBeforeDays();
        for (Contract contract : contracts) {
            if (contract.getRespEnd().isBefore(now)) {
                contract.setStatus(BizConst.CONTRACT_EXPIRED);
                contractMapper.updateById(contract);
                continue;
            }
            if (!Integer.valueOf(1).equals(contract.getRemindFlag())
                    && !contract.getRespEnd().isAfter(now.plusDays(remindDays))) {
                contract.setRemindFlag(1);
                contractMapper.updateById(contract);
                List<SysUser> admins = sysUserMapper.selectList(new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getRoleCode, "ADMIN"));
                admins.forEach(admin -> messageService.send(admin.getId(), "CONTRACT", "责任周期即将届满",
                        "合同 " + contract.getContractNo() + " 责任周期将于 " + contract.getRespEnd() + " 届满",
                        "CONTRACT", contract.getId()));
                WorkOrder order = workOrderMapper.selectById(contract.getOrderId());
                if (order != null && order.getMainWorkerId() != null) {
                    Personnel personnel = personnelMapper.selectById(order.getMainWorkerId());
                    if (personnel != null && personnel.getUserId() != null) {
                        messageService.send(personnel.getUserId(), "CONTRACT", "责任周期即将届满",
                                "合同 " + contract.getContractNo() + " 责任周期将于 " + contract.getRespEnd() + " 届满",
                                "CONTRACT", contract.getId());
                    }
                }
            }
        }
    }

    public Contract byOrder(Long orderId) {
        return contractMapper.selectOne(new LambdaQueryWrapper<Contract>()
                .eq(Contract::getOrderId, orderId).last("limit 1"));
    }

    /** 责任记录包用：设备近 6 个月合同 */
    public List<Contract> listByEquipment(Long equipmentId, LocalDateTime since) {
        return contractMapper.selectList(new LambdaQueryWrapper<Contract>()
                .eq(Contract::getEquipmentId, equipmentId)
                .ge(since != null, Contract::getCreateTime, since)
                .orderByAsc(Contract::getId));
    }

    public byte[] readPdf(Contract contract) {
        if (contract.getPdfPath() != null && fileStorageService.exists(contract.getPdfPath())) {
            return fileStorageService.read(contract.getPdfPath());
        }
        return PdfUtil.render(buildHtml(contract));
    }

    public Path pdfPath(Contract contract) {
        return fileStorageService.resolve(contract.getPdfPath());
    }

    public boolean pdfExists(Contract contract) {
        return contract.getPdfPath() != null && Files.exists(pdfPath(contract));
    }
}
