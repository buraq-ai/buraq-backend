package com.buraqai.backend.controller;

import com.buraqai.backend.dto.DocumentResponseDTO;
import com.buraqai.backend.dto.StatusUpdateRequest;
import com.buraqai.backend.service.DocumentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/documents")
@PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_SYSTEM_ADMIN')")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentResponseDTO> uploadDocument(@RequestParam("file") MultipartFile file) {

        // Extract the uploading admin's email from the JWT token
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String uploadedByEmail = authentication.getName();

        DocumentResponseDTO response = documentService.uploadDocument(file, uploadedByEmail);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<DocumentResponseDTO>> getAllDocuments() {
        List<DocumentResponseDTO> documents = documentService.getAllDocuments();
        return ResponseEntity.ok(documents);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocumentResponseDTO> getDocumentById(@PathVariable Long id) {
        DocumentResponseDTO document = documentService.getDocumentById(id);
        return ResponseEntity.ok(document);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDocument(@PathVariable Long id) {

        // Extract the admin's email from the JWT token (same pattern as uploadDocument)
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String adminEmail = authentication.getName();

        documentService.deleteDocument(id, adminEmail);

        return ResponseEntity.noContent().build(); // HTTP 204
    }

    @PostMapping(value = "/{id}/replace", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentResponseDTO> replaceDocument(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {

        // Extract the admin's email from the JWT token (same pattern as uploadDocument)
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String adminEmail = authentication.getName();

        DocumentResponseDTO response = documentService.replaceDocument(id, file, adminEmail);

        return ResponseEntity.ok(response); // HTTP 200 with updated document
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("permitAll()")  // Override class-level security
    public ResponseEntity<Void> updateDocumentStatus(
            @PathVariable Long id,
            @RequestBody StatusUpdateRequest request,
            @RequestHeader(value = "X-Service-Key", required = false) String serviceKey) {

        documentService.updateDocumentStatus(id, request.getStatus(), serviceKey);
        return ResponseEntity.ok().build();
    }
}