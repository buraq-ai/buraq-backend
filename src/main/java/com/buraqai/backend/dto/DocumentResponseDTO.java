package com.buraqai.backend.dto;

import com.buraqai.backend.model.Document;

import java.time.LocalDateTime;

public class DocumentResponseDTO {

    private Long id;
    private String filename;
    private String fileType;
    private Long fileSize;
    private String uploadedBy;
    private LocalDateTime uploadedAt;
    private Document.DocumentStatus status;

    // Constructor
    public DocumentResponseDTO(Long id, String filename, String fileType, Long fileSize,
                               String uploadedBy, LocalDateTime uploadedAt, Document.DocumentStatus status) {
        this.id = id;
        this.filename = filename;
        this.fileType = fileType;
        this.fileSize = fileSize;
        this.uploadedBy = uploadedBy;
        this.uploadedAt = uploadedAt;
        this.status = status;
    }

    // Getters
    public Long getId() { return id; }
    public String getFilename() { return filename; }
    public String getFileType() { return fileType; }
    public Long getFileSize() { return fileSize; }
    public String getUploadedBy() { return uploadedBy; }
    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public Document.DocumentStatus getStatus() { return status; }

    /**
     * Converts a Document entity to DocumentResponseDTO.
     * storagePath is intentionally excluded for security.
     */
    public static DocumentResponseDTO fromEntity(Document document) {
        return new DocumentResponseDTO(
                document.getId(),
                document.getFilename(),
                document.getFileType(),
                document.getFileSize(),
                document.getUploadedBy(),
                document.getUploadedAt(),
                document.getStatus()
        );
    }
}