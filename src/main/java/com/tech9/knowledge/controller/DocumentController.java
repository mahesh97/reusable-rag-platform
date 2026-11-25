
package com.tech9.knowledge.controller;

import com.tech9.knowledge.dto.DocumentTextResponse;
import com.tech9.knowledge.dto.SearchResult;
import com.tech9.knowledge.entity.Document;
import com.tech9.knowledge.service.DocumentChunkingService;
import com.tech9.knowledge.service.DocumentService;
import com.tech9.knowledge.service.EmbeddingService;
import com.tech9.knowledge.service.SemanticSearchService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;
    private final DocumentChunkingService documentChunkingService;
    private final EmbeddingService embeddingService;
    private final SemanticSearchService semanticSearchService;

    public DocumentController(
            DocumentService documentService,
            DocumentChunkingService documentChunkingService,
            EmbeddingService embeddingService,
            SemanticSearchService semanticSearchService) {

        this.documentService = documentService;
        this.documentChunkingService = documentChunkingService;
        this.embeddingService = embeddingService;
        this.semanticSearchService = semanticSearchService;
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadDocument(
            @RequestParam("file") MultipartFile file) {

        try {
            Document document = documentService.uploadDocument(file);

            return ResponseEntity.status(HttpStatus.CREATED).body(
                    Map.of(
                            "message", "Document uploaded successfully",
                            "id", document.getId(),
                            "fileName", document.getFileName(),
                            "fileSize", document.getFileSize(),
                            "uploadedAt", document.getUploadedAt()
                    )
            );

        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", exception.getMessage()));

        } catch (IOException exception) {
            return ResponseEntity.internalServerError().body(
                    Map.of("error", "Unable to store uploaded document."));
        }
    }

    @PostMapping("/{documentId}/chunks")
    public ResponseEntity<?> createDocumentChunks(
            @PathVariable Long documentId) {

        try {
            int chunkCount =
                    documentChunkingService.createChunks(documentId);

            return ResponseEntity.ok(
                    Map.of(
                            "message", "Document chunks created successfully",
                            "documentId", documentId,
                            "chunkCount", chunkCount
                    )
            );

        } catch (IllegalArgumentException exception) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", exception.getMessage()));
        }
    }

    @PostMapping("/{documentId}/embeddings")
    public ResponseEntity<?> generateDocumentEmbeddings(
            @PathVariable Long documentId) {

        try {
            int embeddedChunks =
                    embeddingService.generateEmbeddings(documentId);

            return ResponseEntity.ok(
                    Map.of(
                            "message", "Document embeddings generated successfully",
                            "documentId", documentId,
                            "embeddedChunks", embeddedChunks
                    )
            );

        } catch (IllegalArgumentException exception) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", exception.getMessage()));

        } catch (IllegalStateException exception) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of(
                            "error",
                            "Unable to generate embeddings. Check that Ollama is running and the embedding model is available."
                    ));
        }
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchDocuments(
            @RequestParam String question,
            @RequestParam(defaultValue = "5") int limit) {

        try {
            List<SearchResult> results =
                    semanticSearchService.search(question, limit);

            return ResponseEntity.ok(results);

        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", exception.getMessage()));

        } catch (IllegalStateException exception) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(
                    Map.of("error",
                            "Unable to generate query embedding. Check that Ollama is running and the embedding model is available."));
        }
    }

    @GetMapping
    public ResponseEntity<List<Document>> getAllDocuments() {
        return ResponseEntity.ok(documentService.getAllDocuments());
    }

    @GetMapping("/{documentId}/text")
    public ResponseEntity<DocumentTextResponse> getDocumentText(
            @PathVariable Long documentId) {

        return ResponseEntity.ok(
                documentService.getDocumentText(documentId));
    }
}
