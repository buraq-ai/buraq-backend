package com.buraqai.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SourceChunkDTO {

    @JsonProperty("document_id")
    private Long documentId;

    private String filename;

    @JsonProperty("chunk_index")
    private Integer chunkIndex;

    @JsonProperty("page_number")
    private Integer pageNumber;

    private String excerpt;

    // Default constructor (required for JSON deserialization)
    public SourceChunkDTO() {}

    // Getters and Setters
    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long documentId) { this.documentId = documentId; }

    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }

    public Integer getChunkIndex() { return chunkIndex; }
    public void setChunkIndex(Integer chunkIndex) { this.chunkIndex = chunkIndex; }

    public Integer getPageNumber() { return pageNumber; }
    public void setPageNumber(Integer pageNumber) { this.pageNumber = pageNumber; }

    public String getExcerpt() { return excerpt; }
    public void setExcerpt(String excerpt) { this.excerpt = excerpt; }
}