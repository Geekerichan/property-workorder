-- =============================================================
-- 物业公司运维工单管理系统 - 演示数据
-- 执行顺序：先 init.sql，再 data.sql
-- 账号：admin/admin123（管理员）、worker/wk123、worker2/wk123、worker3/wk123（作业人员）
--       company/cp123（维保公司管理员）、owner/ow123（业主，PC 演示）
-- 业主端 H5 登录：13800000001 ~ 13800000008，验证码 123456
-- =============================================================
USE `prop_wo`;
SET NAMES utf8mb4;

-- ---------------- 用户与公司 ----------------
INSERT INTO sys_user (id, username, password, real_name, phone, role_code, company_id, personnel_id, status, remark) VALUES
(1, 'admin', '$2a$10$IrDB9hWB3TVT65LS5jVNPe0hKeMMMLGaL8vv14SrAi.MZ0vZ7QP1y', '周工', '13900000001', 'ADMIN', NULL, NULL, 'ENABLED', '物业管理员，兼派单与验收'),
(2, 'worker', '$2a$10$klRHzszvk9m84Z5UHUqgeu1DUmrdYdA9sMlA.PanPvhlI1.CzAEOe', '张伟', '13900000002', 'WORKER', 1, 1, 'ENABLED', '电梯维保技师'),
(3, 'worker2', '$2a$10$klRHzszvk9m84Z5UHUqgeu1DUmrdYdA9sMlA.PanPvhlI1.CzAEOe', '李强', '13900000003', 'WORKER', 2, 2, 'ENABLED', '水电维修技师'),
(4, 'worker3', '$2a$10$klRHzszvk9m84Z5UHUqgeu1DUmrdYdA9sMlA.PanPvhlI1.CzAEOe', '王敏', '13900000004', 'WORKER', 1, 3, 'ENABLED', '电梯巡检员'),
(5, 'company', '$2a$10$yF8z0CoJp2pen0xqh2EEhuYLXIb4byISy93UWCD9/CkPqXa.ydjqW', '恒安电梯-刘经理', '13900000005', 'COMPANY', 1, NULL, 'ENABLED', '维保公司管理员'),
(6, 'owner', '$2a$10$GPwC15DSDxeXRxR6Id2RZuQPymg6cqPSIm225Ilb2pwOd1Ybggk9y', '陈建国', '13800000001', 'OWNER', NULL, NULL, 'ENABLED', '业主 PC 演示账号');

INSERT INTO company (id, name, credit_code, contact_name, contact_phone, address, status, remark) VALUES
(1, '恒安电梯工程有限公司', '91110108MA01AB2C3D', '刘志强', '13900000005', '北京市海淀区学院路 18 号', 'ENABLED', '电梯维保 A 级资质'),
(2, '蓝海机电设备服务有限公司', '91110105MA02EF4G5H', '孙海', '13900000006', '北京市朝阳区安慧里 3 号', 'ENABLED', '给排水与供配电维保');

INSERT INTO company_qualification (company_id, qual_type, cert_no, expire_date, remark) VALUES
(1, '电梯安装改造修理许可证（A 级）', 'TS3311001-2027', DATE_ADD(CURDATE(), INTERVAL 45 DAY), '临期需办理换证'),
(2, '机电设备安装维修资质（三级）', 'JD2024-0871', DATE_ADD(CURDATE(), INTERVAL 300 DAY), '');

INSERT INTO team (id, name, team_type, community_id, leader_id, remark) VALUES
(1, '电梯维保组', '电梯', 1, 1, '负责电梯半月保与应急处置'),
(2, '水电维修组', '水电', 1, 2, '负责给排水与供配电'),
(3, '综合巡检组', '综合', 1, 3, '负责公共区域巡检');

INSERT INTO personnel (id, name, phone, team_id, company_id, user_id, skills, cert_name, cert_no, cert_expire_date, duty_status, remark) VALUES
(1, '张伟', '13900000002', 1, 1, 2, '电梯,曳引系统,门系统', '电梯作业证', 'DT-2024-1183', DATE_ADD(CURDATE(), INTERVAL 210 DAY), 'ON_DUTY', ''),
(2, '李强', '13900000003', 2, 2, 3, '给排水,供配电,焊工', '电工证', 'DG-2023-0921', DATE_ADD(CURDATE(), INTERVAL 25 DAY), 'ON_DUTY', '电工证临期'),
(3, '王敏', '13900000004', 3, 1, 4, '巡检,电梯,安防', '电梯作业证', 'DT-2024-1207', DATE_ADD(CURDATE(), INTERVAL 400 DAY), 'ON_DUTY', ''),
(4, '赵磊', '13900000007', 2, NULL, NULL, '给排水,园林', '电工证', 'DG-2022-0455', DATE_SUB(CURDATE(), INTERVAL 10 DAY), 'LEAVE', '资质已过期，请假中');

-- ---------------- 小区 / 楼栋 / 单元 / 房屋 ----------------
INSERT INTO community (id, name, address, contact_name, contact_phone, build_year, remark) VALUES
(1, '锦和家园', '北京市海淀区锦和路 66 号', '周工', '13900000001', '2012', '3 栋 6 个单元，共 72 户');

INSERT INTO building (id, community_id, name, floors, unit_count, remark) VALUES
(1, 1, '1号楼', 6, 2, '临近南门，含电梯与水泵房'),
(2, 1, '2号楼', 6, 2, ''),
(3, 1, '3号楼', 6, 2, '');

INSERT INTO unit (id, community_id, building_id, name, floors, remark) VALUES
(1, 1, 1, '1单元', 6, ''),
(2, 1, 1, '2单元', 6, ''),
(3, 1, 2, '1单元', 6, ''),
(4, 1, 2, '2单元', 6, ''),
(5, 1, 3, '1单元', 6, ''),
(6, 1, 3, '2单元', 6, '');

INSERT INTO house (community_id, building_id, unit_id, room_no, floor_no, area, house_type, status, remark)
SELECT 1, u.building_id, u.id,
       CONCAT(u.building_id, '-', LEFT(u.name, 1), '-', f.n, LPAD(d.d, 2, '0')),
       f.n, ROUND(86 + f.n * 1.8, 2), '住宅', 'NORMAL', '档案初始化'
FROM unit u
JOIN (SELECT 1 AS n UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6) f
JOIN (SELECT 1 AS d UNION ALL SELECT 2) d
ORDER BY u.id, f.n, d.d;

INSERT INTO owner_user (id, phone, name, remark) VALUES
(1, '13800000001', '陈建国', '1号楼1单元101 主业主'),
(2, '13800000002', '陈小雨', '陈建国女儿，同住'),
(3, '13800000003', '刘慧', '1号楼1单元102'),
(4, '13800000004', '周明', '1号楼2单元101'),
(5, '13800000005', '吴芳', '2号楼1单元101'),
(6, '13800000006', '郑海', '2号楼2单元401'),
(7, '13800000007', '孙丽', '3号楼1单元201'),
(8, '13800000008', '马超', '3号楼2单元501');

INSERT INTO house_owner (house_id, owner_id, relation, is_primary, remark) VALUES
(1, 1, 'OWNER', 1, '一房多业主样例'),
(1, 2, 'FAMILY', 0, '一房多业主样例'),
(2, 3, 'OWNER', 1, ''),
(13, 4, 'OWNER', 1, ''),
(25, 5, 'OWNER', 1, ''),
(40, 6, 'OWNER', 1, ''),
(50, 7, 'OWNER', 1, ''),
(60, 8, 'OWNER', 1, '');

