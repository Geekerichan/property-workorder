-- =============================================================
-- 物业公司运维工单管理系统 - 数据库结构初始化脚本
-- 数据库: MySQL 8.0  字符集: utf8mb4  引擎: InnoDB
-- 说明: 全部业务表采用逻辑删除 is_deleted=0/1，不建物理外键，关联完整性由应用层校验
-- 执行: mysql -uroot -p < sql/init.sql
-- =============================================================

CREATE DATABASE IF NOT EXISTS `prop_wo` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `prop_wo`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS sys_user;
DROP TABLE IF EXISTS sys_dict;
DROP TABLE IF EXISTS sys_dict_item;
DROP TABLE IF EXISTS sys_config;
DROP TABLE IF EXISTS sys_log_login;
DROP TABLE IF EXISTS sys_log_oper;
DROP TABLE IF EXISTS sys_message;
DROP TABLE IF EXISTS community;
DROP TABLE IF EXISTS building;
DROP TABLE IF EXISTS unit;
DROP TABLE IF EXISTS house;
DROP TABLE IF EXISTS house_owner;
DROP TABLE IF EXISTS owner_user;
DROP TABLE IF EXISTS company;
DROP TABLE IF EXISTS company_qualification;
DROP TABLE IF EXISTS team;
DROP TABLE IF EXISTS personnel;
DROP TABLE IF EXISTS equipment;
DROP TABLE IF EXISTS equipment_ext;
DROP TABLE IF EXISTS equipment_qr;
DROP TABLE IF EXISTS annual_check;
DROP TABLE IF EXISTS equipment_status_log;
DROP TABLE IF EXISTS checklist_template;
DROP TABLE IF EXISTS checklist_item;
DROP TABLE IF EXISTS work_order_checklist;
DROP TABLE IF EXISTS maintenance_plan;
DROP TABLE IF EXISTS plan_device_rel;
DROP TABLE IF EXISTS seasonal_node;
DROP TABLE IF EXISTS plan_generate_log;
DROP TABLE IF EXISTS work_order;
DROP TABLE IF EXISTS work_order_log;
DROP TABLE IF EXISTS work_order_claim;
DROP TABLE IF EXISTS work_order_assign;
DROP TABLE IF EXISTS work_order_settlement;
DROP TABLE IF EXISTS contract;
DROP TABLE IF EXISTS contract_confirm;
DROP TABLE IF EXISTS responsibility_link;
DROP TABLE IF EXISTS evidence_checkin;
DROP TABLE IF EXISTS evidence_photo;
DROP TABLE IF EXISTS evidence_record;
DROP TABLE IF EXISTS inspection_route;
DROP TABLE IF EXISTS inspection_point;
DROP TABLE IF EXISTS inspection_check_item;
DROP TABLE IF EXISTS inspection_task;
DROP TABLE IF EXISTS inspection_task_point;
DROP TABLE IF EXISTS inspection_record;
DROP TABLE IF EXISTS acceptance_record;
DROP TABLE IF EXISTS credit_record;
DROP TABLE IF EXISTS publish_notice;
DROP TABLE IF EXISTS export_log;

