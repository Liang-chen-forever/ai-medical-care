-- V4: introduce the doctor-owned appointment lifecycle without rewriting legacy records.
-- This project runs migrations manually, so every schema operation is idempotent.

SET @schema_name = DATABASE();

SELECT COUNT(*) INTO @has_doctor_id
FROM information_schema.columns
WHERE table_schema = @schema_name AND table_name = 'appointment' AND column_name = 'doctor_id';
SET @sql = IF(@has_doctor_id = 0,
    'ALTER TABLE appointment ADD COLUMN doctor_id BIGINT NULL AFTER schedule_id',
    'SELECT 1');
PREPARE statement FROM @sql;
EXECUTE statement;
DEALLOCATE PREPARE statement;

SELECT COUNT(*) INTO @has_status
FROM information_schema.columns
WHERE table_schema = @schema_name AND table_name = 'appointment' AND column_name = 'status';
SET @sql = IF(@has_status = 0,
    'ALTER TABLE appointment ADD COLUMN status VARCHAR(16) NULL AFTER doctor_id',
    'SELECT 1');
PREPARE statement FROM @sql;
EXECUTE statement;
DEALLOCATE PREPARE statement;

SELECT COUNT(*) INTO @has_cancel_reason
FROM information_schema.columns
WHERE table_schema = @schema_name AND table_name = 'appointment' AND column_name = 'cancel_reason';
SET @sql = IF(@has_cancel_reason = 0,
    'ALTER TABLE appointment ADD COLUMN cancel_reason VARCHAR(200) NULL AFTER status',
    'SELECT 1');
PREPARE statement FROM @sql;
EXECUTE statement;
DEALLOCATE PREPARE statement;

SELECT COUNT(*) INTO @has_handled_by
FROM information_schema.columns
WHERE table_schema = @schema_name AND table_name = 'appointment' AND column_name = 'handled_by';
SET @sql = IF(@has_handled_by = 0,
    'ALTER TABLE appointment ADD COLUMN handled_by BIGINT NULL AFTER cancel_reason',
    'SELECT 1');
PREPARE statement FROM @sql;
EXECUTE statement;
DEALLOCATE PREPARE statement;

SELECT COUNT(*) INTO @has_handled_at
FROM information_schema.columns
WHERE table_schema = @schema_name AND table_name = 'appointment' AND column_name = 'handled_at';
SET @sql = IF(@has_handled_at = 0,
    'ALTER TABLE appointment ADD COLUMN handled_at DATETIME NULL AFTER handled_by',
    'SELECT 1');
PREPARE statement FROM @sql;
EXECUTE statement;
DEALLOCATE PREPARE statement;

-- Records created before V4 remain read-only to the new state machine.
UPDATE appointment a
LEFT JOIN schedule s ON s.id = a.schedule_id
SET a.doctor_id = s.doctor_id
WHERE a.doctor_id IS NULL AND s.id IS NOT NULL;

UPDATE appointment
SET status = 'LEGACY'
WHERE status IS NULL;

SELECT COUNT(*) INTO @has_doctor_status_index
FROM information_schema.statistics
WHERE table_schema = @schema_name AND table_name = 'appointment'
  AND index_name = 'idx_appointment_doctor_status';
SET @sql = IF(@has_doctor_status_index = 0,
    'ALTER TABLE appointment ADD KEY idx_appointment_doctor_status (doctor_id, status)',
    'SELECT 1');
PREPARE statement FROM @sql;
EXECUTE statement;
DEALLOCATE PREPARE statement;

SELECT COUNT(*) INTO @has_user_status_index
FROM information_schema.statistics
WHERE table_schema = @schema_name AND table_name = 'appointment'
  AND index_name = 'idx_appointment_user_status';
SET @sql = IF(@has_user_status_index = 0,
    'ALTER TABLE appointment ADD KEY idx_appointment_user_status (user_id, status)',
    'SELECT 1');
PREPARE statement FROM @sql;
EXECUTE statement;
DEALLOCATE PREPARE statement;