-- ---------------- 设备台账（一物一档） ----------------
INSERT INTO equipment (id, code, name, facility_type, community_id, building_id, location, commission_date, status, maintain_overdue, company_id, safety_manager, safety_phone, next_annual_date, longitude, latitude, remark) VALUES
(1, 'DT-1-01', '1号楼1单元电梯（注册代码 31101101082012070001）', 'ELEVATOR', 1, 1, '1号楼1单元电梯井道', '2012-06-18', 'NORMAL', 0, 1, '周工', '13900000001', DATE_ADD(CURDATE(), INTERVAL 16 DAY), 116.404512, 39.915233, '12 层站 2 台主机'),
(2, 'DT-1-02', '1号楼2单元电梯（注册代码 31101101082012070002）', 'ELEVATOR', 1, 2, '1号楼2单元电梯井道', '2012-06-18', 'NORMAL', 1, 1, '周工', '13900000001', DATE_ADD(CURDATE(), INTERVAL 210 DAY), 116.404601, 39.915298, '维保逾期红牌样例'),
(3, 'SB-01', '地下水泵房生活水泵', 'WATER', 1, 1, '1号楼地下二层水泵房', '2012-06-30', 'NORMAL', 0, 2, '李强', '13900000003', NULL, 116.404488, 39.915190, '一用一备，扬程 60m'),
(4, 'SX-01', '1号楼屋顶生活水箱', 'WATER', 1, 1, '1号楼屋顶水箱间', '2012-06-30', 'NORMAL', 0, 2, '李强', '13900000003', NULL, 116.404530, 39.915260, '不锈钢 30m³'),
(5, 'FM-01', '1号楼东侧给水阀门井', 'WATER', 1, 1, '1号楼东侧绿化带阀门井', '2012-07-05', 'NORMAL', 0, 2, '李强', '13900000003', NULL, 116.404700, 39.915210, 'DN100 闸阀'),
(6, 'PD-01', '1号楼配电室', 'POWER', 1, 1, '1号楼地下一层配电室', '2012-06-20', 'NORMAL', 0, 2, '周工', '13900000001', NULL, 116.404455, 39.915175, '含总配电柜与计量柜'),
(7, 'PDG-01', '1号楼总配电柜', 'POWER', 1, 1, '1号楼地下一层配电室', '2012-06-20', 'NORMAL', 0, 2, '周工', '13900000001', NULL, 116.404458, 39.915180, '630A 母排'),
(8, 'ZM-01', '地下车库公共照明回路', 'POWER', 1, 1, '地下车库 B 区照明配电箱', '2013-03-12', 'NORMAL', 0, 2, '李强', '13900000003', NULL, 116.404430, 39.915120, '18 回路'),
(9, 'KT-01', '1号楼空调机房', 'HVAC', 1, 1, '1号楼屋顶空调机房', '2015-05-20', 'REPAIR', 0, 2, '周工', '13900000001', NULL, 116.404560, 39.915300, '主机异响待检修'),
(10, 'JK-01', '1号楼大厅监控点位', 'SECURITY', 1, 1, '1号楼一层大堂', '2019-09-01', 'NORMAL', 0, NULL, '周工', '13900000001', NULL, 116.404520, 39.915240, '录像保留 30 天'),
(11, 'LH-01', '中心花园绿化区', 'GARDEN', 1, NULL, '小区中心花园', '2012-08-01', 'NORMAL', 0, NULL, '王敏', '13900000004', NULL, 116.404580, 39.915150, '含自动灌溉与盆景'),
(12, 'XF-01', '1号楼消防水泵', 'FIRE', 1, 1, '1号楼地下二层消防泵房', '2012-06-30', 'NORMAL', 0, NULL, '周工', '13900000001', NULL, 116.404470, 39.915185, '消防数据仅合规隔离，不对外公开');

INSERT INTO equipment_ext (equipment_id, ext_json) VALUES
(1, '{"注册代码":"31101101082012070001","品牌型号":"通力 KONE-3000","额定载重":"1000kg","额定速度":"1.75m/s","层站数":"12层12站"}'),
(2, '{"注册代码":"31101101082012070002","品牌型号":"通力 KONE-3000","额定载重":"1000kg","额定速度":"1.75m/s","层站数":"12层12站"}'),
(3, '{"水泵型号":"CDL32-60","功率":"15kW","扬程":"60m","台数":"2"}'),
(4, '{"容积":"30m³","材质":"304不锈钢","清洗周期":"半年","最近清洗日期":"2026-05-12","水质检测日期":"2026-05-13"}'),
(5, '{"管径":"DN100","材质":"球墨铸铁","埋深":"1.2m","阀门型号":"Z45X-16"}'),
(6, '{"配电柜编号":"PD-01","型号":"GGD2","额定参数":"400V/630A"}'),
(7, '{"开关型号":"CW3-630","额定电流":"630A","脱扣曲线":"C","试验日期":"2026-03-18","接地电阻":"1.8Ω"}'),
(8, '{"照明回路":"18路","控制方式":"时控+光控"}'),
(9, '{"主机型号":"约克 YVAG-130","制冷量":"130kW","冷媒":"R134a","冷却塔":"2台"}'),
(10, '{"摄像头点位":"4路","分辨率":"1080P","存储策略":"NVR 30天"}'),
(11, '{"植物种类":"桂花/香樟/麦冬","数量":"86株","灌溉系统":"自动喷灌","盆景":"无"}'),
(12, '{"消防泵型号":"XBD-40","流量":"40L/s","扬程":"80m"}');

-- ---------------- 作业清单模板 ----------------
INSERT INTO checklist_template (id, name, facility_type, status, remark) VALUES
(1, '电梯半月保作业清单', 'ELEVATOR', 'ENABLED', '含制动器、限速器、门系统强制拍照项'),
(2, '给排水设施保养清单', 'WATER', 'ENABLED', '含水泵机械密封、水箱人孔、阀门井强制拍照项'),
(3, '供配电设施保养清单', 'POWER', 'ENABLED', '含母排连接、开关触头、接地电阻测试强制拍照项'),
(4, '通用设施作业清单', 'GARDEN', 'ENABLED', '安防/暖通/绿化等通用作业');

INSERT INTO checklist_item (template_id, group_name, item_name, require_photo, require_value, unit, sort_no) VALUES
(1, '机房', '制动器间隙检查', 1, 1, 'mm', 1),
(1, '机房', '限速器动作试验', 1, 0, NULL, 2),
(1, '机房', '曳引机运行声音与温升', 0, 1, '℃', 3),
(1, '轿厢', '轿厢照明与应急照明', 0, 0, NULL, 4),
(1, '轿厢', '门系统开关门试验', 1, 0, NULL, 5),
(1, '井道', '导轨润滑与导靴磨损', 0, 0, NULL, 6),
(1, '井道', '钢丝绳断丝与张力', 0, 0, NULL, 7),
(1, '层站', '层门门锁与地坎清洁', 0, 0, NULL, 8),
(1, '作业完成', '运行试验与现场清理', 1, 0, NULL, 9),
(2, '水泵', '水泵机械密封检查', 1, 0, NULL, 1),
(2, '水泵', '运行电流与出口压力', 0, 1, 'MPa', 2),
(2, '水箱', '水箱人孔密封与防虫网', 1, 0, NULL, 3),
(2, '水箱', '水质余氯检测', 0, 1, 'mg/L', 4),
(2, '阀门', '阀门井积水与阀门启闭', 1, 0, NULL, 5),
(2, '管网', '管道保温与渗漏检查', 0, 0, NULL, 6),
(3, '配电室', '母排连接螺栓紧固', 1, 0, NULL, 1),
(3, '配电柜', '开关触头与灭弧罩', 1, 0, NULL, 2),
(3, '配电柜', '接地电阻测试', 1, 1, 'Ω', 3),
(3, '备用电源', '发电机自启动试验', 0, 0, NULL, 4),
(3, '作业完成', '柜内除尘与现场清理', 0, 0, NULL, 5),
(4, '现场确认', '设备运行状态检查', 0, 0, NULL, 1),
(4, '作业过程', '作业内容执行并记录', 0, 0, NULL, 2),
(4, '作业完成', '功能复位与现场清理', 1, 0, NULL, 3);

