-- ============================================================
-- H2 内存库 schema（零依赖启动用，无 ENGINE/反引号，兼容 MySQL 模式）
-- 仅默认 profile 由 spring.sql.init 自动执行
-- ============================================================

CREATE TABLE IF NOT EXISTS sys_user (
  user_id      BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_name    VARCHAR(50)  DEFAULT '',
  nick_name    VARCHAR(50)  DEFAULT '',
  password     VARCHAR(100) DEFAULT '',
  status       CHAR(1)      DEFAULT '0',
  email        VARCHAR(50)  DEFAULT '',
  phonenumber  VARCHAR(20)  DEFAULT '',
  sex          CHAR(1)      DEFAULT '0',
  create_time  DATETIME,
  remark       VARCHAR(500) DEFAULT ''
);

CREATE TABLE IF NOT EXISTS sys_role (
  role_id    BIGINT AUTO_INCREMENT PRIMARY KEY,
  role_name  VARCHAR(30) DEFAULT '',
  role_key   VARCHAR(100) DEFAULT '',
  status     CHAR(1)     DEFAULT '0',
  create_time DATETIME,
  remark     VARCHAR(500) DEFAULT ''
);

CREATE TABLE IF NOT EXISTS sys_menu (
  menu_id    BIGINT AUTO_INCREMENT PRIMARY KEY,
  parent_id  BIGINT       DEFAULT 0,
  order_num  INT          DEFAULT 0,
  menu_name  VARCHAR(50) DEFAULT '',
  path       VARCHAR(200) DEFAULT '',
  component  VARCHAR(200) DEFAULT '',
  query      VARCHAR(255) DEFAULT '',
  menu_type  CHAR(1)      DEFAULT '',
  perms      VARCHAR(100) DEFAULT '',
  icon       VARCHAR(100) DEFAULT '#',
  status     CHAR(1)      DEFAULT '0',
  create_time DATETIME
);

CREATE TABLE IF NOT EXISTS sys_role_menu (
  id       BIGINT AUTO_INCREMENT PRIMARY KEY,
  role_id BIGINT,
  menu_id BIGINT
);

CREATE TABLE IF NOT EXISTS sys_user_role (
  id       BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id  BIGINT,
  role_id  BIGINT
);

CREATE TABLE IF NOT EXISTS sys_dict_type (
  dict_id    BIGINT AUTO_INCREMENT PRIMARY KEY,
  dict_name  VARCHAR(100) DEFAULT '',
  dict_type  VARCHAR(100) DEFAULT '',
  status     CHAR(1)      DEFAULT '0',
  remark     VARCHAR(500) DEFAULT ''
);

CREATE TABLE IF NOT EXISTS sys_dict_data (
  dict_code   BIGINT AUTO_INCREMENT PRIMARY KEY,
  dict_type   VARCHAR(100) DEFAULT '',
  dict_label  VARCHAR(100) DEFAULT '',
  dict_value  VARCHAR(100) DEFAULT '',
  dict_sort   INT          DEFAULT 0,
  status      CHAR(1)      DEFAULT '0',
  remark      VARCHAR(500) DEFAULT ''
);

CREATE TABLE IF NOT EXISTS sys_config (
  config_id    BIGINT AUTO_INCREMENT PRIMARY KEY,
  config_name  VARCHAR(100) DEFAULT '',
  config_key   VARCHAR(100) DEFAULT '',
  config_value VARCHAR(500) DEFAULT '',
  config_type  CHAR(1)      DEFAULT 'N',
  remark       VARCHAR(500) DEFAULT ''
);

CREATE TABLE IF NOT EXISTS sys_oper_log (
  oper_id        BIGINT AUTO_INCREMENT PRIMARY KEY,
  title          VARCHAR(50)  DEFAULT '',
  business_type  INT           DEFAULT 0,
  method         VARCHAR(200) DEFAULT '',
  request_method VARCHAR(10)  DEFAULT '',
  operator_type  VARCHAR(20)  DEFAULT '',
  oper_name      VARCHAR(50)  DEFAULT '',
  oper_url       VARCHAR(255) DEFAULT '',
  oper_ip        VARCHAR(50)  DEFAULT '',
  oper_param     VARCHAR(2000) DEFAULT '',
  json_result   VARCHAR(500) DEFAULT '',
  status         INT           DEFAULT 0,
  error_msg      VARCHAR(500) DEFAULT '',
  oper_time      DATETIME
);

CREATE TABLE IF NOT EXISTS sys_notice (
  notice_id      BIGINT AUTO_INCREMENT PRIMARY KEY,
  notice_title   VARCHAR(100) DEFAULT '',
  notice_type    CHAR(1)       DEFAULT '1',
  notice_content VARCHAR(2000) DEFAULT '',
  status         CHAR(1)       DEFAULT '0',
  create_by      VARCHAR(50)  DEFAULT '',
  create_time    DATETIME,
  remark         VARCHAR(500) DEFAULT ''
);
