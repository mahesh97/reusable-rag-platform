
package com.tech9.knowledge.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tech9.knowledge.entity.DocumentChunk;
import com.tech9.knowledge.repository.DocumentChunkRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EmbeddingService {

    private static final int BATCH_SIZE = 16;

    private final DocumentChunkRepository documentChunkRepository;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Value("${app.ollama.base-url}")
    private String ollamaBaseUrl;

    @Value("${app.ollama.embedding-model}")
    private String embeddingModel;

    @Value("${app.ollama.embedding-dimensions}")
    private int embeddingDimensions;

    public EmbeddingService(
            DocumentChunkRepository documentChunkRepository,
            JdbcTemplate jdbcTemplate,
            ObjectMapper objectMapper) {

        this.documentChunkRepository = documentChunkRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newHttpClient();
    }

    public int generateEmbeddings(Long documentId) {

        List<DocumentChunk> chunks =
                documentChunkRepository
                        .findByDocumentIdOrderByChunkIndexAsc(documentId);

        if (chunks.isEmpty()) {
            throw new IllegalArgumentException(
                    "No chunks found for document ID: " + documentId);
        }

        int embeddedCount = 0;

        for (int start = 0; start < chunks.size(); start += BATCH_SIZE) {

            int end = Math.min(start + BATCH_SIZE, chunks.size());

            List<DocumentChunk> batch =
                    chunks.subList(start, end);

            List<String> texts = new ArrayList<>();

            for (DocumentChunk chunk : batch) {
                texts.add(chunk.getChunkText());
            }

            List<JsonNode> embeddings = generateBatchEmbeddings(texts);

            if (embeddings.size() != batch.size()) {
                throw new IllegalStateException(
                        "Embedding count does not match chunk count");
            }

            List<Object[]> batchArguments = new ArrayList<>();

            for (int i = 0; i < batch.size(); i++) {

                JsonNode embedding = embeddings.get(i);

                if (embedding.size() != embeddingDimensions) {
                    throw new IllegalStateException(
                            "Unexpected embedding dimensions: "
                                    + embedding.size());
                }

                String vector = convertToVector(embedding);

                batchArguments.add(new Object[]{
                        vector,
                        batch.get(i).getId()
                });
            }

            jdbcTemplate.batchUpdate(
                    "UPDATE document_chunks "
                            + "SET embedding = CAST(? AS vector) "
                            + "WHERE id = ?",
                    batchArguments
            );

            embeddedCount += batch.size();
        }

        return embeddedCount;
    }

    private List<JsonNode> generateBatchEmbeddings(List<String> texts) {

        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", embeddingModel);
            requestBody.put("input", texts);

            String requestJson =
                    objectMapper.writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(ollamaBaseUrl + "/api/embed"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() != 200) {
                throw new IllegalStateException(
                        "Ollama embedding request failed. HTTP status: "
                                + response.statusCode());
            }

            JsonNode responseJson =
                    objectMapper.readTree(response.body());

            JsonNode embeddingsNode =
                    responseJson.get("embeddings");

            if (embeddingsNode == null || !embeddingsNode.isArray()) {
                throw new IllegalStateException(
                        "Invalid embedding response from Ollama");
            }

            List<JsonNode> embeddings = new ArrayList<>();

            for (JsonNode embedding : embeddingsNode) {
                embeddings.add(embedding);
            }

            return embeddings;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "Ollama embedding request was interrupted", e);

        } catch (IOException e) {
            throw new IllegalStateException(
                    "Unable to communicate with Ollama", e);
        }
    }

    public List<Double> generateQueryEmbedding(String text) {

        List<String> texts = new ArrayList<>();
        texts.add(text);

        List<JsonNode> embeddings = generateBatchEmbeddings(texts);

        if (embeddings.isEmpty()) {
            throw new IllegalStateException(
                    "Ollama did not return an embedding for the query");
        }

        JsonNode embedding = embeddings.get(0);

        if (embedding.size() != embeddingDimensions) {
            throw new IllegalStateException(
                    "Unexpected query embedding dimensions: "
                            + embedding.size());
        }

        List<Double> vector = new ArrayList<>();

        for (int i = 0; i < embedding.size(); i++) {
            vector.add(embedding.get(i).asDouble());
        }

        return vector;
    }

    private String convertToVector(JsonNode embedding) {

        StringBuilder vector = new StringBuilder("[");

        for (int i = 0; i < embedding.size(); i++) {

            if (i > 0) {
                vector.append(",");
            }

            vector.append(embedding.get(i).asDouble());
        }

        vector.append("]");

        return vector.toString();
    }
}