-- ---------------- 维保计划与季节节点 ----------------
INSERT INTO maintenance_plan (id, name, facility_type, mode, period_days, interval_days, advance_days, order_type, dispatch_mode, template_id, seasonal_flag, next_generate_date, last_generate_date, status, remark) VALUES
(1, '电梯半月保（15天）', 'ELEVATOR', 'PERIODIC', 15, NULL, 3, 'PLAN_MAINTAIN', 'GRAB', 1, 0, DATE_ADD(CURDATE(), INTERVAL 3 DAY), DATE_SUB(CURDATE(), INTERVAL 12 DAY), 'ENABLED', '电梯法定半月保'),
(2, '生活水泵按需保养', 'WATER', 'ON_DEMAND', NULL, 60, 5, 'DEMAND_MAINTAIN', 'ASSIGN', 2, 0, DATE_ADD(CURDATE(), INTERVAL 20 DAY), DATE_SUB(CURDATE(), INTERVAL 40 DAY), 'ENABLED', '工程主管设定 60 天间隔'),
(3, '供配电季度保养', 'POWER', 'PERIODIC', 90, NULL, 7, 'PLAN_MAINTAIN', 'ASSIGN', 3, 0, DATE_ADD(CURDATE(), INTERVAL 15 DAY), DATE_SUB(CURDATE(), INTERVAL 75 DAY), 'ENABLED', '季度除尘与接地测试');

INSERT INTO plan_device_rel (plan_id, equipment_id) VALUES
(1, 1), (1, 2), (2, 3), (2, 4), (3, 6), (3, 7), (3, 8);

INSERT INTO seasonal_node (name, facility_type, month_no, day_no, template_id, description, enabled) VALUES
('汛前排水系统检查', 'WATER', 4, 15, 2, '汛期前排空集水坑、检查排水泵与阀门井', 1),
('入冬前管道防冻保温', 'WATER', 11, 10, 2, '检查管道保温层、伴热带与室外阀门井', 1),
('制冷季前空调系统保养', 'HVAC', 5, 10, 4, '清洗滤网、检查冷媒压力与冷却塔', 1),
('制热季前空调系统保养', 'HVAC', 10, 15, 4, '检查制热模式、风阀与末端', 1),
('一季度配电除尘', 'POWER', 3, 20, 3, '配电柜除尘、紧固母排、接地测试', 1),
('二季度配电除尘', 'POWER', 6, 20, 3, '配电柜除尘、紧固母排、接地测试', 1),
('三季度配电除尘', 'POWER', 9, 20, 3, '配电柜除尘、紧固母排、接地测试', 1),
('四季度配电除尘', 'POWER', 12, 20, 3, '配电柜除尘、紧固母排、接地测试', 1),
('每月备用电源自启动测试', 'POWER', 1, 5, 3, '发电机与 UPS 带载测试', 1),
('春季绿化补植与施肥', 'GARDEN', 3, 12, 4, '补植麦冬、施肥与灌溉系统检查', 1),
('夏季绿化病虫害防治', 'GARDEN', 7, 8, 4, '喷药防治，提前公示安全提示', 1),
('秋季绿化修剪', 'GARDEN', 9, 18, 4, '乔木修剪与落叶清理', 1),
('电梯年检前自查', 'ELEVATOR', 6, 1, 1, '年检前全面自查并整改', 1),
('安防存储与镜头清洁', 'SECURITY', 2, 25, 4, '录像存储检查、镜头除尘', 1),
('消防合规自查（不产生公开数据）', 'FIRE', 11, 9, 4, '消防设施合规自查，数据不对外公开', 0);

