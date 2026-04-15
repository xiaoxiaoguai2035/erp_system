-- 面向制造企业的ERP管理系统 MySQL 8 建表脚本
-- 说明：
-- 1. 本脚本基于当前毕业设计数据库设计文档生成
-- 2. 适用于 MySQL 8.x
-- 3. 默认创建并使用 manufacturing_erp 数据库，如需修改可自行替换数据库名

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE DATABASE IF NOT EXISTS `manufacturing_erp`
DEFAULT CHARACTER SET utf8mb4
DEFAULT COLLATE utf8mb4_unicode_ci;

USE `manufacturing_erp`;

-- =========================
-- 1. 系统管理相关表
-- =========================

CREATE TABLE IF NOT EXISTS `sys_role` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `code` VARCHAR(50) NOT NULL COMMENT '角色编码',
  `name` VARCHAR(50) NOT NULL COMMENT '角色名称',
  `status` VARCHAR(20) NOT NULL DEFAULT 'enabled' COMMENT '状态',
  `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_role_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

CREATE TABLE IF NOT EXISTS `sys_menu` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `parent_id` BIGINT DEFAULT NULL COMMENT '父菜单ID',
  `name` VARCHAR(50) NOT NULL COMMENT '菜单名称',
  `path` VARCHAR(100) DEFAULT NULL COMMENT '路由路径',
  `component` VARCHAR(100) DEFAULT NULL COMMENT '组件路径',
  `menu_type` VARCHAR(20) NOT NULL COMMENT '目录/菜单/按钮',
  `permission_code` VARCHAR(100) DEFAULT NULL COMMENT '权限标识',
  `status` VARCHAR(20) NOT NULL DEFAULT 'enabled' COMMENT '状态',
  PRIMARY KEY (`id`),
  KEY `idx_sys_menu_parent_id` (`parent_id`),
  KEY `idx_sys_menu_menu_type` (`menu_type`),
  KEY `idx_sys_menu_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='菜单表';

CREATE TABLE IF NOT EXISTS `sys_user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username` VARCHAR(50) NOT NULL COMMENT '登录账号',
  `password` VARCHAR(100) NOT NULL COMMENT '密码',
  `real_name` VARCHAR(50) NOT NULL COMMENT '真实姓名',
  `role_id` BIGINT NOT NULL COMMENT '角色ID',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
  `status` VARCHAR(20) NOT NULL DEFAULT 'enabled' COMMENT '状态',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_user_username` (`username`),
  KEY `idx_sys_user_role_id` (`role_id`),
  KEY `idx_sys_user_status` (`status`),
  CONSTRAINT `fk_sys_user_role_id` FOREIGN KEY (`role_id`) REFERENCES `sys_role` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

CREATE TABLE IF NOT EXISTS `sys_role_menu` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `role_id` BIGINT NOT NULL COMMENT '角色ID',
  `menu_id` BIGINT NOT NULL COMMENT '菜单ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_role_menu` (`role_id`, `menu_id`),
  KEY `idx_sys_role_menu_menu_id` (`menu_id`),
  CONSTRAINT `fk_sys_role_menu_role_id` FOREIGN KEY (`role_id`) REFERENCES `sys_role` (`id`),
  CONSTRAINT `fk_sys_role_menu_menu_id` FOREIGN KEY (`menu_id`) REFERENCES `sys_menu` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色菜单关系表';

-- =========================
-- 2. 基础资料相关表
-- =========================

CREATE TABLE IF NOT EXISTS `bas_warehouse` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `code` VARCHAR(50) NOT NULL COMMENT '仓库编码',
  `name` VARCHAR(100) NOT NULL COMMENT '仓库名称',
  `warehouse_type` VARCHAR(20) DEFAULT NULL COMMENT '仓库类型',
  `manager_name` VARCHAR(50) DEFAULT NULL COMMENT '负责人',
  `status` VARCHAR(20) NOT NULL DEFAULT 'enabled' COMMENT '状态',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_bas_warehouse_code` (`code`),
  KEY `idx_bas_warehouse_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='仓库表';

