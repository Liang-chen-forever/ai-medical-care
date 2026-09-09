-- Add role-bearing login principals without changing legacy doctor records into accounts.
SET @schema_name = DATABASE();

SELECT COUNT(*) INTO @has_user_role
FROM information_schema.columns
WHERE table_schema = @schema_name AND table_name = 'user' AND column_name = 'role';
SET @sql = IF(@has_user_role = 0,
    'ALTER TABLE `user` ADD COLUMN role VARCHAR(16) NOT NULL DEFAULT ''PATIENT'' AFTER phone',
    'SELECT 1');
PREPARE statement FROM @sql;
EXECUTE statement;
DEALLOCATE PREPARE statement;

UPDATE `user` SET role = 'PATIENT' WHERE role IS NULL;

SELECT COUNT(*) INTO @has_doctor_user_id
FROM information_schema.columns
WHERE table_schema = @schema_name AND table_name = 'doctor' AND column_name = 'user_id';
SET @sql = IF(@has_doctor_user_id = 0,
    'ALTER TABLE doctor ADD COLUMN user_id BIGINT NULL AFTER id',
    'SELECT 1');
PREPARE statement FROM @sql;
EXECUTE statement;
DEALLOCATE PREPARE statement;

SELECT COUNT(*) INTO @has_doctor_user_id_index
FROM information_schema.statistics
WHERE table_schema = @schema_name AND table_name = 'doctor' AND index_name = 'uk_doctor_user_id';
SET @sql = IF(@has_doctor_user_id_index = 0,
    'ALTER TABLE doctor ADD UNIQUE KEY uk_doctor_user_id (user_id)',
    'SELECT 1');
PREPARE statement FROM @sql;
EXECUTE statement;
DEALLOCATE PREPARE statement;
