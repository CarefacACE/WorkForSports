CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(64) NOT NULL UNIQUE,
    password VARCHAR(128) NOT NULL,
    real_name VARCHAR(64),
    role VARCHAR(32) NOT NULL,
    phone VARCHAR(32),
    email VARCHAR(128),
    gender VARCHAR(16),
    birthday VARCHAR(32),
    avatar VARCHAR(255),
    remark VARCHAR(500),
    create_time DATETIME,
    update_time DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE sys_user CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS phone VARCHAR(32);
ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS email VARCHAR(128);
ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS gender VARCHAR(16);
ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS birthday VARCHAR(32);
ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS avatar VARCHAR(255);
ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS remark VARCHAR(500);
ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS update_time DATETIME;

CREATE TABLE IF NOT EXISTS sys_file (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    original_name VARCHAR(255) NOT NULL,
    stored_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_size BIGINT NOT NULL,
    file_type VARCHAR(128),
    upload_user_id BIGINT,
    upload_username VARCHAR(64),
    create_time DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS deleted TINYINT DEFAULT 0;
ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS balance DECIMAL(10,2) DEFAULT 0.00;

ALTER TABLE course ADD COLUMN IF NOT EXISTS category VARCHAR(32) DEFAULT 'OTHER';
ALTER TABLE course ADD COLUMN IF NOT EXISTS difficulty VARCHAR(16) DEFAULT 'BEGINNER';
ALTER TABLE course ADD COLUMN IF NOT EXISTS max_students INT DEFAULT 0;
ALTER TABLE course ADD COLUMN IF NOT EXISTS location VARCHAR(255);
ALTER TABLE course ADD COLUMN IF NOT EXISTS start_date DATE;
ALTER TABLE course ADD COLUMN IF NOT EXISTS tags VARCHAR(500);
ALTER TABLE course ADD COLUMN IF NOT EXISTS total_lessons INT DEFAULT 0;
ALTER TABLE course ADD COLUMN IF NOT EXISTS frequency VARCHAR(32);
ALTER TABLE course ADD COLUMN IF NOT EXISTS schedule_mode VARCHAR(16) DEFAULT 'MANUAL';
ALTER TABLE course ADD COLUMN IF NOT EXISTS default_time_slot VARCHAR(16);

ALTER TABLE lesson ADD COLUMN IF NOT EXISTS description VARCHAR(500);
ALTER TABLE lesson ADD COLUMN IF NOT EXISTS duration INT DEFAULT 0;

CREATE TABLE IF NOT EXISTS course_schedule (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    course_id BIGINT NOT NULL,
    coach_id BIGINT NOT NULL,
    title VARCHAR(128) NOT NULL,
    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,
    location VARCHAR(255),
    color VARCHAR(32),
    create_time DATETIME,
    update_time DATETIME,
    deleted TINYINT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS wallet_transaction (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    type VARCHAR(32) NOT NULL,
    remark VARCHAR(255),
    create_time DATETIME,
    update_time DATETIME,
    deleted TINYINT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS course (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    coach_id BIGINT NOT NULL,
    name VARCHAR(128) NOT NULL,
    description TEXT,
    type VARCHAR(32) NOT NULL,
    price DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    cover_image VARCHAR(500),
    status VARCHAR(32) DEFAULT 'ACTIVE',
    create_time DATETIME,
    update_time DATETIME,
    deleted TINYINT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS lesson (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    course_id BIGINT NOT NULL,
    title VARCHAR(128) NOT NULL,
    video_url VARCHAR(500),
    sort_order INT DEFAULT 0,
    is_trial TINYINT DEFAULT 0,
    create_time DATETIME,
    update_time DATETIME,
    deleted TINYINT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS enrollment (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'TRIAL',
    paid_amount DECIMAL(10,2) DEFAULT 0.00,
    create_time DATETIME,
    update_time DATETIME,
    deleted TINYINT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS sys_log (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    username VARCHAR(64),
    role VARCHAR(32),
    operation VARCHAR(128),
    method VARCHAR(255),
    params TEXT,
    ip VARCHAR(64),
    create_time DATETIME,
    deleted TINYINT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS chat_conversation (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    type VARCHAR(16) NOT NULL,
    name VARCHAR(128),
    course_id BIGINT,
    owner_id BIGINT,
    create_time DATETIME,
    update_time DATETIME,
    deleted TINYINT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS chat_conversation_member (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    conversation_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    joined_at DATETIME,
    deleted TINYINT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS chat_message (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    conversation_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    msg_type VARCHAR(16) DEFAULT 'TEXT',
    create_time DATETIME,
    deleted TINYINT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS chat_read_status (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    conversation_id BIGINT NOT NULL,
    last_read_message_id BIGINT DEFAULT 0,
    update_time DATETIME,
    UNIQUE KEY uk_user_conv (user_id, conversation_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS notification (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(128),
    content TEXT,
    type VARCHAR(32),
    related_id BIGINT,
    is_read TINYINT DEFAULT 0,
    create_time DATETIME,
    deleted TINYINT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS chat_group_notice (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    conversation_id BIGINT NOT NULL,
    publisher_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    create_time DATETIME,
    update_time DATETIME,
    deleted TINYINT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS chat_friend_request (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    from_user_id BIGINT NOT NULL,
    to_user_id BIGINT NOT NULL,
    request_type VARCHAR(16) NOT NULL,
    conversation_id BIGINT,
    status VARCHAR(16) DEFAULT 'PENDING',
    message VARCHAR(255),
    create_time DATETIME,
    update_time DATETIME,
    deleted TINYINT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS member_health_profile (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    height DECIMAL(5,1),
    weight DECIMAL(5,1),
    body_fat DECIMAL(4,1),
    muscle_mass DECIMAL(5,1),
    bp_systolic INT,
    bp_diastolic INT,
    resting_heart_rate INT,
    blood_type VARCHAR(16),
    allergies TEXT,
    medical_history TEXT,
    current_medications TEXT,
    emergency_contact_name VARCHAR(64),
    emergency_contact_phone VARCHAR(32),
    target_weight DECIMAL(5,1),
    target_body_fat DECIMAL(4,1),
    target_muscle_mass DECIMAL(5,1),
    fitness_goal VARCHAR(32),
    weekly_workout_freq INT,
    target_date DATE,
    goal_notes TEXT,
    create_time DATETIME,
    update_time DATETIME,
    deleted TINYINT DEFAULT 0,
    UNIQUE KEY uk_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS exercise_record (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    type VARCHAR(32) NOT NULL,
    duration INT NOT NULL DEFAULT 0,
    distance DECIMAL(8,2) DEFAULT 0,
    calories INT DEFAULT 0,
    heart_rate_avg INT,
    heart_rate_max INT,
    pace VARCHAR(16),
    exercise_date DATE NOT NULL,
    notes VARCHAR(500),
    create_time DATETIME,
    update_time DATETIME,
    deleted TINYINT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS check_in_record (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    schedule_id BIGINT NOT NULL COMMENT '排课ID',
    user_id BIGINT NOT NULL COMMENT '签到用户ID',
    role VARCHAR(16) NOT NULL COMMENT '角色: COACH/MEMBER',
    check_in_time DATETIME COMMENT '签到时间',
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING/SIGNED/ABSENT',
    create_time DATETIME,
    update_time DATETIME,
    deleted TINYINT DEFAULT 0,
    INDEX idx_schedule_user (schedule_id, user_id),
    UNIQUE KEY uk_schedule_user_role (schedule_id, user_id, role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE course_schedule ADD COLUMN IF NOT EXISTS member_id BIGINT DEFAULT NULL COMMENT '私教学员ID';
ALTER TABLE course_schedule ADD COLUMN IF NOT EXISTS enrollment_id BIGINT DEFAULT NULL COMMENT '关联报名ID';
ALTER TABLE course_schedule ADD COLUMN IF NOT EXISTS booking_status VARCHAR(16) DEFAULT NULL COMMENT '预约状态';

ALTER TABLE enrollment ADD COLUMN IF NOT EXISTS total_sessions INT DEFAULT 0 COMMENT '总课时';
ALTER TABLE enrollment ADD COLUMN IF NOT EXISTS remaining_sessions INT DEFAULT 0 COMMENT '剩余课时';

CREATE TABLE IF NOT EXISTS agent_chat_message (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    memory_id VARCHAR(64) NOT NULL COMMENT '会话ID(用户ID_角色)',
    role VARCHAR(16) NOT NULL COMMENT 'SYSTEM/USER/ASSISTANT/AI',
    content TEXT NOT NULL COMMENT '消息内容',
    create_time DATETIME,
    INDEX idx_memory_id (memory_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS private_coach_profile (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    coach_id BIGINT NOT NULL,
    description TEXT,
    specialties VARCHAR(500),
    price_per_session DECIMAL(10,2) DEFAULT 0.00,
    session_duration INT DEFAULT 60,
    cover_image VARCHAR(500),
    status VARCHAR(16) DEFAULT 'ACTIVE',
    create_time DATETIME,
    update_time DATETIME,
    deleted TINYINT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 健身卡配置表
-- card_category: SESSION(次卡), TIME(时间卡)
-- type: SESSION->VISIT, TIME->MONTHLY/QUARTERLY/YEARLY/TRIAL
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
-- 健身房超市商品表
-- ============================================================
CREATE TABLE IF NOT EXISTS `gym_product` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(100) NOT NULL COMMENT '商品名称',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '商品描述',
  `price` DECIMAL(10,2) NOT NULL COMMENT '价格',
  `image` VARCHAR(500) DEFAULT NULL COMMENT '商品图片URL',
  `stock` INT NOT NULL DEFAULT 0 COMMENT '库存数量，0表示缺货',
  `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE(上架)/INACTIVE(下架)',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='健身房超市商品';

ALTER TABLE `gym_product` ADD COLUMN IF NOT EXISTS `cost` DECIMAL(10,2) DEFAULT 0.00 COMMENT '成本价(进货价)';

-- ============================================================
-- 商品购买记录表
-- ============================================================
CREATE TABLE IF NOT EXISTS `product_purchase_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL COMMENT '购买用户ID',
  `product_id` BIGINT NOT NULL COMMENT '商品ID',
  `product_name` VARCHAR(100) NOT NULL COMMENT '商品名称(快照)',
  `quantity` INT NOT NULL DEFAULT 1 COMMENT '购买数量',
  `unit_price` DECIMAL(10,2) NOT NULL COMMENT '单价(快照)',
  `total_price` DECIMAL(10,2) NOT NULL COMMENT '总价',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_product_id` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品购买记录';

-- ============================================================
-- 缺货通知表
-- ============================================================
CREATE TABLE IF NOT EXISTS `stock_notification` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL COMMENT '提交通知的用户ID',
  `product_id` BIGINT NOT NULL COMMENT '商品ID',
  `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING(待处理)/NOTIFIED(已通知)', 
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_product_id` (`product_id`),
  UNIQUE KEY `uk_user_product` (`user_id`, `product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='缺货通知';