-- -------------------------------------------------------------
-- 1. 系统管理（M10）
-- -------------------------------------------------------------
CREATE TABLE sys_user (
  id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  username      VARCHAR(50)  NOT NULL COMMENT '登录账号',
  password      VARCHAR(120) NOT NULL COMMENT 'BCrypt 密码',
  real_name     VARCHAR(50)  NOT NULL COMMENT '姓名',
  phone         VARCHAR(11)  DEFAULT NULL COMMENT '手机号',
  role_code     VARCHAR(20)  NOT NULL COMMENT '角色: ADMIN/WORKER/COMPANY/OWNER',
  company_id    BIGINT       DEFAULT NULL COMMENT '维保公司ID（公司管理员与公司人员）',
  personnel_id  BIGINT       DEFAULT NULL COMMENT '关联人员档案ID',
  status        VARCHAR(20)  NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED/DISABLED',
  remark        VARCHAR(255) DEFAULT NULL COMMENT '备注',
  create_by     BIGINT       DEFAULT NULL,
  create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_by     BIGINT       DEFAULT NULL,
  update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted    TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_username (username),
  KEY idx_user_role (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户';

CREATE TABLE sys_dict (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  dict_type   VARCHAR(50)  NOT NULL COMMENT '字典类型编码',
  dict_name   VARCHAR(80)  NOT NULL COMMENT '字典名称',
  status      VARCHAR(20)  NOT NULL DEFAULT 'ENABLED',
  remark      VARCHAR(255) DEFAULT NULL,
  create_by   BIGINT       DEFAULT NULL,
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_by   BIGINT       DEFAULT NULL,
  update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted  TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_dict_type (dict_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字典';

CREATE TABLE sys_dict_item (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  dict_type   VARCHAR(50)  NOT NULL COMMENT '字典类型编码',
  item_label  VARCHAR(80)  NOT NULL COMMENT '显示名称',
  item_value  VARCHAR(50)  NOT NULL COMMENT '字典值',
  sort_no     INT          NOT NULL DEFAULT 0,
  status      VARCHAR(20)  NOT NULL DEFAULT 'ENABLED',
  remark      VARCHAR(255) DEFAULT NULL,
  create_by   BIGINT       DEFAULT NULL,
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_by   BIGINT       DEFAULT NULL,
  update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted  TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_dict_item_type (dict_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字典项';

CREATE TABLE sys_config (
  id           BIGINT       NOT NULL AUTO_INCREMENT,
  config_key   VARCHAR(80)  NOT NULL COMMENT '参数键',
  config_value VARCHAR(500) NOT NULL COMMENT '参数值',
  config_name  VARCHAR(80)  NOT NULL COMMENT '参数名称',
  remark       VARCHAR(255) DEFAULT NULL,
  create_by    BIGINT       DEFAULT NULL,
  create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_by    BIGINT       DEFAULT NULL,
  update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted   TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_config_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统参数';

CREATE TABLE sys_log_login (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  username    VARCHAR(50)  NOT NULL,
  ip          VARCHAR(64)  DEFAULT NULL,
  user_agent  VARCHAR(255) DEFAULT NULL,
  success     TINYINT      NOT NULL DEFAULT 1,
  message     VARCHAR(255) DEFAULT NULL,
  login_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  is_deleted  TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_login_time (login_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录日志';

CREATE TABLE sys_log_oper (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  module        VARCHAR(50)  NOT NULL COMMENT '模块',
  action        VARCHAR(80)  NOT NULL COMMENT '动作',
  biz_id        BIGINT       DEFAULT NULL COMMENT '业务主键',
  content       VARCHAR(500) DEFAULT NULL COMMENT '摘要',
  before_data   TEXT         DEFAULT NULL COMMENT '变更前',
  after_data    TEXT         DEFAULT NULL COMMENT '变更后',
  ip            VARCHAR(64)  DEFAULT NULL,
  operator_id   BIGINT       DEFAULT NULL,
  operator_name VARCHAR(50)  DEFAULT NULL,
  oper_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  is_deleted    TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_oper_time (oper_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志';

CREATE TABLE sys_message (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  user_id     BIGINT       NOT NULL COMMENT '接收人',
  msg_type    VARCHAR(30)  NOT NULL COMMENT 'ORDER/URGE/REJECT/CONTRACT/ANNUAL/SYSTEM',
  title       VARCHAR(120) NOT NULL,
  content     VARCHAR(1000) DEFAULT NULL,
  biz_type    VARCHAR(30)  DEFAULT NULL,
  biz_id      BIGINT       DEFAULT NULL,
  read_flag   TINYINT      NOT NULL DEFAULT 0,
  read_time   DATETIME     DEFAULT NULL,
  create_by   BIGINT       DEFAULT NULL,
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_by   BIGINT       DEFAULT NULL,
  update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted  TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_msg_user (user_id, read_flag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内信';

-- -------------------------------------------------------------
-- 2. 楼盘与业主档案（M12）
-- -------------------------------------------------------------
CREATE TABLE community (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  name          VARCHAR(80)  NOT NULL COMMENT '小区名称',
  address       VARCHAR(255) DEFAULT NULL,
  contact_name  VARCHAR(50)  DEFAULT NULL,
  contact_phone VARCHAR(11)  DEFAULT NULL,
  build_year    VARCHAR(10)  DEFAULT NULL,
  remark        VARCHAR(255) DEFAULT NULL,
  create_by     BIGINT       DEFAULT NULL,
  create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_by     BIGINT       DEFAULT NULL,
  update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted    TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='小区';

CREATE TABLE building (
  id           BIGINT       NOT NULL AUTO_INCREMENT,
  community_id BIGINT       NOT NULL,
  name         VARCHAR(50)  NOT NULL COMMENT '楼栋名称',
  floors       INT          NOT NULL DEFAULT 0,
  unit_count   INT          NOT NULL DEFAULT 0,
  remark       VARCHAR(255) DEFAULT NULL,
  create_by    BIGINT       DEFAULT NULL,
  create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_by    BIGINT       DEFAULT NULL,
  update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted   TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_building_community (community_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='楼栋';

CREATE TABLE unit (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  community_id BIGINT      NOT NULL,
  building_id BIGINT       NOT NULL,
  name        VARCHAR(50)  NOT NULL COMMENT '单元名称',
  floors      INT          NOT NULL DEFAULT 0,
  remark      VARCHAR(255) DEFAULT NULL,
  create_by   BIGINT       DEFAULT NULL,
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_by   BIGINT       DEFAULT NULL,
  update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted  TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_unit_building (building_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='单元';

CREATE TABLE house (
  id           BIGINT       NOT NULL AUTO_INCREMENT,
  community_id BIGINT       NOT NULL,
  building_id  BIGINT       NOT NULL,
  unit_id      BIGINT       NOT NULL,
  room_no      VARCHAR(30)  NOT NULL COMMENT '房号，如 1-602',
  floor_no     INT          NOT NULL DEFAULT 0,
  area         DECIMAL(10,2) DEFAULT NULL COMMENT '建筑面积',
  house_type   VARCHAR(30)  DEFAULT NULL COMMENT '住宅/商铺/车位',
  status       VARCHAR(20)  NOT NULL DEFAULT 'NORMAL' COMMENT 'NORMAL/EMPTY',
  remark       VARCHAR(255) DEFAULT NULL,
  create_by    BIGINT       DEFAULT NULL,
  create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_by    BIGINT       DEFAULT NULL,
  update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted   TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_house_unit (unit_id),
  KEY idx_house_room (room_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='房屋';

CREATE TABLE owner_user (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  phone       VARCHAR(11)  NOT NULL COMMENT '手机号（登录名）',
  name        VARCHAR(50)  NOT NULL,
  remark      VARCHAR(255) DEFAULT NULL,
  create_by   BIGINT       DEFAULT NULL,
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_by   BIGINT       DEFAULT NULL,
  update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted  TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_owner_phone (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='业主账号';

CREATE TABLE house_owner (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  house_id    BIGINT       NOT NULL,
  owner_id    BIGINT       NOT NULL,
  relation    VARCHAR(20)  NOT NULL DEFAULT 'OWNER' COMMENT 'OWNER/FAMILY/TENANT',
  is_primary  TINYINT      NOT NULL DEFAULT 0 COMMENT '是否主业主',
  remark      VARCHAR(255) DEFAULT NULL,
  create_by   BIGINT       DEFAULT NULL,
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_by   BIGINT       DEFAULT NULL,
  update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted  TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_house_owner (house_id, owner_id),
  KEY idx_ho_owner (owner_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='房屋业主绑定';

-- -------------------------------------------------------------
-- 3. 维保公司与班组人员（M12）
-- -------------------------------------------------------------
CREATE TABLE company (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  name          VARCHAR(100) NOT NULL COMMENT '公司名称',
  credit_code   VARCHAR(50)  DEFAULT NULL COMMENT '统一社会信用代码',
  contact_name  VARCHAR(50)  DEFAULT NULL,
  contact_phone VARCHAR(11)  DEFAULT NULL,
  address       VARCHAR(255) DEFAULT NULL,
  status        VARCHAR(20)  NOT NULL DEFAULT 'ENABLED',
  remark        VARCHAR(255) DEFAULT NULL,
  create_by     BIGINT       DEFAULT NULL,
  create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_by     BIGINT       DEFAULT NULL,
  update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted    TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='维保公司';

CREATE TABLE company_qualification (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  company_id  BIGINT       NOT NULL,
  qual_type   VARCHAR(60)  NOT NULL COMMENT '资质类型，如 电梯安装改造修理许可证',
  cert_no     VARCHAR(60)  DEFAULT NULL,
  expire_date DATE         DEFAULT NULL,
  file_path   VARCHAR(255) DEFAULT NULL,
  remark      VARCHAR(255) DEFAULT NULL,
  create_by   BIGINT       DEFAULT NULL,
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_by   BIGINT       DEFAULT NULL,
  update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted  TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_qual_company (company_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公司资质';

CREATE TABLE team (
  id           BIGINT       NOT NULL AUTO_INCREMENT,
  name         VARCHAR(50)  NOT NULL COMMENT '班组名称',
  team_type    VARCHAR(50)  NOT NULL COMMENT '维修类型：水电/电梯/暖通/安防/绿化',
  community_id BIGINT       DEFAULT NULL,
  leader_id    BIGINT       DEFAULT NULL,
  remark       VARCHAR(255) DEFAULT NULL,
  create_by    BIGINT       DEFAULT NULL,
  create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_by    BIGINT       DEFAULT NULL,
  update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted   TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='班组';

CREATE TABLE personnel (
  id               BIGINT       NOT NULL AUTO_INCREMENT,
  name             VARCHAR(50)  NOT NULL,
  phone            VARCHAR(11)  NOT NULL,
  team_id          BIGINT       DEFAULT NULL COMMENT '所属班组（内部班组）',
  company_id       BIGINT       DEFAULT NULL COMMENT '所属维保公司',
  user_id          BIGINT       DEFAULT NULL COMMENT '关联登录账号',
  skills           VARCHAR(255) DEFAULT NULL COMMENT '技能标签，逗号分隔',
  cert_name        VARCHAR(60)  DEFAULT NULL COMMENT '资质证件名称',
  cert_no          VARCHAR(60)  DEFAULT NULL,
  cert_expire_date DATE         DEFAULT NULL,
  duty_status      VARCHAR(20)  NOT NULL DEFAULT 'ON_DUTY' COMMENT 'ON_DUTY/LEAVE/RESIGNED',
  remark           VARCHAR(255) DEFAULT NULL,
  create_by        BIGINT       DEFAULT NULL,
  create_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_by        BIGINT       DEFAULT NULL,
  update_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted       TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_personnel_company (company_id),
  KEY idx_personnel_team (team_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='维保/维修人员';

-- -------------------------------------------------------------
-- 4. 设施设备台账（M8）
-- -------------------------------------------------------------
CREATE TABLE equipment (
  id                BIGINT        NOT NULL AUTO_INCREMENT,
  code              VARCHAR(60)   NOT NULL COMMENT '设备编号',
  name              VARCHAR(120)  NOT NULL COMMENT '设备名称',
  facility_type     VARCHAR(20)   NOT NULL COMMENT 'ELEVATOR/WATER/POWER/HVAC/SECURITY/GARDEN/FIRE',
  community_id      BIGINT        DEFAULT NULL,
  building_id       BIGINT        DEFAULT NULL,
  location          VARCHAR(255)  DEFAULT NULL COMMENT '安装位置',
  commission_date   DATE          DEFAULT NULL COMMENT '投用日期',
  status            VARCHAR(20)   NOT NULL DEFAULT 'NORMAL' COMMENT 'NORMAL/REPAIR/LOCKED',
  maintain_overdue  TINYINT       NOT NULL DEFAULT 0 COMMENT '是否维保逾期',
  company_id        BIGINT        DEFAULT NULL COMMENT '维保单位',
  safety_manager    VARCHAR(50)   DEFAULT NULL,
  safety_phone      VARCHAR(11)   DEFAULT NULL,
  next_annual_date  DATE          DEFAULT NULL COMMENT '下次年检日期',
  longitude         DECIMAL(10,6) DEFAULT NULL,
  latitude          DECIMAL(10,6) DEFAULT NULL,
  monitor_data      JSON          DEFAULT NULL COMMENT '二期物联网数据预留',
  remark            VARCHAR(500)  DEFAULT NULL,
  create_by         BIGINT        DEFAULT NULL,
  create_time       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_by         BIGINT        DEFAULT NULL,
  update_time       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted        TINYINT       NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_equipment_code (code),
  KEY idx_equip_type (facility_type),
  KEY idx_equip_community (community_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备台账（一物一档）';

CREATE TABLE equipment_ext (
  id           BIGINT      NOT NULL AUTO_INCREMENT,
  equipment_id BIGINT      NOT NULL,
  ext_json     LONGTEXT    COMMENT '分类型扩展参数 JSON',
  create_by    BIGINT      DEFAULT NULL,
  create_time  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_by    BIGINT      DEFAULT NULL,
  update_time  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted   TINYINT     NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_ext_equipment (equipment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备扩展参数';

CREATE TABLE equipment_qr (
  id             BIGINT       NOT NULL AUTO_INCREMENT,
  equipment_id   BIGINT       NOT NULL,
  qr_code        VARCHAR(80)  NOT NULL COMMENT '二维码业务编码',
  sign           VARCHAR(80)  NOT NULL COMMENT 'HMAC 签名',
  content        VARCHAR(500) NOT NULL COMMENT '二维码内容（URL）',
  print_count    INT          NOT NULL DEFAULT 0,
  last_print_time DATETIME    DEFAULT NULL,
  create_by      BIGINT       DEFAULT NULL,
  create_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_by      BIGINT       DEFAULT NULL,
  update_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted     TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_qr_equipment (equipment_id),
  UNIQUE KEY uk_qr_code (qr_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备二维码';

CREATE TABLE annual_check (
  id           BIGINT       NOT NULL AUTO_INCREMENT,
  equipment_id BIGINT       NOT NULL,
  check_date   DATE         NOT NULL COMMENT '检验日期',
  expire_date  DATE         NOT NULL COMMENT '有效期至',
  result       VARCHAR(20)  NOT NULL DEFAULT 'PASS' COMMENT 'PASS/FAIL',
  agency       VARCHAR(100) DEFAULT NULL COMMENT '检验机构',
  report_no    VARCHAR(60)  DEFAULT NULL COMMENT '报告编号',
  file_path    VARCHAR(255) DEFAULT NULL,
  remark       VARCHAR(255) DEFAULT NULL,
  create_by    BIGINT       DEFAULT NULL,
  create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_by    BIGINT       DEFAULT NULL,
  update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted   TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_annual_equipment (equipment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='年检记录';

CREATE TABLE equipment_status_log (
  id           BIGINT       NOT NULL AUTO_INCREMENT,
  equipment_id BIGINT       NOT NULL,
  from_status  VARCHAR(20)  DEFAULT NULL,
  to_status    VARCHAR(20)  NOT NULL,
  reason       VARCHAR(255) DEFAULT NULL,
  operator_id  BIGINT       DEFAULT NULL,
  operator_name VARCHAR(50) DEFAULT NULL,
  create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  is_deleted   TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_estatus_equipment (equipment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备状态变更日志';

-- -------------------------------------------------------------
-- 5. 作业清单模板（M3）
-- -------------------------------------------------------------
CREATE TABLE checklist_template (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  name          VARCHAR(80)  NOT NULL,
  facility_type VARCHAR(20)  NOT NULL,
  status        VARCHAR(20)  NOT NULL DEFAULT 'ENABLED',
  remark        VARCHAR(255) DEFAULT NULL,
  create_by     BIGINT       DEFAULT NULL,
  create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_by     BIGINT       DEFAULT NULL,
  update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted    TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_tpl_type (facility_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='作业清单模板';

CREATE TABLE checklist_item (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  template_id   BIGINT       NOT NULL,
  group_name    VARCHAR(50)  NOT NULL COMMENT '分组：机房/轿厢/井道/层站等',
  item_name     VARCHAR(120) NOT NULL,
  require_photo TINYINT      NOT NULL DEFAULT 0 COMMENT '是否强制拍照',
  require_value TINYINT      NOT NULL DEFAULT 0 COMMENT '是否需填读数',
  unit          VARCHAR(20)  DEFAULT NULL COMMENT '读数单位',
  sort_no       INT          NOT NULL DEFAULT 0,
  create_by     BIGINT       DEFAULT NULL,
  create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_by     BIGINT       DEFAULT NULL,
  update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted    TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_citem_tpl (template_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='作业清单项';

CREATE TABLE work_order_checklist (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  order_id      BIGINT       NOT NULL,
  item_id       BIGINT       NOT NULL,
  group_name    VARCHAR(50)  DEFAULT NULL,
  item_name     VARCHAR(120) NOT NULL,
  result        VARCHAR(20)  NOT NULL DEFAULT 'NORMAL' COMMENT 'NORMAL/ABNORMAL',
  item_value    VARCHAR(50)  DEFAULT NULL,
  remark        VARCHAR(255) DEFAULT NULL,
  photo_ids     VARCHAR(255) DEFAULT NULL COMMENT '关联照片ID，逗号分隔',
  require_photo TINYINT      NOT NULL DEFAULT 0,
  sort_no       INT          NOT NULL DEFAULT 0,
  create_by     BIGINT       DEFAULT NULL,
  create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_by     BIGINT       DEFAULT NULL,
  update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted    TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_woc_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单作业清单实例';

-- -------------------------------------------------------------
-- 6. 维保计划引擎（M4）
-- -------------------------------------------------------------
CREATE TABLE maintenance_plan (
  id                 BIGINT       NOT NULL AUTO_INCREMENT,
  name               VARCHAR(100) NOT NULL,
  facility_type      VARCHAR(20)  NOT NULL,
  mode               VARCHAR(20)  NOT NULL COMMENT 'PERIODIC 定期 / ON_DEMAND 按需',
  period_days        INT          DEFAULT NULL COMMENT '定期周期天数（电梯15天）',
  interval_days      INT          DEFAULT NULL COMMENT '按需间隔天数（≤90）',
  advance_days       INT          NOT NULL DEFAULT 3 COMMENT '提前生成天数',
  order_type         VARCHAR(20)  NOT NULL DEFAULT 'PLAN_MAINTAIN',
  dispatch_mode      VARCHAR(20)  NOT NULL DEFAULT 'GRAB' COMMENT 'ASSIGN/GRAB',
  template_id        BIGINT       DEFAULT NULL,
  seasonal_flag      TINYINT      NOT NULL DEFAULT 0,
  next_generate_date DATE         DEFAULT NULL,
  last_generate_date DATE         DEFAULT NULL,
  status             VARCHAR(20)  NOT NULL DEFAULT 'ENABLED',
  remark             VARCHAR(255) DEFAULT NULL,
  create_by          BIGINT       DEFAULT NULL,
  create_time        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_by          BIGINT       DEFAULT NULL,
  update_time        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted         TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_plan_type (facility_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='维保计划';

CREATE TABLE plan_device_rel (
  id           BIGINT      NOT NULL AUTO_INCREMENT,
  plan_id      BIGINT      NOT NULL,
  equipment_id BIGINT      NOT NULL,
  create_time  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  is_deleted   TINYINT     NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_plan_equipment (plan_id, equipment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='计划设备关联';

CREATE TABLE seasonal_node (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  name          VARCHAR(80)  NOT NULL COMMENT '节点名称',
  facility_type VARCHAR(20)  NOT NULL,
  month_no      INT          NOT NULL COMMENT '月份',
  day_no        INT          NOT NULL DEFAULT 1,
  template_id   BIGINT       DEFAULT NULL,
  description   VARCHAR(255) DEFAULT NULL,
  enabled       TINYINT      NOT NULL DEFAULT 1,
  create_by     BIGINT       DEFAULT NULL,
  create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_by     BIGINT       DEFAULT NULL,
  update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted    TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='季节保养节点';

CREATE TABLE plan_generate_log (
  id           BIGINT   NOT NULL AUTO_INCREMENT,
  plan_id      BIGINT   NOT NULL,
  period_start DATE     NOT NULL COMMENT '周期起始日（幂等键）',
  order_id     BIGINT   DEFAULT NULL,
  order_count  INT      NOT NULL DEFAULT 1,
  trigger_type VARCHAR(20) NOT NULL DEFAULT 'SCHEDULE' COMMENT 'SCHEDULE/MANUAL',
  create_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  is_deleted   TINYINT  NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_plan_period (plan_id, period_start)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='计划生成日志（幂等）';

-- -------------------------------------------------------------
-- 7. 工单（M1）
-- -------------------------------------------------------------
CREATE TABLE work_order (
  id                   BIGINT        NOT NULL AUTO_INCREMENT,
  order_no             VARCHAR(30)   NOT NULL COMMENT '工单号 WO+日期+6位序列',
  order_type           VARCHAR(30)   NOT NULL COMMENT 'PLAN_MAINTAIN/DEMAND_MAINTAIN/FAULT_REPAIR/EMERGENCY/INSPECTION',
  facility_type        VARCHAR(20)   NOT NULL,
  equipment_id         BIGINT        DEFAULT NULL,
  community_id         BIGINT        DEFAULT NULL,
  building_id          BIGINT        DEFAULT NULL,
  house_id             BIGINT        DEFAULT NULL,
  title                VARCHAR(120)  NOT NULL,
  description          VARCHAR(1000) DEFAULT NULL,
  urgency              VARCHAR(20)   NOT NULL DEFAULT 'NORMAL' COMMENT 'LOW/NORMAL/HIGH/URGENT',
  status               VARCHAR(30)   NOT NULL DEFAULT 'PENDING_DISPATCH',
  source               VARCHAR(20)   NOT NULL DEFAULT 'MANUAL' COMMENT 'PLAN/OWNER/INSPECTION/MANUAL',
  plan_id              BIGINT        DEFAULT NULL,
  dispatch_mode        VARCHAR(20)   NOT NULL DEFAULT 'ASSIGN' COMMENT 'ASSIGN/GRAB',
  deadline             DATETIME      DEFAULT NULL,
  main_worker_id       BIGINT        DEFAULT NULL COMMENT '主责人 personnel.id',
  team_id              BIGINT        DEFAULT NULL,
  contract_id          BIGINT        DEFAULT NULL,
  responsibility_start DATETIME      DEFAULT NULL,
  responsibility_end   DATETIME      DEFAULT NULL,
  accept_result        VARCHAR(20)   DEFAULT NULL COMMENT 'PASS/REJECT',
  satisfaction_score   INT           DEFAULT NULL COMMENT '1-5',
  publish_flag         TINYINT       NOT NULL DEFAULT 0,
  reporter_user_id     BIGINT        DEFAULT NULL,
  reporter_name        VARCHAR(50)   DEFAULT NULL,
  reporter_phone       VARCHAR(11)   DEFAULT NULL,
  reward_amount        DECIMAL(10,2) DEFAULT NULL COMMENT '单次报酬',
  finish_time          DATETIME      DEFAULT NULL,
  archive_time         DATETIME      DEFAULT NULL,
  cancel_reason        VARCHAR(255)  DEFAULT NULL,
  urge_count           INT           NOT NULL DEFAULT 0,
  last_urge_time       DATETIME      DEFAULT NULL COMMENT '最近催办时间',
  remark               VARCHAR(500)  DEFAULT NULL,
  create_by            BIGINT        DEFAULT NULL,
  create_time          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_by            BIGINT        DEFAULT NULL,
  update_time          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted           TINYINT       NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_order_no (order_no),
  KEY idx_wo_status_time (status, create_time),
  KEY idx_wo_worker (main_worker_id, status),
  KEY idx_wo_equipment (equipment_id, create_time),
  KEY idx_wo_reporter (reporter_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='运维工单';

CREATE TABLE work_order_log (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  order_id      BIGINT       NOT NULL,
  action        VARCHAR(40)  NOT NULL COMMENT 'CREATE/DISPATCH/CLAIM/START/SUBMIT/ACCEPT/REJECT/RETURN/CANCEL/ARCHIVE/URGE',
  from_status   VARCHAR(30)  DEFAULT NULL,
  to_status     VARCHAR(30)  DEFAULT NULL,
  reason        VARCHAR(500) DEFAULT NULL,
  operator_id   BIGINT       DEFAULT NULL,
  operator_name VARCHAR(50)  DEFAULT NULL,
  create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  is_deleted    TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_wolog_order (order_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单流转日志';

CREATE TABLE work_order_claim (
  id           BIGINT   NOT NULL AUTO_INCREMENT,
  order_id     BIGINT   NOT NULL,
  personnel_id BIGINT   NOT NULL,
  user_id      BIGINT   DEFAULT NULL,
  claim_time   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  success      TINYINT  NOT NULL DEFAULT 1,
  fail_reason  VARCHAR(120) DEFAULT NULL,
  is_deleted   TINYINT  NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_claim_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='抢单记录';

CREATE TABLE work_order_assign (
  id           BIGINT       NOT NULL AUTO_INCREMENT,
  order_id     BIGINT       NOT NULL,
  personnel_id BIGINT       NOT NULL,
  assign_type  VARCHAR(20)  NOT NULL DEFAULT 'MAIN' COMMENT 'MAIN/ASSIST',
  remark       VARCHAR(255) DEFAULT NULL,
  assign_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  is_deleted   TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_assign_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='派单与协助人员';

CREATE TABLE work_order_settlement (
  id           BIGINT        NOT NULL AUTO_INCREMENT,
  order_id     BIGINT        NOT NULL,
  company_id   BIGINT        DEFAULT NULL,
  personnel_id BIGINT        DEFAULT NULL,
  amount       DECIMAL(10,2) NOT NULL DEFAULT 0,
  settle_status VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/SETTLED',
  settle_time  DATETIME      DEFAULT NULL,
  remark       VARCHAR(255)  DEFAULT NULL,
  create_by    BIGINT        DEFAULT NULL,
  create_time  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_by    BIGINT        DEFAULT NULL,
  update_time  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted   TINYINT       NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_settle_order (order_id),
  KEY idx_settle_company (company_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单结算记录';

-- -------------------------------------------------------------
-- 8. 电子合同与责任周期（M2）
-- -------------------------------------------------------------
CREATE TABLE contract (
  id                  BIGINT       NOT NULL AUTO_INCREMENT,
  contract_no         VARCHAR(40)  NOT NULL COMMENT '合同编号',
  order_id            BIGINT       NOT NULL,
  equipment_id        BIGINT       DEFAULT NULL,
  equipment_name      VARCHAR(120) DEFAULT NULL,
  party_a             VARCHAR(100) NOT NULL DEFAULT '物业服务中心',
  company_id          BIGINT       DEFAULT NULL COMMENT '乙方维保公司',
  party_b             VARCHAR(100) DEFAULT NULL,
  service_content     VARCHAR(1000) DEFAULT NULL,
  responsibility_days INT          NOT NULL DEFAULT 45,
  resp_start          DATETIME     DEFAULT NULL,
  resp_end            DATETIME     DEFAULT NULL,
  status              VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/ACTIVE/EXPIRED',
  sign_time_a         DATETIME     DEFAULT NULL,
  sign_user_a         VARCHAR(50)  DEFAULT NULL,
  sign_time_b         DATETIME     DEFAULT NULL,
  sign_user_b         VARCHAR(50)  DEFAULT NULL,
  pdf_path            VARCHAR(255) DEFAULT NULL,
  remind_flag         TINYINT      NOT NULL DEFAULT 0,
  create_by           BIGINT       DEFAULT NULL,
  create_time         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_by           BIGINT       DEFAULT NULL,
  update_time         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted          TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_contract_no (contract_no),
  KEY idx_contract_order (order_id),
  KEY idx_contract_equipment (equipment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='单次电子合同';

CREATE TABLE contract_confirm (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  contract_id   BIGINT       NOT NULL,
  party         VARCHAR(2)   NOT NULL COMMENT 'A 物业 / B 接单方',
  operator_id   BIGINT       DEFAULT NULL,
  operator_name VARCHAR(50)  DEFAULT NULL,
  confirm_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  remark        VARCHAR(255) DEFAULT NULL,
  is_deleted    TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_confirm_contract (contract_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合同确认记录';

CREATE TABLE responsibility_link (
  id               BIGINT   NOT NULL AUTO_INCREMENT,
  equipment_id     BIGINT   NOT NULL,
  prev_contract_id BIGINT   NOT NULL,
  next_contract_id BIGINT   NOT NULL,
  create_time      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  is_deleted       TINYINT  NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_link_equipment (equipment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='同设备相邻合同责任链';

-- -------------------------------------------------------------
-- 9. 现场作业留痕（M3）
-- -------------------------------------------------------------
CREATE TABLE evidence_checkin (
  id           BIGINT        NOT NULL AUTO_INCREMENT,
  order_id     BIGINT        NOT NULL,
  personnel_id BIGINT        NOT NULL,
  checkin_type VARCHAR(20)   NOT NULL DEFAULT 'LOCATION' COMMENT 'SCAN/LOCATION',
  longitude    DECIMAL(10,6) DEFAULT NULL,
  latitude     DECIMAL(10,6) DEFAULT NULL,
  address      VARCHAR(255)  DEFAULT NULL,
  distance     INT           DEFAULT NULL COMMENT '与设备距离（米）',
  valid_flag   TINYINT       NOT NULL DEFAULT 1,
  checkin_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  is_deleted   TINYINT       NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_checkin_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='到场打卡';

CREATE TABLE evidence_photo (
  id             BIGINT        NOT NULL AUTO_INCREMENT,
  order_id       BIGINT        NOT NULL,
  checkin_id     BIGINT        DEFAULT NULL,
  task_id        BIGINT        DEFAULT NULL COMMENT '巡检任务ID',
  photo_type     VARCHAR(20)   NOT NULL DEFAULT 'OTHER' COMMENT 'BEFORE/AFTER/KEY/OTHER',
  original_path  VARCHAR(255)  NOT NULL COMMENT '原始图路径',
  watermark_path VARCHAR(255)  NOT NULL COMMENT '水印图路径',
  md5            VARCHAR(32)   NOT NULL COMMENT '水印图MD5',
  longitude      DECIMAL(10,6) DEFAULT NULL,
  latitude       DECIMAL(10,6) DEFAULT NULL,
  shoot_time     DATETIME      NOT NULL COMMENT '拍摄/上传时间',
  operator_id    BIGINT        DEFAULT NULL,
  operator_name  VARCHAR(50)   DEFAULT NULL,
  equipment_name VARCHAR(120)  DEFAULT NULL,
  item_id        BIGINT        DEFAULT NULL,
  remark         VARCHAR(255)  DEFAULT NULL,
  create_time    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  is_deleted     TINYINT       NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_photo_order (order_id),
  KEY idx_photo_task (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='留痕照片（水印）';

CREATE TABLE evidence_record (
  id           BIGINT        NOT NULL AUTO_INCREMENT,
  order_id     BIGINT        NOT NULL,
  personnel_id BIGINT        DEFAULT NULL,
  record_type  VARCHAR(20)   NOT NULL DEFAULT 'PROGRESS' COMMENT 'PROGRESS/HAZARD/REVIEW',
  content      VARCHAR(1000) NOT NULL,
  create_by    BIGINT        DEFAULT NULL,
  create_time  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  is_deleted   TINYINT       NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_erecord_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='现场作业记录';

-- -------------------------------------------------------------
-- 10. 巡检（M11）
-- -------------------------------------------------------------
CREATE TABLE inspection_route (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  name          VARCHAR(80)  NOT NULL,
  community_id  BIGINT       DEFAULT NULL,
  facility_type VARCHAR(20)  NOT NULL,
  frequency     VARCHAR(20)  NOT NULL DEFAULT 'WEEKLY' COMMENT 'DAILY/WEEKLY/MONTHLY',
  leader_id     BIGINT       DEFAULT NULL COMMENT '默认负责人 personnel.id',
  status        VARCHAR(20)  NOT NULL DEFAULT 'ENABLED',
  remark        VARCHAR(255) DEFAULT NULL,
  create_by     BIGINT       DEFAULT NULL,
  create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_by     BIGINT       DEFAULT NULL,
  update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted    TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='巡检路线';

CREATE TABLE inspection_point (
  id           BIGINT       NOT NULL AUTO_INCREMENT,
  route_id     BIGINT       NOT NULL,
  equipment_id BIGINT       DEFAULT NULL,
  point_name   VARCHAR(100) NOT NULL,
  sort_no      INT          NOT NULL DEFAULT 0,
  remark       VARCHAR(255) DEFAULT NULL,
  create_by    BIGINT       DEFAULT NULL,
  create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_by    BIGINT       DEFAULT NULL,
  update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted   TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_point_route (route_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='巡检点位';

CREATE TABLE inspection_check_item (
  id           BIGINT       NOT NULL AUTO_INCREMENT,
  point_id     BIGINT       NOT NULL,
  item_name    VARCHAR(120) NOT NULL,
  require_value TINYINT     NOT NULL DEFAULT 0,
  unit         VARCHAR(20)  DEFAULT NULL,
  sort_no      INT          NOT NULL DEFAULT 0,
  is_deleted   TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_icitem_point (point_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='巡检点位检查项';

CREATE TABLE inspection_task (
  id             BIGINT       NOT NULL AUTO_INCREMENT,
  task_no        VARCHAR(30)  NOT NULL,
  route_id       BIGINT       NOT NULL,
  plan_date      DATE         NOT NULL,
  assignee_id    BIGINT       DEFAULT NULL,
  status         VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/DOING/DONE',
  finish_time    DATETIME     DEFAULT NULL,
  abnormal_count INT          NOT NULL DEFAULT 0,
  create_by      BIGINT       DEFAULT NULL,
  create_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_by      BIGINT       DEFAULT NULL,
  update_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted     TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_task_no (task_no),
  KEY idx_task_assignee (assignee_id, plan_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='巡检任务';

CREATE TABLE inspection_task_point (
  id           BIGINT        NOT NULL AUTO_INCREMENT,
  task_id      BIGINT        NOT NULL,
  point_id     BIGINT        NOT NULL,
  point_name   VARCHAR(100)  DEFAULT NULL,
  equipment_id BIGINT        DEFAULT NULL,
  status       VARCHAR(20)   NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/DONE',
  result       VARCHAR(20)   DEFAULT NULL COMMENT 'NORMAL/ABNORMAL',
  checkin_time DATETIME      DEFAULT NULL,
  longitude    DECIMAL(10,6) DEFAULT NULL,
  latitude     DECIMAL(10,6) DEFAULT NULL,
  remark       VARCHAR(255)  DEFAULT NULL,
  sort_no      INT           NOT NULL DEFAULT 0,
  is_deleted   TINYINT       NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_tpoint_task (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='巡检任务点位';

CREATE TABLE inspection_record (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  task_id       BIGINT       NOT NULL,
  task_point_id BIGINT       NOT NULL,
  item_id       BIGINT       DEFAULT NULL,
  item_name     VARCHAR(120) NOT NULL,
  result        VARCHAR(20)  NOT NULL DEFAULT 'NORMAL',
  item_value    VARCHAR(50)  DEFAULT NULL,
  photo_path    VARCHAR(255) DEFAULT NULL,
  remark        VARCHAR(255) DEFAULT NULL,
  create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  is_deleted    TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_irecord_task (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='巡检记录';

-- -------------------------------------------------------------
-- 11. 验收稽核 / 信用 / 公示 / 导出 / 文件
-- -------------------------------------------------------------
CREATE TABLE acceptance_record (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  order_id      BIGINT       NOT NULL,
  result        VARCHAR(20)  NOT NULL COMMENT 'PASS/REJECT',
  reason        VARCHAR(500) DEFAULT NULL,
  check_detail  VARCHAR(1000) DEFAULT NULL COMMENT '自动校验结果摘要',
  deduct_score  INT          NOT NULL DEFAULT 0,
  operator_id   BIGINT       DEFAULT NULL,
  operator_name VARCHAR(50)  DEFAULT NULL,
  create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  is_deleted    TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_accept_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='验收记录';

CREATE TABLE credit_record (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  target_type   VARCHAR(20)  NOT NULL COMMENT 'COMPANY/PERSONNEL',
  target_id     BIGINT       NOT NULL,
  target_name   VARCHAR(100) DEFAULT NULL,
  order_id      BIGINT       DEFAULT NULL,
  credit_type   VARCHAR(20)  NOT NULL COMMENT 'EVALUATE 评价 / DEDUCT 扣分',
  score         INT          NOT NULL DEFAULT 0,
  reason        VARCHAR(255) DEFAULT NULL,
  operator_id   BIGINT       DEFAULT NULL,
  operator_name VARCHAR(50)  DEFAULT NULL,
  create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  is_deleted    TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_credit_target (target_type, target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='信用记录';

CREATE TABLE publish_notice (
  id             BIGINT       NOT NULL AUTO_INCREMENT,
  title          VARCHAR(120) NOT NULL,
  content        VARCHAR(2000) NOT NULL,
  notice_type    VARCHAR(20)  NOT NULL DEFAULT 'REPAIR' COMMENT 'REPAIR/WATER/ELECTRIC/GREEN/OTHER',
  facility_type  VARCHAR(20)  DEFAULT NULL,
  order_id       BIGINT       DEFAULT NULL,
  publish_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  expire_time    DATETIME     DEFAULT NULL,
  status         VARCHAR(20)  NOT NULL DEFAULT 'PUBLISHED',
  publish_user_id BIGINT      DEFAULT NULL,
  create_by      BIGINT       DEFAULT NULL,
  create_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_by      BIGINT       DEFAULT NULL,
  update_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted     TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_notice_time (publish_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对外公示';

CREATE TABLE export_log (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  export_type   VARCHAR(40)  NOT NULL COMMENT 'REGULATORY/RESPONSIBILITY/INSPECTION/HOUSE/WORKORDER/CONTRACT_PDF',
  params        VARCHAR(1000) DEFAULT NULL,
  file_path     VARCHAR(255) DEFAULT NULL,
  record_count  INT          NOT NULL DEFAULT 0,
  operator_id   BIGINT       DEFAULT NULL,
  operator_name VARCHAR(50)  DEFAULT NULL,
  create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  is_deleted    TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='导出记录';

SET FOREIGN_KEY_CHECKS = 1;

-- =============================================================
-- 字典与系统参数初始值
-- =============================================================
INSERT INTO sys_dict (dict_type, dict_name, remark) VALUES
('facility_type', '设施类型', '七类设施，消防仅做合规隔离'),
('order_type', '工单业务类型', '五类业务'),
('order_status', '工单状态', '单向流转'),
('urgency', '紧急等级', ''),
('order_source', '工单触发源', '四类触发源'),
('duty_status', '人员在岗状态', ''),
('personnel_cert', '人员资质类型', ''),
('notice_type', '公示类型', '');

INSERT INTO sys_dict_item (dict_type, item_label, item_value, sort_no) VALUES
('facility_type', '电梯', 'ELEVATOR', 1),
('facility_type', '给排水', 'WATER', 2),
('facility_type', '供配电', 'POWER', 3),
('facility_type', '暖通空调', 'HVAC', 4),
('facility_type', '安防监控', 'SECURITY', 5),
('facility_type', '园林绿化', 'GARDEN', 6),
('facility_type', '消防（合规隔离）', 'FIRE', 7),
('order_type', '计划性维保', 'PLAN_MAINTAIN', 1),
('order_type', '按需维保', 'DEMAND_MAINTAIN', 2),
('order_type', '故障报修', 'FAULT_REPAIR', 3),
('order_type', '应急处置', 'EMERGENCY', 4),
('order_type', '日常巡检', 'INSPECTION', 5),
('order_status', '待派单', 'PENDING_DISPATCH', 1),
('order_status', '已派单', 'DISPATCHED', 2),
('order_status', '处理中', 'PROCESSING', 3),
('order_status', '待回访', 'PENDING_REVIEW', 4),
('order_status', '归档', 'ARCHIVED', 5),
('order_status', '已作废', 'CANCELED', 6),
('urgency', '低', 'LOW', 1),
('urgency', '一般', 'NORMAL', 2),
('urgency', '紧急', 'HIGH', 3),
('urgency', '特急', 'URGENT', 4),
('order_source', '计划引擎', 'PLAN', 1),
('order_source', '业主上报', 'OWNER', 2),
('order_source', '巡检异常', 'INSPECTION', 3),
('order_source', '手动创建', 'MANUAL', 4),
('duty_status', '在岗', 'ON_DUTY', 1),
('duty_status', '请假', 'LEAVE', 2),
('duty_status', '离职', 'RESIGNED', 3),
('personnel_cert', '电工证', 'ELECTRICIAN', 1),
('personnel_cert', '电梯作业证', 'ELEVATOR', 2),
('personnel_cert', '焊工证', 'WELDER', 3),
('personnel_cert', '高处作业证', 'HEIGHT', 4),
('notice_type', '设施维修', 'REPAIR', 1),
('notice_type', '停水通知', 'WATER', 2),
('notice_type', '停电通知', 'ELECTRIC', 3),
('notice_type', '绿化施药', 'GREEN', 4),
('notice_type', '其他', 'OTHER', 5);

INSERT INTO sys_config (config_key, config_value, config_name, remark) VALUES
('RESP_PERIOD_DAYS', '45', '责任周期天数', '默认45天，可配45-60天'),
('RESP_REMIND_DAYS', '7', '责任周期到期提醒天数', ''),
('PHOTO_MAX_MB', '5', '照片大小上限(MB)', ''),
('CHECKIN_RADIUS_METER', '200', '定位打卡半径(米)', ''),
('PLAN_ADVANCE_DAYS', '3', '计划提前生成天数', ''),
('DEDUCT_SCORE_REJECT', '2', '验收驳回扣分', ''),
('ANNUAL_REMIND_DAYS', '30,15,7', '年检提醒天数', ''),
('SMS_FIXED_CODE', '123456', '开发环境短信验证码', '');