CREATE TABLE IF NOT EXISTS `bas_material` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `code` VARCHAR(50) NOT NULL COMMENT '物料编码',
  `name` VARCHAR(100) NOT NULL COMMENT '物料名称',
  `spec` VARCHAR(100) DEFAULT NULL COMMENT '规格型号',
  `material_type` VARCHAR(20) NOT NULL COMMENT '原料/半成品/成品',
  `unit_code` VARCHAR(30) NOT NULL COMMENT '计量单位编码',
  `safety_stock` DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '安全库存',
  `batch_enabled` TINYINT NOT NULL DEFAULT 0 COMMENT '是否批次管理',
  `default_warehouse_id` BIGINT DEFAULT NULL COMMENT '默认仓库',
  `status` VARCHAR(20) NOT NULL DEFAULT 'enabled' COMMENT '状态',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_bas_material_code` (`code`),
  KEY `idx_bas_material_type` (`material_type`),
  KEY `idx_bas_material_status` (`status`),
  KEY `idx_bas_material_default_warehouse_id` (`default_warehouse_id`),
  CONSTRAINT `fk_bas_material_default_warehouse_id` FOREIGN KEY (`default_warehouse_id`) REFERENCES `bas_warehouse` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='物料表';

CREATE TABLE IF NOT EXISTS `bas_customer` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `code` VARCHAR(50) NOT NULL COMMENT '客户编码',
  `name` VARCHAR(100) NOT NULL COMMENT '客户名称',
  `contact` VARCHAR(50) DEFAULT NULL COMMENT '联系人',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '电话',
  `address` VARCHAR(255) DEFAULT NULL COMMENT '地址',
  `status` VARCHAR(20) NOT NULL DEFAULT 'enabled' COMMENT '状态',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_bas_customer_code` (`code`),
  KEY `idx_bas_customer_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='客户表';

CREATE TABLE IF NOT EXISTS `bas_supplier` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `code` VARCHAR(50) NOT NULL COMMENT '供应商编码',
  `name` VARCHAR(100) NOT NULL COMMENT '供应商名称',
  `contact` VARCHAR(50) DEFAULT NULL COMMENT '联系人',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '电话',
  `address` VARCHAR(255) DEFAULT NULL COMMENT '地址',
  `status` VARCHAR(20) NOT NULL DEFAULT 'enabled' COMMENT '状态',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_bas_supplier_code` (`code`),
  KEY `idx_bas_supplier_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='供应商表';

CREATE TABLE IF NOT EXISTS `bas_bom` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `product_id` BIGINT NOT NULL COMMENT '产品物料ID',
  `version_no` VARCHAR(20) DEFAULT NULL COMMENT '版本号',
  `status` VARCHAR(20) NOT NULL DEFAULT 'enabled' COMMENT '状态',
  `effective_date` DATE DEFAULT NULL COMMENT '生效日期',
  PRIMARY KEY (`id`),
  KEY `idx_bas_bom_product_id` (`product_id`),
  KEY `idx_bas_bom_status` (`status`),
  CONSTRAINT `fk_bas_bom_product_id` FOREIGN KEY (`product_id`) REFERENCES `bas_material` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='BOM主表';

CREATE TABLE IF NOT EXISTS `bas_bom_item` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `bom_id` BIGINT NOT NULL COMMENT 'BOM主表ID',
  `material_id` BIGINT NOT NULL COMMENT '子项物料ID',
  `qty` DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '用量',
  `loss_rate` DECIMAL(8,4) NOT NULL DEFAULT 0 COMMENT '损耗率',
  `sort_no` INT NOT NULL DEFAULT 0 COMMENT '排序',
  PRIMARY KEY (`id`),
  KEY `idx_bas_bom_item_bom_id` (`bom_id`),
  KEY `idx_bas_bom_item_material_id` (`material_id`),
  CONSTRAINT `fk_bas_bom_item_bom_id` FOREIGN KEY (`bom_id`) REFERENCES `bas_bom` (`id`),
  CONSTRAINT `fk_bas_bom_item_material_id` FOREIGN KEY (`material_id`) REFERENCES `bas_material` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='BOM明细表';

CREATE TABLE IF NOT EXISTS `bas_process_route` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `product_id` BIGINT NOT NULL COMMENT '产品物料ID',
  `version_no` VARCHAR(20) DEFAULT NULL COMMENT '版本号',
  `status` VARCHAR(20) NOT NULL DEFAULT 'enabled' COMMENT '状态',
  PRIMARY KEY (`id`),
  KEY `idx_bas_process_route_product_id` (`product_id`),
  KEY `idx_bas_process_route_status` (`status`),
  CONSTRAINT `fk_bas_process_route_product_id` FOREIGN KEY (`product_id`) REFERENCES `bas_material` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工艺路线主表';

CREATE TABLE IF NOT EXISTS `bas_process_route_item` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `route_id` BIGINT NOT NULL COMMENT '工艺路线ID',
  `process_code` VARCHAR(50) NOT NULL COMMENT '工序编号',
  `process_name` VARCHAR(100) NOT NULL COMMENT '工序名称',
  `standard_hours` DECIMAL(10,2) DEFAULT NULL COMMENT '标准工时',
  `work_center` VARCHAR(100) DEFAULT NULL COMMENT '工作中心',
  `sort_no` INT NOT NULL DEFAULT 0 COMMENT '排序',
  PRIMARY KEY (`id`),
  KEY `idx_bas_process_route_item_route_id` (`route_id`),
  CONSTRAINT `fk_bas_process_route_item_route_id` FOREIGN KEY (`route_id`) REFERENCES `bas_process_route` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工艺路线明细表';

-- =========================
-- 3. 采购与销售相关表
-- =========================

CREATE TABLE IF NOT EXISTS `pur_doc` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `code` VARCHAR(50) NOT NULL COMMENT '单据编号',
  `doc_type` VARCHAR(20) NOT NULL COMMENT 'request/order',
  `supplier_id` BIGINT DEFAULT NULL COMMENT '供应商ID',
  `doc_date` DATE NOT NULL COMMENT '单据日期',
  `expected_date` DATE DEFAULT NULL COMMENT '预计到货日期',
  `total_amount` DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '总金额',
  `status` VARCHAR(20) NOT NULL DEFAULT 'draft' COMMENT '状态',
  `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pur_doc_code` (`code`),
  KEY `idx_pur_doc_supplier_id` (`supplier_id`),
  KEY `idx_pur_doc_doc_date` (`doc_date`),
  KEY `idx_pur_doc_status` (`status`),
  CONSTRAINT `fk_pur_doc_supplier_id` FOREIGN KEY (`supplier_id`) REFERENCES `bas_supplier` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='采购单据表';

CREATE TABLE IF NOT EXISTS `pur_doc_item` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `doc_id` BIGINT NOT NULL COMMENT '单据ID',
  `material_id` BIGINT NOT NULL COMMENT '物料ID',
  `warehouse_id` BIGINT DEFAULT NULL COMMENT '需求仓库',
  `qty` DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '单据数量',
  `received_qty` DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '已到货数量',
  `qualified_qty` DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '合格数量',
  `price` DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '单价',
  `amount` DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '金额',
  `need_date` DATE DEFAULT NULL COMMENT '需求日期',
  `lot_no` VARCHAR(50) DEFAULT NULL COMMENT '批次号',
  PRIMARY KEY (`id`),
  KEY `idx_pur_doc_item_doc_id` (`doc_id`),
  KEY `idx_pur_doc_item_material_id` (`material_id`),
  KEY `idx_pur_doc_item_warehouse_id` (`warehouse_id`),
  CONSTRAINT `fk_pur_doc_item_doc_id` FOREIGN KEY (`doc_id`) REFERENCES `pur_doc` (`id`),
  CONSTRAINT `fk_pur_doc_item_material_id` FOREIGN KEY (`material_id`) REFERENCES `bas_material` (`id`),
  CONSTRAINT `fk_pur_doc_item_warehouse_id` FOREIGN KEY (`warehouse_id`) REFERENCES `bas_warehouse` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='采购单据明细表';

CREATE TABLE IF NOT EXISTS `sal_doc` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `code` VARCHAR(50) NOT NULL COMMENT '单据编号',
  `doc_type` VARCHAR(20) NOT NULL COMMENT 'quote/order',
  `customer_id` BIGINT NOT NULL COMMENT '客户ID',
  `doc_date` DATE NOT NULL COMMENT '单据日期',
  `delivery_date` DATE DEFAULT NULL COMMENT '交货日期',
  `total_amount` DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '总金额',
  `status` VARCHAR(20) NOT NULL DEFAULT 'draft' COMMENT '状态',
  `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sal_doc_code` (`code`),
  KEY `idx_sal_doc_customer_id` (`customer_id`),
  KEY `idx_sal_doc_doc_date` (`doc_date`),
  KEY `idx_sal_doc_status` (`status`),
  CONSTRAINT `fk_sal_doc_customer_id` FOREIGN KEY (`customer_id`) REFERENCES `bas_customer` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='销售单据表';

CREATE TABLE IF NOT EXISTS `sal_doc_item` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `doc_id` BIGINT NOT NULL COMMENT '单据ID',
  `material_id` BIGINT NOT NULL COMMENT '产品ID',
  `qty` DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '单据数量',
  `shipped_qty` DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '已发货数量',
  `price` DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '单价',
  `amount` DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '金额',
  PRIMARY KEY (`id`),
  KEY `idx_sal_doc_item_doc_id` (`doc_id`),
  KEY `idx_sal_doc_item_material_id` (`material_id`),
  CONSTRAINT `fk_sal_doc_item_doc_id` FOREIGN KEY (`doc_id`) REFERENCES `sal_doc` (`id`),
  CONSTRAINT `fk_sal_doc_item_material_id` FOREIGN KEY (`material_id`) REFERENCES `bas_material` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='销售单据明细表';

-- =========================
-- 4. 库存与生产相关表
-- =========================

CREATE TABLE IF NOT EXISTS `stk_inventory` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `material_id` BIGINT NOT NULL COMMENT '物料ID',
  `warehouse_id` BIGINT NOT NULL COMMENT '仓库ID',
  `lot_no` VARCHAR(50) DEFAULT NULL COMMENT '批次号',
  `qty` DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '当前库存',
  `locked_qty` DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '锁定库存',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_stk_inventory_material_warehouse_lot` (`material_id`, `warehouse_id`, `lot_no`),
  KEY `idx_stk_inventory_warehouse_id` (`warehouse_id`),
  CONSTRAINT `fk_stk_inventory_material_id` FOREIGN KEY (`material_id`) REFERENCES `bas_material` (`id`),
  CONSTRAINT `fk_stk_inventory_warehouse_id` FOREIGN KEY (`warehouse_id`) REFERENCES `bas_warehouse` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='库存余额表';

