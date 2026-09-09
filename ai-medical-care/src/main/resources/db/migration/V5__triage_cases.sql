CREATE TABLE IF NOT EXISTS triage_case (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    patient_id BIGINT NOT NULL,
    chief_complaint VARCHAR(1000) NOT NULL,
    risk_level VARCHAR(16) NOT NULL,
    status VARCHAR(24) NOT NULL,
    recommended_department VARCHAR(50) NULL,
    retrieval_confidence DECIMAL(5,4) NULL,
    knowledge_version VARCHAR(32) NULL,
    rule_code VARCHAR(64) NULL,
    fallback_reason VARCHAR(32) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_triage_case_patient_created (patient_id, created_at),
    KEY idx_triage_case_status_created (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS triage_evidence (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    triage_case_id BIGINT NOT NULL,
    document_id VARCHAR(128) NOT NULL,
    chunk_id CHAR(64) NOT NULL,
    excerpt VARCHAR(500) NOT NULL,
    score DECIMAL(5,4) NOT NULL,
    rank_no INT NOT NULL,
    knowledge_version VARCHAR(32) NOT NULL,
    UNIQUE KEY uk_triage_evidence_case_rank (triage_case_id, rank_no),
    KEY idx_triage_evidence_case (triage_case_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
