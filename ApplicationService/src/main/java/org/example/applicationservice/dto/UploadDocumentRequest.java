package org.example.applicationservice.dto;

import lombok.Data;

@Data
public class UploadDocumentRequest {

    private String documentType;

    private String fileName;

    private String filePath;
}