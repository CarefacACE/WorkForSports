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
