package com.tech9.knowledge.repository;

import com.tech9.knowledge.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository
        extends JpaRepository<Document, Long> {
}