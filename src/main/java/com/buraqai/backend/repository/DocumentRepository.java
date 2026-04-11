package com.buraqai.backend.repository;

import com.buraqai.backend.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    // JpaRepository already gives us:
    // - save(Document entity)
    // - findById(Long id)
    // - findAll()
    // - delete(Document entity)
    // ...and many more.

    // We can add custom query methods here later if needed.
    // Example: Find all documents uploaded by a specific user:
    // List<Document> findByUploadedBy(String email);
}