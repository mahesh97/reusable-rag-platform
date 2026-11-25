
package com.tech9.knowledge.service;

import com.tech9.knowledge.dto.SearchResult;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SemanticSearchService {

    private final JdbcTemplate jdbcTemplate;
    private final EmbeddingService embeddingService;

    public SemanticSearchService(
            JdbcTemplate jdbcTemplate,
            EmbeddingService embeddingService) {

        this.jdbcTemplate = jdbcTemplate;
        this.embeddingService = embeddingService;
    }

    public List<SearchResult> search(String question, int limit) {

        if (question == null || question.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Search question cannot be empty");
        }

        if (limit < 1 || limit > 50) {
            throw new IllegalArgumentException(
                    "Search limit must be between 1 and 50");
        }

        List<Double> queryEmbedding =
                embeddingService.generateQueryEmbedding(question);

        String vector = queryEmbedding.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(",", "[", "]"));

        String sql =
                "SELECT dc.id, dc.document_id, d.file_name, " +
                        "dc.chunk_index, dc.chunk_text, " +
                        "dc.embedding <=> CAST(? AS vector) AS distance " +
                        "FROM document_chunks dc " +
                        "JOIN documents d ON d.id = dc.document_id " +
                        "WHERE dc.embedding IS NOT NULL " +
                        "ORDER BY dc.embedding <=> CAST(? AS vector) " +
                        "LIMIT ?";

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new SearchResult(
                        rs.getLong("id"),
                        rs.getLong("document_id"),
                        rs.getString("file_name"),
                        rs.getInt("chunk_index"),
                        rs.getString("chunk_text"),
                        rs.getDouble("distance")
                ),
                vector,
                vector,
                limit
        );
    }
}
