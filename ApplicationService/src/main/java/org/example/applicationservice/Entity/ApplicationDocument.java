package org.example.applicationservice.Entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Table("application_documents")
public class ApplicationDocument {

    @Id
    @Column("document_id")
    private String documentId;

    @Column("application_id")
    private String applicationId;

    @Column("document_type")
    private String documentType;

    @Column("file_name")
    private String fileName;

    @Column("file_path")
    private String filePath;

    @Column("review_status")
    private String reviewStatus;
}