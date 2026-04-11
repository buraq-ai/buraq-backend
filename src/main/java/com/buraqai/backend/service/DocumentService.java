package com.buraqai.backend.service;

import com.buraqai.backend.dto.DocumentResponseDTO;
import com.buraqai.backend.exception.FileSizeLimitExceededException;
import com.buraqai.backend.exception.InvalidFileTypeException;
import com.buraqai.backend.model.Document;
import com.buraqai.backend.repository.DocumentRepository;
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
import java.util.UUID;
import java.util.List;
import java.util.stream.Collectors;



@Service
public class DocumentService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentService.class);

    private final DocumentRepository documentRepository;
    private final AIServiceClient aiServiceClient;

    @Value("${app.storage.path}")
    private String storagePath;

    // Maximum file size: 10MB in bytes
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    // Allowed file types
    private static final String[] ALLOWED_TYPES = {"application/pdf", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"};

    public DocumentService(DocumentRepository documentRepository, AIServiceClient aiServiceClient) {
        this.documentRepository = documentRepository;
        this.aiServiceClient = aiServiceClient;
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

        // 7. Trigger AI processing (fire-and-forget - failures are logged but don't break upload)
        boolean aiTriggered = aiServiceClient.triggerDocumentProcessing(
                savedDocument.getId(),
                savedDocument.getStoragePath()
        );

        if (aiTriggered) {
            logger.info("AI processing triggered successfully for document ID: {}", savedDocument.getId());
        } else {
            logger.warn("AI processing could not be triggered for document ID: {}. Document remains in PENDING state.", savedDocument.getId());
        }

        // 8. Return DTO
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
}