package com.prop.wo.modules.export.service;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.prop.wo.common.context.LoginUserHolder;
import com.prop.wo.common.entity.PageQuery;
import com.prop.wo.common.exception.BizException;
import com.prop.wo.common.result.PageResult;
import com.prop.wo.common.storage.FileStorageService;
import com.prop.wo.common.util.PdfUtil;
import com.prop.wo.modules.contract.entity.Contract;
import com.prop.wo.modules.contract.service.ContractService;
import com.prop.wo.modules.equipment.entity.Equipment;
import com.prop.wo.modules.equipment.mapper.EquipmentMapper;
import com.prop.wo.modules.evidence.entity.EvidencePhoto;
import com.prop.wo.modules.evidence.mapper.EvidencePhotoMapper;
import com.prop.wo.modules.export.dto.RegulatoryExcelRow;
import com.prop.wo.modules.export.entity.ExportLog;
import com.prop.wo.modules.export.mapper.ExportLogMapper;
import com.prop.wo.modules.export.mapper.ExportQueryMapper;
import com.prop.wo.modules.inspection.dto.InspectionExcelRow;
import com.prop.wo.modules.inspection.service.InspectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 导出中心：监管报表、责任记录包、巡检记录、导出留痕
 */
@Service
@RequiredArgsConstructor
public class ExportService {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ExportQueryMapper queryMapper;
    private final ExportLogMapper exportLogMapper;
    private final ContractService contractService;
    private final EquipmentMapper equipmentMapper;
    private final EvidencePhotoMapper photoMapper;
    private final InspectionService inspectionService;
    private final FileStorageService fileStorageService;

    /** 监管报送 Excel：电梯维保记录 */
    public List<RegulatoryExcelRow> regulatoryRows() {
        List<Map<String, Object>> records = queryMapper.elevatorMaintenanceRecords();
        List<RegulatoryExcelRow> rows = new ArrayList<>();
        for (Map<String, Object> record : records) {
            RegulatoryExcelRow row = new RegulatoryExcelRow();
            row.setDeviceCode(str(record.get("deviceCode")));
            row.setDeviceName(str(record.get("deviceName")));
            row.setUseUnit(str(record.get("useUnit")));
            row.setMaintainUnit(str(record.get("maintainUnit")));
            row.setMaintainDate(format(record.get("maintainDate")));
            row.setWorkerName(str(record.get("workerName")));
            row.setConclusion("PASS".equals(str(record.get("acceptResult"))) ? "合格" : "已提交待验收");
            row.setNextMaintainDate(format(record.get("nextMaintainDate")));
            row.setSignPlace("（此处加盖维保单位公章）");
            rows.add(row);
        }
        return rows;
    }

    public byte[] regulatoryExcel() {
        List<RegulatoryExcelRow> rows = regulatoryRows();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        EasyExcel.write(out, RegulatoryExcelRow.class).sheet("电梯维保记录").doWrite(rows);
        recordLog("REGULATORY", "全部电梯维保记录", null, rows.size());
        return out.toByteArray();
    }

