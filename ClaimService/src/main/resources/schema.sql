CREATE TABLE IF NOT EXISTS claims (
                                      claim_id VARCHAR(255) PRIMARY KEY,

    policy_id VARCHAR(255) NOT NULL,

    application_id VARCHAR(255) NOT NULL,

    reason VARCHAR(1000) NOT NULL,

    claimed_amount DOUBLE NOT NULL,

    approved_amount DOUBLE NULL,

    eligible_delay_months INT NULL,

    status VARCHAR(50) NOT NULL,

    remarks VARCHAR(1000) NULL,

    created_at TIMESTAMP NOT NULL,

    actual_possession_date DATE NULL,

    CONSTRAINT uk_claim_policy
    UNIQUE (policy_id)
    );