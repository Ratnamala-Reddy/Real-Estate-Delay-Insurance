CREATE TABLE IF NOT EXISTS applications(

                             application_id VARCHAR(36) PRIMARY KEY,

                             user_id INT NOT NULL,

                             builder_name VARCHAR(255),

                             project_name VARCHAR(255),

                             unit_number VARCHAR(100),

                             property_value DECIMAL(15,2),

                             purchase_date DATE,

                             promised_possession_date DATE,

                             status VARCHAR(50)
);





CREATE TABLE IF NOT EXISTS application_documents(

                                      document_id VARCHAR(36) PRIMARY KEY,

                                      application_id VARCHAR(36) NOT NULL,

                                      document_type VARCHAR(100),

                                      file_name VARCHAR(255),

                                      file_path VARCHAR(500),

                                      review_status VARCHAR(50)
);