-- ---------------- 工单（覆盖 5 状态 / 5 类型 / 应急 / 驳回） ----------------
INSERT INTO work_order (id, order_no, order_type, facility_type, equipment_id, community_id, building_id, house_id, title, description, urgency, status, source, plan_id, dispatch_mode, deadline, main_worker_id, team_id, contract_id, accept_result, satisfaction_score, publish_flag, reporter_user_id, reporter_name, reporter_phone, reward_amount, finish_time, archive_time, cancel_reason, urge_count, remark, create_time) VALUES
(1, 'WO20261015000001', 'PLAN_MAINTAIN', 'ELEVATOR', 1, 1, 1, NULL, '1号楼1单元电梯半月保', '按电梯半月保清单执行机房、轿厢、井道、层站检查，制动器、限速器、门系统需拍照留痕。', 'NORMAL', 'ARCHIVED', 'PLAN', 1, 'GRAB', DATE_SUB(NOW(), INTERVAL 19 DAY), 1, 1, 1, 'PASS', 5, 1, NULL, NULL, NULL, 300.00, DATE_SUB(NOW(), INTERVAL 19 DAY), DATE_SUB(NOW(), INTERVAL 18 DAY), NULL, 0, '半月保正常', DATE_SUB(NOW(), INTERVAL 21 DAY)),
(2, 'WO20261017000002', 'PLAN_MAINTAIN', 'ELEVATOR', 2, 1, 2, NULL, '1号楼2单元电梯半月保', '按计划执行半月保，重点关注门系统与平层精度。', 'NORMAL', 'ARCHIVED', 'PLAN', 1, 'GRAB', DATE_SUB(NOW(), INTERVAL 17 DAY), 1, 1, 2, 'PASS', 4, 1, NULL, NULL, NULL, 300.00, DATE_SUB(NOW(), INTERVAL 17 DAY), DATE_SUB(NOW(), INTERVAL 16 DAY), NULL, 0, '', DATE_SUB(NOW(), INTERVAL 19 DAY)),
(3, 'WO20261102000003', 'DEMAND_MAINTAIN', 'WATER', 3, 1, 1, NULL, '地下水泵房生活水泵按需维保', '按 60 天间隔执行水泵机械密封检查、压力测试与阀门井检查。', 'NORMAL', 'PENDING_REVIEW', 'PLAN', 2, 'ASSIGN', DATE_SUB(NOW(), INTERVAL 1 DAY), 2, 2, NULL, NULL, NULL, 0, NULL, NULL, NULL, 500.00, DATE_SUB(NOW(), INTERVAL 1 DAY), NULL, NULL, 0, '已提交待验收', DATE_SUB(NOW(), INTERVAL 3 DAY)),
(4, 'WO20261101000004', 'PLAN_MAINTAIN', 'POWER', 6, 1, 1, NULL, '1号楼配电室季度除尘保养', '按供配电清单执行母排紧固、开关触头检查、接地电阻测试。', 'NORMAL', 'PENDING_REVIEW', 'PLAN', 3, 'ASSIGN', DATE_SUB(NOW(), INTERVAL 1 DAY), 2, 2, NULL, NULL, NULL, 0, NULL, NULL, NULL, 400.00, DATE_SUB(NOW(), INTERVAL 1 DAY), NULL, NULL, 0, '', DATE_SUB(NOW(), INTERVAL 4 DAY)),
(5, 'WO20261103000005', 'FAULT_REPAIR', 'WATER', 5, 1, 1, 13, '2单元6层水管接口渗漏', '业主反映 1号楼2单元6层公共管道接口渗水，墙面已出现水渍，请尽快处理。', 'HIGH', 'PROCESSING', 'OWNER', NULL, 'ASSIGN', DATE_ADD(NOW(), INTERVAL 6 HOUR), 2, 2, NULL, NULL, NULL, 0, 4, '周明', '13800000004', 180.00, NULL, NULL, NULL, 1, '', DATE_SUB(NOW(), INTERVAL 1 DAY)),
(6, 'WO20261104000006', 'FAULT_REPAIR', 'ELEVATOR', 1, 1, 1, NULL, '1号楼1单元电梯门机异响', '电梯开关门时有金属摩擦声，业主担心安全，请安排检查。', 'HIGH', 'DISPATCHED', 'OWNER', NULL, 'ASSIGN', DATE_ADD(NOW(), INTERVAL 7 HOUR), 1, 1, NULL, NULL, NULL, 0, 1, '陈建国', '13800000001', 260.00, NULL, NULL, NULL, 0, '', DATE_SUB(NOW(), INTERVAL 10 HOUR)),
(7, 'WO20261030000007', 'EMERGENCY', 'WATER', 3, 1, 1, NULL, '地下水泵房出水压力骤降应急抢修', '夜间供水压力由 0.45MPa 降至 0.12MPa，怀疑止回阀故障，已启动应急抢修。', 'URGENT', 'ARCHIVED', 'MANUAL', NULL, 'ASSIGN', DATE_SUB(NOW(), INTERVAL 5 DAY), 2, 2, 3, 'PASS', 5, 1, NULL, NULL, NULL, 800.00, DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY), NULL, 0, '更换止回阀后恢复', DATE_SUB(NOW(), INTERVAL 5 DAY)),
(8, 'WO20261104000008', 'INSPECTION', 'POWER', 7, 1, 1, NULL, '巡检发现总配电柜母排温度异常', '巡检任务 XJ202611030002 在总配电柜点位发现 B 相母排温度 78℃，超出 65℃ 告警值。', 'HIGH', 'PENDING_DISPATCH', 'INSPECTION', NULL, 'GRAB', DATE_ADD(NOW(), INTERVAL 8 HOUR), NULL, NULL, NULL, NULL, NULL, 0, NULL, NULL, NULL, 350.00, NULL, NULL, NULL, 0, '巡检异常转单', DATE_SUB(NOW(), INTERVAL 6 HOUR)),
(9, 'WO20261104000009', 'FAULT_REPAIR', 'HVAC', 9, 1, 1, NULL, '1号楼空调机房异响', '空调主机启动后持续异响，制冷效果下降，建议检查压缩机与风机轴承。', 'NORMAL', 'PENDING_DISPATCH', 'OWNER', NULL, 'GRAB', DATE_ADD(NOW(), INTERVAL 20 HOUR), NULL, NULL, NULL, NULL, NULL, 0, 3, '刘慧', '13800000003', 320.00, NULL, NULL, NULL, 0, '', DATE_SUB(NOW(), INTERVAL 3 HOUR)),
(10, 'WO20261031000010', 'FAULT_REPAIR', 'GARDEN', 11, 1, NULL, NULL, '中心花园灌溉喷头堵塞', '喷灌出水不均匀，部分绿植缺水。', 'LOW', 'CANCELED', 'OWNER', NULL, 'ASSIGN', DATE_SUB(NOW(), INTERVAL 4 DAY), NULL, NULL, NULL, NULL, NULL, 0, 5, '吴芳', '13800000005', NULL, NULL, NULL, '业主表示已自行处理，申请作废', 0, '', DATE_SUB(NOW(), INTERVAL 4 DAY)),
(11, 'WO20261023000011', 'PLAN_MAINTAIN', 'POWER', 8, 1, 1, NULL, '地下车库公共照明回路保养', '检查照明回路、时控开关与灯具，处理 3 处接触不良。', 'NORMAL', 'ARCHIVED', 'PLAN', 3, 'ASSIGN', DATE_SUB(NOW(), INTERVAL 12 DAY), 2, 2, 4, 'PASS', 4, 1, NULL, NULL, NULL, 200.00, DATE_SUB(NOW(), INTERVAL 12 DAY), DATE_SUB(NOW(), INTERVAL 11 DAY), NULL, 0, '', DATE_SUB(NOW(), INTERVAL 14 DAY)),
(12, 'WO20261102000012', 'DEMAND_MAINTAIN', 'WATER', 4, 1, 1, NULL, '屋顶生活水箱清洗消毒', '按半年周期清洗水箱并送检水质，需停水 4 小时并提前公示。', 'NORMAL', 'PROCESSING', 'PLAN', 2, 'ASSIGN', DATE_ADD(NOW(), INTERVAL 2 DAY), 2, 2, NULL, NULL, NULL, 0, NULL, NULL, NULL, 450.00, NULL, NULL, NULL, 0, '', DATE_SUB(NOW(), INTERVAL 2 DAY)),
(13, 'WO20261103000013', 'FAULT_REPAIR', 'SECURITY', 10, 1, 1, NULL, '大厅监控画面闪烁', '1号楼大厅 2 号摄像头画面闪烁，疑似线路接触不良。', 'NORMAL', 'PENDING_REVIEW', 'OWNER', NULL, 'ASSIGN', DATE_SUB(NOW(), INTERVAL 6 HOUR), 3, 3, NULL, NULL, NULL, 0, 4, '周明', '13800000004', 150.00, DATE_SUB(NOW(), INTERVAL 4 HOUR), NULL, NULL, 0, '', DATE_SUB(NOW(), INTERVAL 1 DAY)),
(14, 'WO20261104000014', 'FAULT_REPAIR', 'WATER', 4, 1, 1, NULL, '屋顶水箱溢流管漏水', '巡查发现屋顶水箱溢流管接口滴水，需更换密封垫。', 'HIGH', 'DISPATCHED', 'MANUAL', NULL, 'ASSIGN', DATE_ADD(NOW(), INTERVAL 10 HOUR), 2, 2, NULL, NULL, NULL, 0, NULL, NULL, NULL, 160.00, NULL, NULL, NULL, 0, '', DATE_SUB(NOW(), INTERVAL 8 HOUR)),
(15, 'WO20261005000015', 'PLAN_MAINTAIN', 'ELEVATOR', 2, 1, 2, NULL, '电梯困人应急演练与季度检查', '按季度组织困人救援演练，检查五方对讲与应急照明。', 'NORMAL', 'ARCHIVED', 'PLAN', 1, 'ASSIGN', DATE_SUB(NOW(), INTERVAL 30 DAY), 3, 3, 5, 'PASS', 5, 1, NULL, NULL, NULL, 280.00, DATE_SUB(NOW(), INTERVAL 30 DAY), DATE_SUB(NOW(), INTERVAL 29 DAY), NULL, 0, '', DATE_SUB(NOW(), INTERVAL 32 DAY)),
(16, 'WO20261103000016', 'INSPECTION', 'SECURITY', 10, 1, 1, NULL, '监控存储硬盘告警巡检转单', '巡检发现 NVR 硬盘健康度 62%，存在录像丢失风险。', 'NORMAL', 'PROCESSING', 'INSPECTION', NULL, 'GRAB', DATE_ADD(NOW(), INTERVAL 12 HOUR), 3, 3, NULL, NULL, NULL, 0, NULL, NULL, NULL, 220.00, NULL, NULL, NULL, 0, '', DATE_SUB(NOW(), INTERVAL 1 DAY)),
(17, 'WO20261026000017', 'FAULT_REPAIR', 'POWER', 7, 1, 1, NULL, '配电柜指示灯异常', '总配电柜 A 相指示灯不亮，已更换指示灯并复测。', 'NORMAL', 'ARCHIVED', 'OWNER', NULL, 'ASSIGN', DATE_SUB(NOW(), INTERVAL 8 DAY), 2, 2, NULL, 'PASS', 4, 1, 6, '陈建国', '13800000001', 120.00, DATE_SUB(NOW(), INTERVAL 7 DAY), DATE_SUB(NOW(), INTERVAL 7 DAY), NULL, 0, '', DATE_SUB(NOW(), INTERVAL 8 DAY)),
(18, 'WO20261028000018', 'EMERGENCY', 'ELEVATOR', 2, 1, 2, NULL, '1号楼2单元电梯困人应急处置', '19:42 电梯困人 2 人，值班人员 12 分钟内到场，20:05 安全救出。', 'URGENT', 'ARCHIVED', 'MANUAL', NULL, 'ASSIGN', DATE_SUB(NOW(), INTERVAL 6 DAY), 1, 1, NULL, 'PASS', 5, 0, NULL, NULL, NULL, 600.00, DATE_SUB(NOW(), INTERVAL 6 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY), NULL, 0, '已完成 24 小时内复盘', DATE_SUB(NOW(), INTERVAL 6 DAY)),
(19, 'WO20261104000019', 'FAULT_REPAIR', 'WATER', 3, 1, 1, NULL, '水泵房地面渗水', '水泵房集水坑水位偏高，地面有渗水痕迹，请检查排水泵。', 'NORMAL', 'PENDING_DISPATCH', 'OWNER', NULL, 'ASSIGN', DATE_ADD(NOW(), INTERVAL 20 HOUR), NULL, NULL, NULL, NULL, NULL, 0, 7, '孙丽', '13800000007', 180.00, NULL, NULL, NULL, 0, '', DATE_SUB(NOW(), INTERVAL 2 HOUR)),
(20, 'WO20261104000020', 'PLAN_MAINTAIN', 'WATER', 5, 1, 1, NULL, '给排水系统汛前排水检查', '汛前检查阀门井积水、排水泵与管网通畅情况。', 'NORMAL', 'PENDING_DISPATCH', 'PLAN', NULL, 'GRAB', DATE_ADD(NOW(), INTERVAL 3 DAY), NULL, NULL, NULL, NULL, NULL, 0, NULL, NULL, NULL, 260.00, NULL, NULL, NULL, 0, '季节节点生成', DATE_SUB(NOW(), INTERVAL 1 DAY)),
(21, 'WO20261024000021', 'FAULT_REPAIR', 'ELEVATOR', 2, 1, 2, NULL, '电梯平层精度偏差', '6 层停靠时轿厢地坎高出层门地坎约 15mm，已重新调整平层。', 'NORMAL', 'ARCHIVED', 'OWNER', NULL, 'ASSIGN', DATE_SUB(NOW(), INTERVAL 11 DAY), 1, 1, NULL, 'PASS', 3, 0, 8, '马超', '13800000008', 240.00, DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_SUB(NOW(), INTERVAL 10 DAY), NULL, 0, '', DATE_SUB(NOW(), INTERVAL 11 DAY)),
(22, 'WO20261027000022', 'INSPECTION', 'GARDEN', 11, 1, NULL, NULL, '绿化带护栏松动', '巡检发现花园西侧护栏立柱松动，已转报修。', 'LOW', 'CANCELED', 'INSPECTION', NULL, 'GRAB', DATE_SUB(NOW(), INTERVAL 7 DAY), NULL, NULL, NULL, NULL, NULL, 0, NULL, NULL, NULL, 80.00, NULL, NULL, '与工单重复，作废', 0, '', DATE_SUB(NOW(), INTERVAL 7 DAY));

