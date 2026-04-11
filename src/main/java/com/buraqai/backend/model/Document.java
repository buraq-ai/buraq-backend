package com.buraqai.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "documents")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String filename; // Original file name (e.g., "HR_Policy.pdf")

    @Column(nullable = false)
    private String storagePath; // Path where file is saved on disk

    @Column(nullable = false)
    private String fileType; // "PDF" or "DOCX"

    @Column(nullable = false)
    private Long fileSize; // Size in bytes

    @Column(nullable = false)
    private String uploadedBy; // Email of the admin who uploaded

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime uploadedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentStatus status;

    // The Enum definition lives INSIDE the Document class for clarity
    public enum DocumentStatus {
        PENDING, INDEXED, FAILED
    }
}