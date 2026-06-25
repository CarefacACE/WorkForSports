-- ============================================================
-- 健身卡配置表（次卡 / 时间卡分开管理）
-- card_category: SESSION=次卡(按次数), TIME=时间卡(按月/季/年/体验)
-- type: SESSION→VISIT, TIME→MONTHLY/QUARTERLY/YEARLY/TRIAL
-- ============================================================
CREATE TABLE IF NOT EXISTS `gym_card` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(100) NOT NULL COMMENT '卡名',
  `card_category` VARCHAR(20) NOT NULL DEFAULT 'SESSION' COMMENT 'SESSION(次卡)/TIME(时间卡)',
  `type` VARCHAR(20) NOT NULL COMMENT 'VISIT/MONTHLY/QUARTERLY/YEARLY/TRIAL',
  `price` DECIMAL(10,2) NOT NULL COMMENT '价格',
  `duration` INT NOT NULL COMMENT '有效期天数(次卡=次数,时间卡=天数)',
  `sub_card_limit` INT NOT NULL DEFAULT 2 COMMENT '次卡允许的副卡数量',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '描述',
  `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/INACTIVE',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='健身卡配置';

-- ============================================================
-- 会员健身卡持有表
-- card_holder_type: PRIMARY(主卡)/SUB(副卡)
-- primary_membership_id: 副卡→主卡的自关联ID
-- holder_name: 副卡显示名称（如"张三的副卡"）
-- ============================================================
CREATE TABLE IF NOT EXISTS `gym_membership` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL COMMENT '会员ID',
  `gym_card_id` BIGINT NOT NULL COMMENT '关联健身卡ID',
  `primary_membership_id` BIGINT DEFAULT NULL COMMENT '副卡关联的主卡ID',
  `card_holder_type` VARCHAR(20) NOT NULL DEFAULT 'PRIMARY' COMMENT 'PRIMARY(主卡)/SUB(副卡)',
  `holder_name` VARCHAR(50) DEFAULT NULL COMMENT '副卡持有人名称',
  `start_date` DATE NOT NULL COMMENT '生效日期',
  `end_date` DATE NOT NULL COMMENT '到期日期',
  `remaining_visits` INT DEFAULT NULL COMMENT '剩余次数(次卡)',
  `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/EXPIRED/USED_UP',
  `paid_amount` DECIMAL(10,2) NOT NULL COMMENT '支付金额',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_gym_card_id` (`gym_card_id`),
  KEY `idx_primary_membership_id` (`primary_membership_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员健身卡';

-- ============================================================
-- 数据迁移脚本（首次部署时执行）
-- 将旧数据补齐 card_category
-- ============================================================
-- UPDATE gym_card SET card_category = 'SESSION' WHERE type = 'VISIT';
-- UPDATE gym_card SET card_category = 'TIME' WHERE type IN ('MONTHLY','YEARLY');