-- 工单流转日志（关键链路）
INSERT INTO work_order_log (order_id, action, from_status, to_status, reason, operator_id, operator_name, create_time) VALUES
(1, 'CREATE', NULL, 'PENDING_DISPATCH', '计划引擎自动生成', NULL, '系统', DATE_SUB(NOW(), INTERVAL 21 DAY)),
(1, 'CLAIM', 'PENDING_DISPATCH', 'DISPATCHED', '抢单成功', 2, '张伟', DATE_SUB(NOW(), INTERVAL 20 DAY)),
(1, 'START', 'DISPATCHED', 'PROCESSING', '到场打卡，开始作业', 2, '张伟', DATE_SUB(NOW(), INTERVAL 20 DAY)),
(1, 'SUBMIT', 'PROCESSING', 'PENDING_REVIEW', '完工提交，等待验收', 2, '张伟', DATE_SUB(NOW(), INTERVAL 19 DAY)),
(1, 'ACCEPT', 'PENDING_REVIEW', 'PENDING_REVIEW', '验收通过：作业清单 9/9，强制拍照项 4/4', 1, '周工', DATE_SUB(NOW(), INTERVAL 19 DAY)),
(1, 'ARCHIVE', 'PENDING_REVIEW', 'ARCHIVED', '回访完成，工单归档', 1, '周工', DATE_SUB(NOW(), INTERVAL 18 DAY)),
(2, 'CREATE', NULL, 'PENDING_DISPATCH', '计划引擎自动生成', NULL, '系统', DATE_SUB(NOW(), INTERVAL 19 DAY)),
(2, 'CLAIM', 'PENDING_DISPATCH', 'DISPATCHED', '抢单成功', 2, '张伟', DATE_SUB(NOW(), INTERVAL 18 DAY)),
(2, 'START', 'DISPATCHED', 'PROCESSING', '到场打卡，开始作业', 2, '张伟', DATE_SUB(NOW(), INTERVAL 18 DAY)),
(2, 'SUBMIT', 'PROCESSING', 'PENDING_REVIEW', '完工提交，等待验收', 2, '张伟', DATE_SUB(NOW(), INTERVAL 17 DAY)),
(2, 'ACCEPT', 'PENDING_REVIEW', 'PENDING_REVIEW', '验收通过', 1, '周工', DATE_SUB(NOW(), INTERVAL 17 DAY)),
(2, 'ARCHIVE', 'PENDING_REVIEW', 'ARCHIVED', '回访完成，工单归档', 1, '周工', DATE_SUB(NOW(), INTERVAL 16 DAY)),
(3, 'CREATE', NULL, 'PENDING_DISPATCH', '计划引擎自动生成', NULL, '系统', DATE_SUB(NOW(), INTERVAL 3 DAY)),
(3, 'DISPATCH', 'PENDING_DISPATCH', 'DISPATCHED', '派单给 李强', 1, '周工', DATE_SUB(NOW(), INTERVAL 3 DAY)),
(3, 'START', 'DISPATCHED', 'PROCESSING', '到场打卡，开始作业', 3, '李强', DATE_SUB(NOW(), INTERVAL 2 DAY)),
(3, 'SUBMIT', 'PROCESSING', 'PENDING_REVIEW', '完工提交，等待验收', 3, '李强', DATE_SUB(NOW(), INTERVAL 1 DAY)),
(5, 'CREATE', NULL, 'PENDING_DISPATCH', '业主上报：周明', NULL, '系统', DATE_SUB(NOW(), INTERVAL 1 DAY)),
(5, 'DISPATCH', 'PENDING_DISPATCH', 'DISPATCHED', '派单给 李强', 1, '周工', DATE_SUB(NOW(), INTERVAL 1 DAY)),
(5, 'START', 'DISPATCHED', 'PROCESSING', '到场打卡，开始作业', 3, '李强', DATE_SUB(NOW(), INTERVAL 20 HOUR)),
(5, 'URGE', 'PROCESSING', 'PROCESSING', '第 1 次催办', 1, '周工', DATE_SUB(NOW(), INTERVAL 8 HOUR)),
(6, 'CREATE', NULL, 'PENDING_DISPATCH', '业主上报：陈建国', NULL, '系统', DATE_SUB(NOW(), INTERVAL 10 HOUR)),
(6, 'DISPATCH', 'PENDING_DISPATCH', 'DISPATCHED', '派单给 张伟', 1, '周工', DATE_SUB(NOW(), INTERVAL 9 HOUR)),
(7, 'CREATE', NULL, 'PENDING_DISPATCH', '创建工单', 1, '周工', DATE_SUB(NOW(), INTERVAL 5 DAY)),
(7, 'DISPATCH', 'PENDING_DISPATCH', 'DISPATCHED', '应急工单直接指派给 李强', 1, '周工', DATE_SUB(NOW(), INTERVAL 5 DAY)),
(7, 'START', 'DISPATCHED', 'PROCESSING', '到场打卡，开始作业', 3, '李强', DATE_SUB(NOW(), INTERVAL 5 DAY)),
(7, 'SUBMIT', 'PROCESSING', 'PENDING_REVIEW', '完工提交，等待验收', 3, '李强', DATE_SUB(NOW(), INTERVAL 5 DAY)),
(7, 'ACCEPT', 'PENDING_REVIEW', 'PENDING_REVIEW', '验收通过', 1, '周工', DATE_SUB(NOW(), INTERVAL 5 DAY)),
(7, 'ARCHIVE', 'PENDING_REVIEW', 'ARCHIVED', '回访完成，工单归档', 1, '周工', DATE_SUB(NOW(), INTERVAL 4 DAY)),
(8, 'CREATE', NULL, 'PENDING_DISPATCH', '巡检异常转单', NULL, '系统', DATE_SUB(NOW(), INTERVAL 6 HOUR)),
(9, 'CREATE', NULL, 'PENDING_DISPATCH', '业主上报：刘慧', NULL, '系统', DATE_SUB(NOW(), INTERVAL 3 HOUR)),
(13, 'CREATE', NULL, 'PENDING_DISPATCH', '业主上报：周明', NULL, '系统', DATE_SUB(NOW(), INTERVAL 1 DAY)),
(13, 'DISPATCH', 'PENDING_DISPATCH', 'DISPATCHED', '派单给 王敏', 1, '周工', DATE_SUB(NOW(), INTERVAL 1 DAY)),
(13, 'SUBMIT', 'PROCESSING', 'PENDING_REVIEW', '完工提交，等待验收', 4, '王敏', DATE_SUB(NOW(), INTERVAL 4 HOUR)),
(17, 'CREATE', NULL, 'PENDING_DISPATCH', '业主上报：陈建国', NULL, '系统', DATE_SUB(NOW(), INTERVAL 8 DAY)),
(17, 'ACCEPT', 'PENDING_REVIEW', 'PENDING_REVIEW', '验收通过', 1, '周工', DATE_SUB(NOW(), INTERVAL 7 DAY)),
(17, 'REJECT', 'PENDING_REVIEW', 'PROCESSING', '验收不合格驳回重做：指示灯亮度不足', 1, '周工', DATE_SUB(NOW(), INTERVAL 8 DAY)),
(18, 'CREATE', NULL, 'PENDING_DISPATCH', '创建工单', 1, '周工', DATE_SUB(NOW(), INTERVAL 6 DAY)),
(18, 'DISPATCH', 'PENDING_DISPATCH', 'DISPATCHED', '应急工单直接指派给 张伟', 1, '周工', DATE_SUB(NOW(), INTERVAL 6 DAY)),
(18, 'ARCHIVE', 'PENDING_REVIEW', 'ARCHIVED', '回访完成，工单归档', 1, '周工', DATE_SUB(NOW(), INTERVAL 5 DAY)),
(21, 'CREATE', NULL, 'PENDING_DISPATCH', '业主上报：马超', NULL, '系统', DATE_SUB(NOW(), INTERVAL 11 DAY)),
(21, 'ACCEPT', 'PENDING_REVIEW', 'PENDING_REVIEW', '验收通过', 1, '周工', DATE_SUB(NOW(), INTERVAL 10 DAY)),
(21, 'ARCHIVE', 'PENDING_REVIEW', 'ARCHIVED', '回访完成，工单归档', 1, '周工', DATE_SUB(NOW(), INTERVAL 10 DAY));

