CREATE TABLE IF NOT EXISTS risk_assessments (
    assessment_id VARCHAR(36) PRIMARY KEY,
    application_id VARCHAR(36) NOT NULL UNIQUE,
    risk_score INT,
    risk_category VARCHAR(50),
    premium_multiplier DOUBLE,
    remarks VARCHAR(1000),
    status VARCHAR(50),
    assessed_by VARCHAR(255),
    assessed_at DATETIME
);