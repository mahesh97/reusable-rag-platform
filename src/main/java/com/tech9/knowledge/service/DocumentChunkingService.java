
package com.tech9.knowledge.service;

import com.tech9.knowledge.entity.Document;
import com.tech9.knowledge.entity.DocumentChunk;
import com.tech9.knowledge.repository.DocumentChunkRepository;
import com.tech9.knowledge.repository.DocumentRepository;
import com.tech9.knowledge.repository.DocumentTextRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class DocumentChunkingService {

    private static final int CHUNK_SIZE = 1000;
    private static final int CHUNK_OVERLAP = 200;

    private final DocumentRepository documentRepository;
    private final DocumentTextRepository documentTextRepository;
    private final DocumentChunkRepository documentChunkRepository;

    public DocumentChunkingService(
            DocumentRepository documentRepository,
            DocumentTextRepository documentTextRepository,
            DocumentChunkRepository documentChunkRepository) {

        this.documentRepository = documentRepository;
        this.documentTextRepository = documentTextRepository;
        this.documentChunkRepository = documentChunkRepository;
    }

    @Transactional
    public int createChunks(Long documentId) {

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Document not found"));

        String extractedText = documentTextRepository
                .findByDocumentId(documentId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Extracted text not found"))
                .getExtractedText();

        if (extractedText == null || extractedText.trim().isEmpty()) {
            throw new IllegalArgumentException("Document text is empty");
        }

        // Remove previously generated chunks before regenerating.
        documentChunkRepository.deleteByDocumentId(documentId);

        List<DocumentChunk> chunks = new ArrayList<>();

        int start = 0;
        int chunkIndex = 0;

        while (start < extractedText.length()) {

            int end = Math.min(start + CHUNK_SIZE, extractedText.length());

            String chunkText = extractedText.substring(start, end);

            DocumentChunk chunk = new DocumentChunk();
            chunk.setDocument(document);
            chunk.setChunkIndex(chunkIndex++);
            chunk.setChunkText(chunkText);
            chunk.setCreatedAt(LocalDateTime.now());

            chunks.add(chunk);

            if (end == extractedText.length()) {
                break;
            }

            start = end - CHUNK_OVERLAP;
        }

        documentChunkRepository.saveAll(chunks);

        return chunks.size();
    }
}
