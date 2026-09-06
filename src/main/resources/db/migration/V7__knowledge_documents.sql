-- V7: governed, versioned knowledge documents.
-- Content is immutable after upload; publishing only switches status pointers.
SET @schema_name = DATABASE();

CREATE TABLE IF NOT EXISTS knowledge_document (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    document_key VARCHAR(255) NOT NULL,
    document_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(64) NOT NULL,
    content_sha256 CHAR(64) NOT NULL,
    content_text MEDIUMTEXT NOT NULL,
    version_no INT NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
    created_by BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at DATETIME NULL,
    UNIQUE KEY uk_knowledge_document_hash (content_sha256),
    UNIQUE KEY uk_knowledge_document_key_version (document_key, version_no),
    KEY idx_knowledge_document_key_status (document_key, status),
    KEY idx_knowledge_document_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