CREATE TABLE IF NOT EXISTS `stk_doc` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `code` VARCHAR(50) NOT NULL COMMENT '库存单号',
  `doc_type` VARCHAR(30) NOT NULL COMMENT '业务类型',
  `source_type` VARCHAR(30) DEFAULT NULL COMMENT '来源类型',
  `source_id` BIGINT DEFAULT NULL COMMENT '来源主单ID',
  `warehouse_id` BIGINT NOT NULL COMMENT '主仓库',
  `target_warehouse_id` BIGINT DEFAULT NULL COMMENT '目标仓库',
  `biz_date` DATE NOT NULL COMMENT '业务日期',
  `status` VARCHAR(20) NOT NULL DEFAULT 'draft' COMMENT '状态',
  `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_stk_doc_code` (`code`),
  KEY `idx_stk_doc_doc_type` (`doc_type`),
  KEY `idx_stk_doc_source` (`source_type`, `source_id`),
  KEY `idx_stk_doc_warehouse_id` (`warehouse_id`),
  KEY `idx_stk_doc_target_warehouse_id` (`target_warehouse_id`),
  KEY `idx_stk_doc_biz_date` (`biz_date`),
  KEY `idx_stk_doc_status` (`status`),
  CONSTRAINT `fk_stk_doc_warehouse_id` FOREIGN KEY (`warehouse_id`) REFERENCES `bas_warehouse` (`id`),
  CONSTRAINT `fk_stk_doc_target_warehouse_id` FOREIGN KEY (`target_warehouse_id`) REFERENCES `bas_warehouse` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='库存单据表';

