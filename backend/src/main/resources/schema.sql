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
ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS deleted TINYINT DEFAULT 0;
ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS balance DECIMAL(10,2) DEFAULT 0.00;

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
