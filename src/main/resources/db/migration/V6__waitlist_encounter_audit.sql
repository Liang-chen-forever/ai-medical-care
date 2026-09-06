-- V6: add waitlist, encounter summaries, audit logs and optional triage linkage.
-- Every statement is safe to run repeatedly against the selected database.

SET @schema_name = DATABASE();

SELECT COUNT(*) INTO @has_triage_case_id
FROM information_schema.columns
WHERE table_schema = @schema_name AND table_name = 'appointment' AND column_name = 'triage_case_id';
SET @sql = IF(@has_triage_case_id = 0,
    'ALTER TABLE appointment ADD COLUMN triage_case_id BIGINT NULL AFTER schedule_id',
    'SELECT 1');
PREPARE statement FROM @sql;
EXECUTE statement;
DEALLOCATE PREPARE statement;

SELECT COUNT(*) INTO @has_triage_case_index
FROM information_schema.statistics
WHERE table_schema = @schema_name AND table_name = 'appointment' AND index_name = 'idx_appointment_triage_case';
SET @sql = IF(@has_triage_case_index = 0,
    'ALTER TABLE appointment ADD KEY idx_appointment_triage_case (triage_case_id)',
    'SELECT 1');
PREPARE statement FROM @sql;
EXECUTE statement;
DEALLOCATE PREPARE statement;

CREATE TABLE IF NOT EXISTS waitlist_entry (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    schedule_id BIGINT NOT NULL,
    patient_id BIGINT NOT NULL,
    triage_case_id BIGINT NULL,
    appointment_id BIGINT NULL,
    priority INT NOT NULL DEFAULT 20,
    status VARCHAR(16) NOT NULL DEFAULT 'WAITING',
    offer_expires_at DATETIME NULL,
    offered_at DATETIME NULL,
    accepted_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_waitlist_schedule_status_priority (schedule_id, status, priority, created_at),
    KEY idx_waitlist_patient_created (patient_id, created_at),
    KEY idx_waitlist_triage_case (triage_case_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS encounter (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    appointment_id BIGINT NOT NULL,
    doctor_id BIGINT NOT NULL,
    patient_id BIGINT NOT NULL,
    summary VARCHAR(2000) NOT NULL,
    follow_up_advice VARCHAR(2000) NULL,
    completed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_encounter_appointment (appointment_id),
    KEY idx_encounter_patient_completed (patient_id, completed_at),
    KEY idx_encounter_doctor_completed (doctor_id, completed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    actor_id BIGINT NULL,
    actor_role VARCHAR(16) NULL,
    action VARCHAR(64) NOT NULL,
    target_type VARCHAR(64) NULL,
    target_id VARCHAR(64) NULL,
    trace_id VARCHAR(64) NULL,
    result VARCHAR(16) NOT NULL,
    detail VARCHAR(500) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_audit_trace_created (trace_id, created_at),
    KEY idx_audit_actor_created (actor_id, created_at),
    KEY idx_audit_action_created (action, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
