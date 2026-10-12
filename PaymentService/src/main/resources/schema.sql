CREATE TABLE IF NOT EXISTS premium_payments(

                                 payment_id VARCHAR(36) PRIMARY KEY,

                                 quote_id VARCHAR(36),

                                 application_id VARCHAR(36),

                                 premium_amount DOUBLE,

                                 payment_provider VARCHAR(100),

                                 transaction_reference VARCHAR(255),

                                 status VARCHAR(50),

                                 created_at DATETIME
);


CREATE TABLE IF NOT EXISTS compensation_payments(

                                      payment_id VARCHAR(36) PRIMARY KEY,

                                      claim_id VARCHAR(36),

                                      policy_id VARCHAR(36),

                                      payout_amount DOUBLE,

                                      transaction_reference VARCHAR(255),

                                      status VARCHAR(50),

                                      created_at DATETIME
);