
package com.tech9.knowledge.repository;

import com.tech9.knowledge.entity.DocumentText;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DocumentTextRepository
        extends JpaRepository<DocumentText, Long> {

    Optional<DocumentText> findByDocumentId(Long documentId);
}