INSERT INTO work_order_assign (order_id, personnel_id, assign_type, remark, assign_time) VALUES
(1, 1, 'MAIN', '开放抢单', DATE_SUB(NOW(), INTERVAL 20 DAY)),
(2, 1, 'MAIN', '开放抢单', DATE_SUB(NOW(), INTERVAL 18 DAY)),
(3, 2, 'MAIN', '按需保养指派', DATE_SUB(NOW(), INTERVAL 3 DAY)),
(5, 2, 'MAIN', '报修派单', DATE_SUB(NOW(), INTERVAL 1 DAY)),
(5, 4, 'ASSIST', '协助关闭总阀（不占派单位）', DATE_SUB(NOW(), INTERVAL 1 DAY)),
(6, 1, 'MAIN', '报修派单', DATE_SUB(NOW(), INTERVAL 9 HOUR)),
(7, 2, 'MAIN', '应急指派', DATE_SUB(NOW(), INTERVAL 5 DAY)),
(13, 3, 'MAIN', '报修派单', DATE_SUB(NOW(), INTERVAL 1 DAY)),
(17, 2, 'MAIN', '报修派单', DATE_SUB(NOW(), INTERVAL 8 DAY));

INSERT INTO work_order_claim (order_id, personnel_id, user_id, claim_time, success, fail_reason) VALUES
(1, 1, 2, DATE_SUB(NOW(), INTERVAL 20 DAY), 1, NULL),
(2, 1, 2, DATE_SUB(NOW(), INTERVAL 18 DAY), 1, NULL),
(2, 3, 4, DATE_SUB(NOW(), INTERVAL 18 DAY), 0, '已被其他人员抢单');

INSERT INTO work_order_settlement (order_id, company_id, personnel_id, amount, settle_status, settle_time, remark) VALUES
(1, 1, 1, 300.00, 'SETTLED', DATE_SUB(NOW(), INTERVAL 18 DAY), '归档后结算'),
(2, 1, 1, 300.00, 'SETTLED', DATE_SUB(NOW(), INTERVAL 16 DAY), '归档后结算'),
(3, 2, 2, 500.00, 'PENDING', NULL, '待验收后结算'),
(7, 2, 2, 800.00, 'SETTLED', DATE_SUB(NOW(), INTERVAL 4 DAY), '应急处置结算'),
(11, 2, 2, 200.00, 'SETTLED', DATE_SUB(NOW(), INTERVAL 11 DAY), ''),
(13, NULL, 3, 150.00, 'PENDING', NULL, '内部班组结算'),
(17, 2, 2, 120.00, 'SETTLED', DATE_SUB(NOW(), INTERVAL 7 DAY), '');

