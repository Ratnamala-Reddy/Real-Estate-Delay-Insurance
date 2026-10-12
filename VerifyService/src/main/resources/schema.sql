CREATE TABLE IF NOT EXISTS verification_records(
    verification_id VARCHAR(36) PRIMARY KEY,
    application_id VARCHAR(36) NOT NULL UNIQUE,
    verification_status VARCHAR(50),
    remarks VARCHAR(1000),
    verified_by VARCHAR(255),
    verified_at DATETIME
);