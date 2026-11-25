
package com.tech9.knowledge.dto;

public class SearchResult {

    private Long chunkId;
    private Long documentId;
    private String fileName;
    private Integer chunkIndex;
    private String chunkText;
    private Double distance;

    public SearchResult() {
    }

    public SearchResult(Long chunkId, Long documentId, String fileName,
                        Integer chunkIndex, String chunkText, Double distance) {
        this.chunkId = chunkId;
        this.documentId = documentId;
        this.fileName = fileName;
        this.chunkIndex = chunkIndex;
        this.chunkText = chunkText;
        this.distance = distance;
    }

    public Long getChunkId() {
        return chunkId;
    }

    public void setChunkId(Long chunkId) {
        this.chunkId = chunkId;
    }

    public Long getDocumentId() {
        return documentId;
    }

    public void setDocumentId(Long documentId) {
        this.documentId = documentId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Integer getChunkIndex() {
        return chunkIndex;
    }

    public void setChunkIndex(Integer chunkIndex) {
        this.chunkIndex = chunkIndex;
    }

    public String getChunkText() {
        return chunkText;
    }

    public void setChunkText(String chunkText) {
        this.chunkText = chunkText;
    }

    public Double getDistance() {
        return distance;
    }

    public void setDistance(Double distance) {
        this.distance = distance;
    }
}