-- ---------------- 电子合同与责任链 ----------------
INSERT INTO contract (id, contract_no, order_id, equipment_id, equipment_name, party_a, company_id, party_b, service_content, responsibility_days, resp_start, resp_end, status, sign_time_a, sign_user_a, sign_time_b, sign_user_b, remind_flag) VALUES
(1, 'HT202610150001', 1, 1, '1号楼1单元电梯半月保', '物业服务中心', 1, '恒安电梯工程有限公司', '电梯半月保：机房、轿厢、井道、层站共 9 项作业，制动器/限速器/门系统拍照留痕。', 45, DATE_SUB(NOW(), INTERVAL 19 DAY), DATE_ADD(NOW(), INTERVAL 26 DAY), 'ACTIVE', DATE_SUB(NOW(), INTERVAL 19 DAY), '周工', DATE_SUB(NOW(), INTERVAL 19 DAY), '张伟', 0),
(2, 'HT202610170002', 2, 2, '1号楼2单元电梯半月保', '物业服务中心', 1, '恒安电梯工程有限公司', '电梯半月保 9 项作业，含平层精度复核。', 45, DATE_SUB(NOW(), INTERVAL 17 DAY), DATE_ADD(NOW(), INTERVAL 28 DAY), 'ACTIVE', DATE_SUB(NOW(), INTERVAL 17 DAY), '周工', DATE_SUB(NOW(), INTERVAL 17 DAY), '张伟', 0),
(3, 'HT202610300003', 7, 3, '地下水泵房出水压力骤降应急抢修', '物业服务中心', 2, '蓝海机电设备服务有限公司', '更换 DN100 止回阀，复测供水压力至 0.45MPa。', 45, DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_ADD(NOW(), INTERVAL 40 DAY), 'ACTIVE', DATE_SUB(NOW(), INTERVAL 5 DAY), '周工', DATE_SUB(NOW(), INTERVAL 5 DAY), '李强', 0),
(4, 'HT202610230004', 11, 8, '地下车库公共照明回路保养', '物业服务中心', 2, '蓝海机电设备服务有限公司', '照明回路保养，处理 3 处接触不良。', 60, DATE_SUB(NOW(), INTERVAL 12 DAY), DATE_ADD(NOW(), INTERVAL 48 DAY), 'ACTIVE', DATE_SUB(NOW(), INTERVAL 12 DAY), '周工', DATE_SUB(NOW(), INTERVAL 12 DAY), '李强', 0),
(5, 'HT202610050005', 15, 2, '电梯困人应急演练与季度检查', '物业服务中心', 1, '恒安电梯工程有限公司', '困人救援演练、五方对讲与应急照明检查。', 45, DATE_SUB(NOW(), INTERVAL 30 DAY), DATE_ADD(NOW(), INTERVAL 15 DAY), 'ACTIVE', DATE_SUB(NOW(), INTERVAL 30 DAY), '周工', DATE_SUB(NOW(), INTERVAL 30 DAY), '王敏', 0);

INSERT INTO contract_confirm (contract_id, party, operator_id, operator_name, confirm_time, remark) VALUES
(1, 'A', 1, '周工', DATE_SUB(NOW(), INTERVAL 19 DAY), '物业方确认'),
(1, 'B', 2, '张伟', DATE_SUB(NOW(), INTERVAL 19 DAY), '接单方确认'),
(2, 'A', 1, '周工', DATE_SUB(NOW(), INTERVAL 17 DAY), ''),
(2, 'B', 2, '张伟', DATE_SUB(NOW(), INTERVAL 17 DAY), ''),
(3, 'A', 1, '周工', DATE_SUB(NOW(), INTERVAL 5 DAY), ''),
(3, 'B', 3, '李强', DATE_SUB(NOW(), INTERVAL 5 DAY), ''),
(4, 'A', 1, '周工', DATE_SUB(NOW(), INTERVAL 12 DAY), ''),
(4, 'B', 3, '李强', DATE_SUB(NOW(), INTERVAL 12 DAY), ''),
(5, 'A', 1, '周工', DATE_SUB(NOW(), INTERVAL 30 DAY), ''),
(5, 'B', 4, '王敏', DATE_SUB(NOW(), INTERVAL 30 DAY), '');

INSERT INTO responsibility_link (equipment_id, prev_contract_id, next_contract_id) VALUES
(1, 1, 1),
(2, 2, 5);

-- ---------------- 现场记录 ----------------
INSERT INTO evidence_record (order_id, personnel_id, record_type, content, create_by, create_time) VALUES
(1, 1, 'PROGRESS', '到场打卡（定位，距设备 12 米）', 2, DATE_SUB(NOW(), INTERVAL 20 DAY)),
(1, 1, 'PROGRESS', '制动器间隙 0.35mm，符合要求', 2, DATE_SUB(NOW(), INTERVAL 19 DAY)),
(7, 2, 'PROGRESS', '止回阀阀芯卡阻，已更换同型号备件', 3, DATE_SUB(NOW(), INTERVAL 5 DAY)),
(7, 2, 'HAZARD', '建议对同型号止回阀开展专项排查', 3, DATE_SUB(NOW(), INTERVAL 5 DAY)),
(5, 2, 'PROGRESS', '已关闭单元总阀，等待管件到场', 3, DATE_SUB(NOW(), INTERVAL 20 HOUR)),
(5, 1, 'REVIEW', '回访：业主催促尽快恢复供水，满意度待评价', 1, DATE_SUB(NOW(), INTERVAL 8 HOUR));

INSERT INTO acceptance_record (order_id, result, reason, check_detail, deduct_score, operator_id, operator_name, create_time) VALUES
(1, 'PASS', NULL, '作业清单 9/9，强制拍照项 4/4', 0, 1, '周工', DATE_SUB(NOW(), INTERVAL 19 DAY)),
(2, 'PASS', NULL, '作业清单 9/9，强制拍照项 4/4', 0, 1, '周工', DATE_SUB(NOW(), INTERVAL 17 DAY)),
(7, 'PASS', NULL, '作业清单 6/6，强制拍照项 3/3', 0, 1, '周工', DATE_SUB(NOW(), INTERVAL 5 DAY)),
(17, 'REJECT', '指示灯亮度不足，未达到验收标准', '作业清单 5/5，强制拍照项 3/3', 2, 1, '周工', DATE_SUB(NOW(), INTERVAL 8 DAY)),
(17, 'PASS', '整改后复验合格', '作业清单 5/5，强制拍照项 3/3', 0, 1, '周工', DATE_SUB(NOW(), INTERVAL 7 DAY)),
(21, 'PASS', NULL, '平层偏差调整至 3mm 以内', 0, 1, '周工', DATE_SUB(NOW(), INTERVAL 10 DAY));

INSERT INTO credit_record (target_type, target_id, target_name, order_id, credit_type, score, reason, operator_id, operator_name, create_time) VALUES
('PERSONNEL', 1, '张伟', 1, 'EVALUATE', 5, '业主满意度评价 5 星', 1, '周工', DATE_SUB(NOW(), INTERVAL 18 DAY)),
('COMPANY', 1, '恒安电梯工程有限公司', 1, 'EVALUATE', 5, '业主满意度评价 5 星', 1, '周工', DATE_SUB(NOW(), INTERVAL 18 DAY)),
('PERSONNEL', 2, '李强', 17, 'DEDUCT', -2, '验收驳回重做：指示灯亮度不足', 1, '周工', DATE_SUB(NOW(), INTERVAL 8 DAY)),
('COMPANY', 2, '蓝海机电设备服务有限公司', 17, 'DEDUCT', -2, '验收驳回重做：指示灯亮度不足', 1, '周工', DATE_SUB(NOW(), INTERVAL 8 DAY)),
('PERSONNEL', 1, '张伟', 21, 'EVALUATE', 3, '业主满意度评价 3 星', 1, '周工', DATE_SUB(NOW(), INTERVAL 10 DAY)),
('PERSONNEL', 2, '李强', 7, 'EVALUATE', 5, '应急处置响应及时', 1, '周工', DATE_SUB(NOW(), INTERVAL 4 DAY));

