
package com.tech9.knowledge.service;

import com.tech9.knowledge.dto.DocumentTextResponse;
import com.tech9.knowledge.entity.Document;
import com.tech9.knowledge.entity.DocumentText;
import com.tech9.knowledge.repository.DocumentRepository;
import com.tech9.knowledge.repository.DocumentTextRepository;

import org.apache.tika.exception.TikaException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final Path uploadDirectory;
    private final DocumentTextRepository documentTextRepository;
    private final DocumentExtractionService extractionService;

    private static final List<String> ALLOWED_EXTENSIONS =
            Arrays.asList("pdf", "docx", "txt");

    public DocumentService(
            DocumentRepository documentRepository,
            DocumentTextRepository documentTextRepository,
            DocumentExtractionService extractionService,
            @Value("${app.document.upload-dir}") String uploadDir) {

        this.documentRepository = documentRepository;
        this.documentTextRepository = documentTextRepository;
        this.extractionService = extractionService;

        this.uploadDirectory = Paths.get(uploadDir)
                .toAbsolutePath().normalize();
    }

    @Transactional
    public Document uploadDocument(MultipartFile file) throws IOException {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Please select a file.");
        }

        String originalFileName = file.getOriginalFilename();

        if (originalFileName == null || originalFileName.trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid file name.");
        }

        originalFileName = originalFileName.replace('\\', '/');
        originalFileName = originalFileName.substring(
                originalFileName.lastIndexOf('/') + 1);

        int extensionIndex = originalFileName.lastIndexOf('.');
        if (extensionIndex < 0) {
            throw new IllegalArgumentException("File extension is required.");
        }

        String extension = originalFileName
                .substring(extensionIndex + 1)
                .toLowerCase(Locale.ROOT);

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException(
                    "Only PDF, DOCX and TXT files are allowed.");
        }

        try {
            Files.createDirectories(uploadDirectory);

            String storedFileName = UUID.randomUUID() + "." + extension;
            Path targetPath = uploadDirectory.resolve(storedFileName)
                    .normalize();

            if (!targetPath.startsWith(uploadDirectory)) {
                throw new IllegalArgumentException("Invalid file path.");
            }

            Files.copy(file.getInputStream(), targetPath,
                    StandardCopyOption.REPLACE_EXISTING);

            try {
                Document document = new Document();
                document.setFileName(originalFileName);
                document.setStoredFileName(storedFileName);
                document.setFilePath(targetPath.toString());
                document.setContentType(file.getContentType());
                document.setFileSize(file.getSize());
                document.setUploadedAt(LocalDateTime.now());

                Document savedDocument = documentRepository.save(document);

                String extractedText = extractionService.extractText(
                        savedDocument.getFilePath());

                DocumentText documentText = new DocumentText();
                documentText.setDocument(savedDocument);
                documentText.setExtractedText(extractedText);
                documentText.setExtractedAt(LocalDateTime.now());

                documentTextRepository.save(documentText);

                return savedDocument;

            } catch (IOException | TikaException | RuntimeException exception) {
                Files.deleteIfExists(targetPath);
                throw new IOException(
                        "Document upload or text extraction failed.",
                        exception);
            }

        } catch (IOException exception) {
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public DocumentTextResponse getDocumentText(Long documentId) {

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Document not found"));

        DocumentText documentText = documentTextRepository
                .findByDocumentId(documentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Extracted text not found"));

        return new DocumentTextResponse(
                document.getId(),
                document.getFileName(),
                documentText.getExtractedText(),
                documentText.getExtractedAt()
        );
    }

    public List<Document> getAllDocuments() {
        return documentRepository.findAll();
    }
}