    /**
     * 责任记录包：近 6 个月工单履历 + 合同 PDF + 留痕照片索引 + 责任认定汇总 PDF，打包 zip
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> responsibilityPackage(Long equipmentId) {
        Equipment equipment = equipmentMapper.selectById(equipmentId);
        if (equipment == null) {
            throw new BizException("设备不存在");
        }
        LocalDateTime since = LocalDateTime.now().minusMonths(6);
        List<Map<String, Object>> history = queryMapper.equipmentHistory(equipmentId, since);
        List<Contract> contracts = contractService.listByEquipment(equipmentId, since);

        Map<String, byte[]> entries = new LinkedHashMap<>();
        entries.put("责任认定汇总.pdf", PdfUtil.render(summaryHtml(equipment, history, contracts)));
        entries.put("维保履历.xlsx", historyExcel(history));

        int index = 1;
        StringBuilder photoIndex = new StringBuilder("序号,工单ID,拍摄时间,作业人,设备,MD5,照片文件\n");
        List<Long> orderIds = new ArrayList<>();
        for (Map<String, Object> row : history) {
            if (row.get("orderId") != null) {
                orderIds.add(Long.parseLong(row.get("orderId").toString()));
            }
        }
        List<EvidencePhoto> photos = orderIds.isEmpty() ? List.of() : photoMapper.selectList(
                new LambdaQueryWrapper<EvidencePhoto>()
                        .in(EvidencePhoto::getOrderId, orderIds)
                        .orderByAsc(EvidencePhoto::getId));
        for (EvidencePhoto photo : photos) {
            if (!fileStorageService.exists(photo.getWatermarkPath())) {
                continue;
            }
            String name = "照片/" + String.format("%03d", index) + "_" + photo.getId() + ".jpg";
            entries.put(name, fileStorageService.read(photo.getWatermarkPath()));
            photoIndex.append(index).append(',').append(photo.getOrderId()).append(',')
                    .append(photo.getShootTime() == null ? "" : photo.getShootTime().format(TIME_FMT)).append(',')
                    .append(photo.getOperatorName()).append(',')
                    .append(photo.getEquipmentName() == null ? "" : photo.getEquipmentName()).append(',')
                    .append(photo.getMd5()).append(',').append(name).append('\n');
            index++;
        }
        entries.put("照片索引.csv", photoIndex.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));

        for (Contract contract : contracts) {
            byte[] pdf = contractService.readPdf(contract);
            entries.put("合同/" + contract.getContractNo() + ".pdf", pdf);
        }

        byte[] zip = zip(entries);
        String path = fileStorageService.save(zip, "export", "zip");
        recordLog("RESPONSIBILITY", "设备 " + equipment.getCode() + " 近6个月", path, history.size());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("path", path);
        result.put("fileName", equipment.getName() + "-责任记录包.zip");
        result.put("orderCount", history.size());
        result.put("contractCount", contracts.size());
        result.put("photoCount", index - 1);
        return result;
    }

    private String summaryHtml(Equipment equipment, List<Map<String, Object>> history, List<Contract> contracts) {
        StringBuilder sb = new StringBuilder();
        sb.append("<h1>设 备 责 任 认 定 记 录 汇 总</h1>");
        sb.append("<p class=\"muted\" style=\"text-align:center\">导出时间：")
                .append(LocalDateTime.now().format(TIME_FMT)).append("</p>");
        sb.append("<h2>一、设备档案</h2><table>")
                .append("<tr><th style=\"width:22%\">设备编号</th><td class=\"mono\">")
                .append(PdfUtil.escape(equipment.getCode())).append("</td></tr>")
                .append("<tr><th>设备名称</th><td>").append(PdfUtil.escape(equipment.getName())).append("</td></tr>")
                .append("<tr><th>安装位置</th><td>").append(PdfUtil.escape(equipment.getLocation())).append("</td></tr>")
                .append("<tr><th>运行状态</th><td>").append(PdfUtil.escape(equipment.getStatus())).append("</td></tr>")
                .append("</table>");
        sb.append("<h2>二、近 6 个月维保/维修履历</h2><table>")
                .append("<tr><th>工单号</th><th>业务类型</th><th>标题</th><th>完成时间</th><th>责任人</th><th>结论</th></tr>");
        for (Map<String, Object> row : history) {
            sb.append("<tr><td class=\"mono\">").append(PdfUtil.escape(str(row.get("orderNo"))))
                    .append("</td><td>").append(PdfUtil.escape(str(row.get("orderType"))))
                    .append("</td><td>").append(PdfUtil.escape(str(row.get("title"))))
                    .append("</td><td class=\"mono\">").append(format(row.get("finishTime")))
                    .append("</td><td>").append(PdfUtil.escape(str(row.get("workerName"))))
                    .append("</td><td>").append("ARCHIVED".equals(str(row.get("status"))) ? "已归档" : str(row.get("status")))
                    .append("</td></tr>");
        }
        sb.append("</table>");
        sb.append("<h2>三、责任周期标注</h2><table>")
                .append("<tr><th>合同编号</th><th>服务方</th><th>责任周期</th><th>起始</th><th>届满</th></tr>");
        for (Contract contract : contracts) {
            sb.append("<tr><td class=\"mono\">").append(PdfUtil.escape(contract.getContractNo()))
                    .append("</td><td>").append(PdfUtil.escape(contract.getPartyB()))
                    .append("</td><td>").append(contract.getResponsibilityDays()).append(" 天")
                    .append("</td><td class=\"mono\">")
                    .append(contract.getRespStart() == null ? "—" : contract.getRespStart().format(TIME_FMT))
                    .append("</td><td class=\"mono\">")
                    .append(contract.getRespEnd() == null ? "—" : contract.getRespEnd().format(TIME_FMT))
                    .append("</td></tr>");
        }
        sb.append("</table>");
        sb.append("<p class=\"muted\">说明：本记录包由物业运维工单管理平台自动生成，用于责任认定与迎检；照片含服务端时间戳水印与 MD5 校验码。</p>");
        return sb.toString();
    }

    private byte[] historyExcel(List<Map<String, Object>> history) {
        List<Map<Integer, String>> rows = new ArrayList<>();
        for (Map<String, Object> item : history) {
            Map<Integer, String> row = new LinkedHashMap<>();
            row.put(0, str(item.get("orderNo")));
            row.put(1, str(item.get("orderType")));
            row.put(2, str(item.get("title")));
            row.put(3, str(item.get("status")));
            row.put(4, format(item.get("finishTime")));
            row.put(5, str(item.get("workerName")));
            row.put(6, str(item.get("contractNo")));
            row.put(7, str(item.get("partyB")));
            row.put(8, format(item.get("respStart")));
            row.put(9, format(item.get("respEnd")));
            rows.add(row);
        }
        List<List<String>> head = List.of(
                List.of("工单号"), List.of("业务类型"), List.of("标题"), List.of("状态"), List.of("完成时间"),
                List.of("责任人"), List.of("合同编号"), List.of("服务方"), List.of("责任周期起"), List.of("责任周期止"));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        EasyExcel.write(out).head(head).sheet("维保履历").doWrite(rows);
        return out.toByteArray();
    }

    private byte[] zip(Map<String, byte[]> entries) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(out, java.nio.charset.StandardCharsets.UTF_8)) {
            for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
                zos.putNextEntry(new ZipEntry(entry.getKey()));
                zos.write(entry.getValue());
                zos.closeEntry();
            }
        } catch (Exception e) {
            throw new BizException("责任记录包生成失败：" + e.getMessage());
        }
        return out.toByteArray();
    }

    public List<InspectionExcelRow> inspectionRows(java.time.LocalDate start, java.time.LocalDate end) {
        return inspectionService.exportRows(start, end);
    }

    public void recordLog(String type, String params, String filePath, int count) {
        ExportLog log = new ExportLog();
        log.setExportType(type);
        log.setParams(params);
        log.setFilePath(filePath);
        log.setRecordCount(count);
        log.setOperatorId(LoginUserHolder.get() == null ? null : LoginUserHolder.userId());
        log.setOperatorName(LoginUserHolder.realName());
        log.setCreateTime(LocalDateTime.now());
        exportLogMapper.insert(log);
    }

    public PageResult<ExportLog> logs(PageQuery query) {
        Page<ExportLog> page = exportLogMapper.selectPage(new Page<>(query.safePageNum(), query.safePageSize()),
                new LambdaQueryWrapper<ExportLog>().orderByDesc(ExportLog::getId));
        return PageResult.of(page);
    }

    private String str(Object value) {
        return value == null ? "" : value.toString();
    }

    private String format(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof LocalDateTime time) {
            return time.format(TIME_FMT);
        }
        String text = value.toString();
        return text.length() > 19 ? text.substring(0, 19) : text;
    }
}
