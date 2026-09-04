-- V2: 将历史预约升级为用户归属 + 排班归属模型。
--
-- 本项目尚未接入 Flyway，因此该文件由运维人员在备份后手动执行。
-- 脚本可重复执行：旧预约的 user_id/schedule_id 保持 NULL，不删除历史数据；
-- 新预约由后端写入完整归属字段。

SET NAMES utf8mb4;

SET @has_user_id = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'appointment'
      AND column_name = 'user_id'
);
SET @sql = IF(
    @has_user_id = 0,
    'ALTER TABLE appointment ADD COLUMN user_id BIGINT NULL COMMENT ''预约所属用户ID'' AFTER doctor_name',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_schedule_id = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'appointment'
      AND column_name = 'schedule_id'
);
SET @sql = IF(
    @has_schedule_id = 0,
    'ALTER TABLE appointment ADD COLUMN schedule_id BIGINT NULL COMMENT ''关联排班ID'' AFTER user_id',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_unique_key = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'appointment'
      AND index_name = 'uk_appointment_user_schedule'
);
SET @sql = IF(
    @has_unique_key = 0,
    'ALTER TABLE appointment ADD UNIQUE KEY uk_appointment_user_schedule (user_id, schedule_id)',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_user_index = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'appointment'
      AND index_name = 'idx_appointment_user_id'
);
SET @sql = IF(
    @has_user_index = 0,
    'ALTER TABLE appointment ADD KEY idx_appointment_user_id (user_id)',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_schedule_index = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'appointment'
      AND index_name = 'idx_appointment_schedule_id'
);
SET @sql = IF(
    @has_schedule_index = 0,
    'ALTER TABLE appointment ADD KEY idx_appointment_schedule_id (schedule_id)',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
