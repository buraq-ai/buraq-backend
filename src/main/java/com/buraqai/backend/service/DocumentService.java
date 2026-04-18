package com.buraqai.backend.service;

import com.buraqai.backend.dto.DocumentResponseDTO;
import com.buraqai.backend.exception.FileSizeLimitExceededException;
import com.buraqai.backend.exception.InvalidFileTypeException;
import com.buraqai.backend.model.Document;
import com.buraqai.backend.repository.DocumentRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import com.buraqai.backend.model.AuditLog;
import com.buraqai.backend.repository.AuditLogRepository;
import com.buraqai.backend.exception.DocumentNotFoundException;



@Service
public class DocumentService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentService.class);

    private final DocumentRepository documentRepository;
    private final AIServiceClient aiServiceClient;
    private final AuditLogRepository auditLogRepository;


    @Value("${app.storage.path}")
    private String storagePath;

    @Value("${internal.service.key}")
    private String internalServiceKey;

    @PersistenceContext
    private EntityManager entityManager;

    // Maximum file size: 10MB in bytes
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    // Allowed file types
    private static final String[] ALLOWED_TYPES = {"application/pdf", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"};

    public DocumentService(DocumentRepository documentRepository,
                           AIServiceClient aiServiceClient,
                           AuditLogRepository auditLogRepository) {
        this.documentRepository = documentRepository;
        this.aiServiceClient = aiServiceClient;
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public DocumentResponseDTO uploadDocument(MultipartFile file, String uploadedByEmail) {
        // 1. Validate file type
        validateFileType(file);

        // 2. Validate file size
        validateFileSize(file);

        // 3. Generate unique filename and storage path
        String uniqueFilename = generateUniqueFilename(file.getOriginalFilename());
        String datePath = generateDatePath();
        String fullStoragePath = storagePath + "/" + datePath;

        // 4. Create directories if they don't exist
        createStorageDirectory(fullStoragePath);

        // 5. Save file to disk
        Path filePath = Paths.get(fullStoragePath, uniqueFilename);
        try {
            Files.copy(file.getInputStream(), filePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save file to disk: " + e.getMessage(), e);
        }

        // 6. Save metadata to database
        Document document = new Document();
        document.setFilename(file.getOriginalFilename());
        document.setStoragePath(fullStoragePath + "/" + uniqueFilename);
        document.setFileType(getFileExtension(file.getOriginalFilename()));
        document.setFileSize(file.getSize());
        document.setUploadedBy(uploadedByEmail);
        document.setStatus(Document.DocumentStatus.PENDING);

        Document savedDocument = documentRepository.save(document);

        logger.info("Document saved with ID: {}, status: PENDING", savedDocument.getId());
        // Force immediate write to database so callback can find the document
        entityManager.flush();

        logger.info(">>> Flushed document {} to database. Transaction will commit after method completes.", savedDocument.getId());

        // 7. Trigger AI processing AFTER transaction commits so callback can find the document
        final Document finalSavedDocument = savedDocument;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                Path absolutePath = Paths.get(finalSavedDocument.getStoragePath()).toAbsolutePath();
                boolean aiTriggered = aiServiceClient.triggerDocumentProcessing(
                        finalSavedDocument.getId(),
                        absolutePath.toString()
                );
                if (aiTriggered) {
                    logger.info("AI processing triggered successfully for document ID: {}", finalSavedDocument.getId());
                } else {
                    logger.warn("AI processing could not be triggered for document ID: {}. Document remains in PENDING state.", finalSavedDocument.getId());
                }
            }
        });

        // 8. Return DTO
        logger.info(">>> Transaction committing now for document ID: {}", savedDocument.getId());
        return DocumentResponseDTO.fromEntity(savedDocument);
    }

    private void validateFileType(MultipartFile file) {
        String contentType = file.getContentType();
        boolean isValid = false;

        for (String allowedType : ALLOWED_TYPES) {
            if (allowedType.equals(contentType)) {
                isValid = true;
                break;
            }
        }

        if (!isValid) {
            throw new InvalidFileTypeException("Only PDF and DOCX files are allowed");
        }
    }

    private void validateFileSize(MultipartFile file) {
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new FileSizeLimitExceededException("File size cannot exceed 10MB");
        }
    }

    private String generateUniqueFilename(String originalFilename) {
        String uuid = UUID.randomUUID().toString();
        return uuid + "_" + originalFilename;
    }

    private String generateDatePath() {
        LocalDateTime now = LocalDateTime.now();
        String year = String.valueOf(now.getYear());
        String month = now.format(DateTimeFormatter.ofPattern("MM"));
        return year + "/" + month;
    }

    private void createStorageDirectory(String path) {
        try {
            Path directoryPath = Paths.get(path);
            if (!Files.exists(directoryPath)) {
                Files.createDirectories(directoryPath);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to create storage directory: " + e.getMessage(), e);
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "UNKNOWN";
        }
        String extension = filename.substring(filename.lastIndexOf(".") + 1).toUpperCase();
        // Handle DOCX special case
        if ("DOCX".equals(extension) || "application/vnd.openxmlformats-officedocument.wordprocessingml.document".equals(extension)) {
            return "DOCX";
        }
        return extension;
    }

    /**
     * Retrieves all documents from the database and returns them as DTOs.
     * The storagePath field is excluded from the response for security.
     *
     * @return List of DocumentResponseDTO objects
     */
    @Transactional(readOnly = true)
    public List<DocumentResponseDTO> getAllDocuments() {
        logger.info("Fetching all documents");
        List<Document> documents = documentRepository.findAll();

        List<DocumentResponseDTO> response = documents.stream()
                .map(DocumentResponseDTO::fromEntity)
                .collect(Collectors.toList());

        logger.info("Retrieved {} documents", response.size());
        return response;
    }

    @Transactional(readOnly = true)
    public DocumentResponseDTO getDocumentById(Long id) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new DocumentNotFoundException(id));
        return DocumentResponseDTO.fromEntity(document);
    }

    @Transactional
    public void deleteDocument(Long id, String adminEmail) {
        // 1. Find the document — throws 404 if not found
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new DocumentNotFoundException(id));

        String filename = document.getFilename();
        String filePath = document.getStoragePath();

        // 2. Delete physical file from disk
        try {
            Path fileToDelete = Paths.get(filePath);
            boolean deleted = Files.deleteIfExists(fileToDelete);
            if (deleted) {
                logger.info("Physical file deleted from disk: {}", filePath);
            } else {
                logger.warn("Physical file not found on disk (already missing?): {}", filePath);
            }
        } catch (IOException e) {
            // Log warning but DO NOT stop the deletion process
            logger.warn("Could not delete physical file at path: {}. Reason: {}", filePath, e.getMessage());
        }

        // 3. Call FastAPI to remove chunks from ChromaDB (best-effort, never blocks deletion)
        aiServiceClient.deleteDocumentChunks(id);

        // 4. Create audit log entry
        AuditLog auditLog = new AuditLog();
        auditLog.setEntityType("DOCUMENT");
        auditLog.setEntityId(id);
        auditLog.setAction("DELETE");
        auditLog.setPerformedBy(adminEmail);
        auditLog.setDetails("Document deleted: " + filename);
        auditLogRepository.save(auditLog);

        // 5. Delete metadata from PostgreSQL
        documentRepository.delete(document);

        logger.info("Document ID: {} ('{}') deleted by {}", id, filename, adminEmail);
    }


    @Transactional
    public DocumentResponseDTO replaceDocument(Long id, MultipartFile newFile, String adminEmail) {
        // 1. Find the document — throws 404 if not found
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new DocumentNotFoundException(id));

        // 2. Validate the new file (same rules as upload)
        validateFileType(newFile);
        validateFileSize(newFile);

        String oldFilePath = document.getStoragePath();
        String oldFilename = document.getFilename();

        // 3. Delete old physical file from disk
        try {
            Path oldFile = Paths.get(oldFilePath);
            boolean deleted = Files.deleteIfExists(oldFile);
            if (deleted) {
                logger.info("Old physical file deleted from disk: {}", oldFilePath);
            } else {
                logger.warn("Old physical file not found on disk (already missing?): {}", oldFilePath);
            }
        } catch (IOException e) {
            logger.warn("Could not delete old physical file at path: {}. Reason: {}", oldFilePath, e.getMessage());
        }

        // 4. Save new file to disk (same directory structure as upload)
        String uniqueFilename = generateUniqueFilename(newFile.getOriginalFilename());
        String datePath = generateDatePath();
        String fullStoragePath = storagePath + "/" + datePath;
        createStorageDirectory(fullStoragePath);

        Path newFilePath = Paths.get(fullStoragePath, uniqueFilename);
        try {
            Files.copy(newFile.getInputStream(), newFilePath);
            logger.info("New file saved to disk: {}", newFilePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save new file to disk: " + e.getMessage(), e);
        }

        // 5. Call FastAPI to remove old chunks from ChromaDB
        aiServiceClient.deleteDocumentChunks(id);

        // 6. Update document metadata in PostgreSQL
        document.setFilename(newFile.getOriginalFilename());
        document.setStoragePath(fullStoragePath + "/" + uniqueFilename);
        document.setFileType(getFileExtension(newFile.getOriginalFilename()));
        document.setFileSize(newFile.getSize());
        document.setStatus(Document.DocumentStatus.PENDING);

        Document updatedDocument = documentRepository.save(document);
        entityManager.flush();

        logger.info("Document ID: {} replaced by {}. Status reset to PENDING.", id, adminEmail);

        // 7. Create audit log entry
        AuditLog auditLog = new AuditLog();
        auditLog.setEntityType("DOCUMENT");
        auditLog.setEntityId(id);
        auditLog.setAction("REPLACE");
        auditLog.setPerformedBy(adminEmail);
        auditLog.setDetails("Document replaced: " + oldFilename + " → " + newFile.getOriginalFilename());
        auditLogRepository.save(auditLog);

        // 8. Trigger AI processing AFTER transaction commits
        final Document finalUpdatedDocument = updatedDocument;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                Path absolutePath = Paths.get(finalUpdatedDocument.getStoragePath()).toAbsolutePath();
                boolean aiTriggered = aiServiceClient.triggerDocumentProcessing(
                        finalUpdatedDocument.getId(),
                        absolutePath.toString()
                );
                if (aiTriggered) {
                    logger.info("AI reprocessing triggered successfully for replaced document ID: {}", finalUpdatedDocument.getId());
                } else {
                    logger.warn("AI reprocessing could not be triggered for document ID: {}. Document remains in PENDING state.", finalUpdatedDocument.getId());
                }
            }
        });

        return DocumentResponseDTO.fromEntity(updatedDocument);
    }
    /**
     * Updates the status of a document.
     * This method is called by the AI service after processing completes.
     *
     * @param documentId The ID of the document to update
     * @param status The new status (INDEXED or FAILED)
     * @param serviceKey The service key from the request header for validation
     * @throws IllegalArgumentException if the service key is invalid
     * @throws RuntimeException if the document is not found
     */
    @Transactional
    public void updateDocumentStatus(Long documentId, String status, String serviceKey) {
        logger.info("Received status update request for document ID: {}, status: {}", documentId, status);

        // Validate service key
        if (internalServiceKey == null || internalServiceKey.isEmpty()) {
            logger.error("Internal service key is not configured on the server");
            throw new SecurityException("Service-to-service authentication is not configured");
        }

        if (serviceKey == null || !internalServiceKey.equals(serviceKey)) {
            logger.warn("Invalid or missing service key for document ID: {}", documentId);
            throw new SecurityException("Invalid service key");
        }

        // Retry up to 3 times with 500ms delay if document not found
        Document document = null;
        for (int i = 0; i < 3; i++) {
            Optional<Document> opt = documentRepository.findById(documentId);
            if (opt.isPresent()) {
                document = opt.get();
                break;
            }
            if (i < 2) {
                logger.debug("Document {} not found, retrying in 500ms (attempt {}/3)", documentId, i + 1);
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.warn("Retry interrupted for document ID: {}", documentId);
                    break;
                }
            }
        }

        if (document == null) {
            logger.error("Document not found with ID: {} after 3 retries", documentId);
            throw new RuntimeException("Document not found with ID: " + documentId);
        }

        // Update status based on the received value
        try {
            Document.DocumentStatus newStatus = Document.DocumentStatus.valueOf(status);
            document.setStatus(newStatus);
            documentRepository.save(document);
            logger.info("Document ID: {} status updated to: {}", documentId, newStatus);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid status value received: {}", status);
            throw new IllegalArgumentException("Invalid status value. Expected INDEXED or FAILED");
        }
    }
}