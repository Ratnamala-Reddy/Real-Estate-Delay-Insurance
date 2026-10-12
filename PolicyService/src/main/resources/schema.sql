CREATE TABLE IF NOT EXISTS quotes (
    quote_id VARCHAR(36) PRIMARY KEY,
    application_id VARCHAR(36),
    plan_name VARCHAR(100),
    premium_amount DOUBLE,
    monthly_compensation DOUBLE,
    maximum_delay_months INT,
    maximum_payout DOUBLE,
    grace_period_days INT,
    expiry_date DATETIME,
    status VARCHAR(50),
    created_at DATETIME
);

CREATE TABLE IF NOT EXISTS policies (
    policy_id VARCHAR(36) PRIMARY KEY,
    application_id VARCHAR(36) NOT NULL,
    quote_id VARCHAR(36),
    payment_id VARCHAR(36),
    policy_number VARCHAR(100),
    coverage_amount DOUBLE,
    premium_amount DOUBLE,
    policy_status VARCHAR(50),
    start_date DATE,
    end_date DATE,
    created_at DATETIME
);