-- ---------------- 巡检配置 ----------------
INSERT INTO inspection_route (id, name, community_id, facility_type, frequency, leader_id, status, remark) VALUES
(1, '1号楼公共区域巡检路线', 1, 'POWER', 'WEEKLY', 3, 'ENABLED', '配电室 → 大厅监控 → 车库照明'),
(2, '地下水泵房日常巡检', 1, 'WATER', 'DAILY', 2, 'ENABLED', '水泵 → 水箱 → 阀门井');

INSERT INTO inspection_point (id, route_id, equipment_id, point_name, sort_no, remark) VALUES
(1, 1, 6, '1号楼配电室', 1, '查看配电室温湿度与告警'),
(2, 1, 10, '1号楼大厅监控点位', 2, '查看画面与存储状态'),
(3, 1, 8, '地下车库照明配电箱', 3, '查看回路指示灯'),
(4, 2, 3, '地下水泵房', 1, '查看水泵运行与压力'),
(5, 2, 4, '屋顶水箱', 2, '查看水位与溢流管'),
(6, 2, 5, '东侧阀门井', 3, '查看积水与阀门状态');

INSERT INTO inspection_check_item (point_id, item_name, require_value, unit, sort_no) VALUES
(1, '配电室温度', 1, '℃', 1),
(1, '配电柜指示灯', 0, NULL, 2),
(2, '监控画面清晰度', 0, NULL, 1),
(2, '录像存储天数', 1, '天', 2),
(3, '照明回路上电状态', 0, NULL, 1),
(4, '水泵出口压力', 1, 'MPa', 1),
(4, '水泵运行声音', 0, NULL, 2),
(5, '水箱水位', 1, 'm', 1),
(5, '溢流管接口', 0, NULL, 2),
(6, '阀门井积水', 0, NULL, 1),
(6, '闸阀启闭状态', 0, NULL, 2);

INSERT INTO inspection_task (id, task_no, route_id, plan_date, assignee_id, status, finish_time, abnormal_count) VALUES
(1, 'XJ202611020001', 2, DATE_SUB(CURDATE(), INTERVAL 2 DAY), 2, 'DONE', DATE_SUB(NOW(), INTERVAL 2 DAY), 0),
(2, 'XJ202611040002', 2, CURDATE(), 2, 'PENDING', NULL, 0),
(3, 'XJ202611030003', 1, DATE_SUB(CURDATE(), INTERVAL 1 DAY), 3, 'PENDING', NULL, 1);

INSERT INTO inspection_task_point (task_id, point_id, point_name, equipment_id, status, result, checkin_time, longitude, latitude, remark, sort_no) VALUES
(1, 4, '地下水泵房', 3, 'DONE', 'NORMAL', DATE_SUB(NOW(), INTERVAL 2 DAY), 116.404488, 39.915190, '压力 0.45MPa 正常', 1),
(1, 5, '屋顶水箱', 4, 'DONE', 'NORMAL', DATE_SUB(NOW(), INTERVAL 2 DAY), 116.404530, 39.915260, '水位 2.1m 正常', 2),
(1, 6, '东侧阀门井', 5, 'DONE', 'NORMAL', DATE_SUB(NOW(), INTERVAL 2 DAY), 116.404700, 39.915210, '无积水，阀门开启', 3),
(2, 4, '地下水泵房', 3, 'PENDING', NULL, NULL, NULL, NULL, NULL, 1),
(2, 5, '屋顶水箱', 4, 'PENDING', NULL, NULL, NULL, NULL, NULL, 2),
(2, 6, '东侧阀门井', 5, 'PENDING', NULL, NULL, NULL, NULL, NULL, 3),
(3, 1, '1号楼配电室', 6, 'DONE', 'ABNORMAL', DATE_SUB(NOW(), INTERVAL 1 DAY), 116.404455, 39.915175, 'B 相母排温度 78℃，超出 65℃ 告警值', 1),
(3, 2, '1号楼大厅监控点位', 10, 'PENDING', NULL, NULL, NULL, NULL, NULL, 2),
(3, 3, '地下车库照明配电箱', 8, 'PENDING', NULL, NULL, NULL, NULL, NULL, 3);

INSERT INTO inspection_record (task_id, task_point_id, item_id, item_name, result, item_value, remark, create_time) VALUES
(1, 1, 6, '水泵出口压力', 'NORMAL', '0.45', 'MPa', DATE_SUB(NOW(), INTERVAL 2 DAY)),
(1, 1, 7, '水泵运行声音', 'NORMAL', NULL, '运行平稳', DATE_SUB(NOW(), INTERVAL 2 DAY)),
(1, 2, 8, '水箱水位', 'NORMAL', '2.1', 'm', DATE_SUB(NOW(), INTERVAL 2 DAY)),
(1, 3, 10, '阀门井积水', 'NORMAL', NULL, '无积水', DATE_SUB(NOW(), INTERVAL 2 DAY)),
(3, 7, 1, '配电室温度', 'ABNORMAL', '42', '℃', DATE_SUB(NOW(), INTERVAL 1 DAY));

-- ---------------- 公示与通知 ----------------
INSERT INTO publish_notice (title, content, notice_type, facility_type, order_id, publish_time, expire_time, status, publish_user_id) VALUES
('1号楼1单元电梯半月保完成公告', '1号楼1单元电梯已于近日完成半月保，制动器、限速器、门系统检查合格，运行正常。维保单位：恒安电梯工程有限公司。', 'REPAIR', 'ELEVATOR', 1, DATE_SUB(NOW(), INTERVAL 18 DAY), DATE_ADD(NOW(), INTERVAL 12 DAY), 'PUBLISHED', 1),
('地下水泵房应急抢修完成，供水已恢复', '10月30日夜间地下水泵房出水压力异常，已更换止回阀，当日恢复供水。给您带来不便敬请谅解。', 'WATER', 'WATER', 7, DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_ADD(NOW(), INTERVAL 10 DAY), 'PUBLISHED', 1),
('中心花园绿化施药安全提示', '本周五上午对中心花园进行病虫害防治施药，施药后 24 小时内请勿在草坪逗留，请看护好儿童与宠物。', 'GREEN', 'GARDEN', NULL, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_ADD(NOW(), INTERVAL 5 DAY), 'PUBLISHED', 1);

INSERT INTO sys_message (user_id, msg_type, title, content, biz_type, biz_id, read_flag, create_time) VALUES
(1, 'ORDER', '工单待验收', '工单 WO20261102000003 已提交完工，请及时验收', 'WORK_ORDER', 3, 0, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(1, 'ORDER', '工单超时', '工单 WO20261103000005 已超时未完成', 'WORK_ORDER', 5, 0, DATE_SUB(NOW(), INTERVAL 8 HOUR)),
(2, 'ORDER', '新工单待接单', '工单 WO20261104000006 已派给您，请及时到场', 'WORK_ORDER', 6, 0, DATE_SUB(NOW(), INTERVAL 9 HOUR)),
(3, 'ORDER', '工单催办', '工单 WO20261103000005 已超时，请尽快处理', 'WORK_ORDER', 5, 0, DATE_SUB(NOW(), INTERVAL 8 HOUR)),
(1, 'ANNUAL', '年检临期提醒', '1号楼1单元电梯年检将于近期到期，请提前安排复检', 'EQUIPMENT', 1, 0, DATE_SUB(NOW(), INTERVAL 2 DAY));
