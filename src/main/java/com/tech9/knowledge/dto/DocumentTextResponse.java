package com.tech9.knowledge.dto;

import java.time.LocalDateTime;

public class DocumentTextResponse {

    private Long documentId;
    private String fileName;
    private int extractedCharacters;
    private String extractedText;
    private LocalDateTime extractedAt;

    public DocumentTextResponse(Long documentId, String fileName,
                                String extractedText, LocalDateTime extractedAt) {
        this.documentId = documentId;
        this.fileName = fileName;
        this.extractedText = extractedText;
        this.extractedCharacters = extractedText != null ? extractedText.length() : 0;
        this.extractedAt = extractedAt;
    }

    public Long getDocumentId() {
        return documentId;
    }

    public String getFileName() {
        return fileName;
    }

    public int getExtractedCharacters() {
        return extractedCharacters;
    }

    public String getExtractedText() {
        return extractedText;
    }

    public LocalDateTime getExtractedAt() {
        return extractedAt;
    }
}