CREATE TABLE IF NOT EXISTS `stk_doc_item` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `doc_id` BIGINT NOT NULL COMMENT '库存单ID',
  `material_id` BIGINT NOT NULL COMMENT '物料ID',
  `lot_no` VARCHAR(50) DEFAULT NULL COMMENT '批次号',
  `qty` DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '数量',
  `unit_price` DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '单价',
  `amount` DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '金额',
  `source_item_id` BIGINT DEFAULT NULL COMMENT '来源明细ID',
  PRIMARY KEY (`id`),
  KEY `idx_stk_doc_item_doc_id` (`doc_id`),
  KEY `idx_stk_doc_item_material_id` (`material_id`),
  CONSTRAINT `fk_stk_doc_item_doc_id` FOREIGN KEY (`doc_id`) REFERENCES `stk_doc` (`id`),
  CONSTRAINT `fk_stk_doc_item_material_id` FOREIGN KEY (`material_id`) REFERENCES `bas_material` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='库存单据明细表';

CREATE TABLE IF NOT EXISTS `prd_plan` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `code` VARCHAR(50) NOT NULL COMMENT '计划单号',
  `material_id` BIGINT NOT NULL COMMENT '产品ID',
  `plan_qty` DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '计划数量',
  `start_date` DATE NOT NULL COMMENT '计划开工日期',
  `end_date` DATE NOT NULL COMMENT '计划完工日期',
  `source_sales_id` BIGINT DEFAULT NULL COMMENT '来源销售单',
  `status` VARCHAR(20) NOT NULL DEFAULT 'draft' COMMENT '状态',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_prd_plan_code` (`code`),
  KEY `idx_prd_plan_material_id` (`material_id`),
  KEY `idx_prd_plan_source_sales_id` (`source_sales_id`),
  KEY `idx_prd_plan_status` (`status`),
  CONSTRAINT `fk_prd_plan_material_id` FOREIGN KEY (`material_id`) REFERENCES `bas_material` (`id`),
  CONSTRAINT `fk_prd_plan_source_sales_id` FOREIGN KEY (`source_sales_id`) REFERENCES `sal_doc` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='生产计划表';

CREATE TABLE IF NOT EXISTS `prd_work_order` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `code` VARCHAR(50) NOT NULL COMMENT '工单编号',
  `plan_id` BIGINT NOT NULL COMMENT '来源计划ID',
  `material_id` BIGINT NOT NULL COMMENT '产品ID',
  `bom_id` BIGINT NOT NULL COMMENT 'BOM ID',
  `route_id` BIGINT NOT NULL COMMENT '工艺路线ID',
  `plan_qty` DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '计划数量',
  `finished_qty` DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '完工数量',
  `status` VARCHAR(20) NOT NULL DEFAULT 'draft' COMMENT '状态',
  `start_date` DATE DEFAULT NULL COMMENT '开工日期',
  `end_date` DATE DEFAULT NULL COMMENT '完工日期',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_prd_work_order_code` (`code`),
  KEY `idx_prd_work_order_plan_id_material_id_status` (`plan_id`, `material_id`, `status`),
  KEY `idx_prd_work_order_bom_id` (`bom_id`),
  KEY `idx_prd_work_order_route_id` (`route_id`),
  CONSTRAINT `fk_prd_work_order_plan_id` FOREIGN KEY (`plan_id`) REFERENCES `prd_plan` (`id`),
  CONSTRAINT `fk_prd_work_order_material_id` FOREIGN KEY (`material_id`) REFERENCES `bas_material` (`id`),
  CONSTRAINT `fk_prd_work_order_bom_id` FOREIGN KEY (`bom_id`) REFERENCES `bas_bom` (`id`),
  CONSTRAINT `fk_prd_work_order_route_id` FOREIGN KEY (`route_id`) REFERENCES `bas_process_route` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='生产工单表';

CREATE TABLE IF NOT EXISTS `prd_report` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `work_order_id` BIGINT NOT NULL COMMENT '工单ID',
  `process_item_id` BIGINT NOT NULL COMMENT '工艺路线明细ID',
  `report_date` DATETIME NOT NULL COMMENT '报工时间',
  `report_qty` DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '报工数量',
  `qualified_qty` DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '合格数量',
  `defective_qty` DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '不良数量',
  `reporter_name` VARCHAR(50) DEFAULT NULL COMMENT '报工人',
  `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_prd_report_work_order_id` (`work_order_id`),
  KEY `idx_prd_report_process_item_id` (`process_item_id`),
  KEY `idx_prd_report_report_date` (`report_date`),
  CONSTRAINT `fk_prd_report_work_order_id` FOREIGN KEY (`work_order_id`) REFERENCES `prd_work_order` (`id`),
  CONSTRAINT `fk_prd_report_process_item_id` FOREIGN KEY (`process_item_id`) REFERENCES `bas_process_route_item` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='生产报工表';

SET FOREIGN_KEY_CHECKS = 1;
