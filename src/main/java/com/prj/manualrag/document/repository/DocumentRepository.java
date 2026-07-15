package com.prj.manualrag.document.repository;

import com.prj.manualrag.document.entity.DocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository
        extends JpaRepository<DocumentEntity, Long> {
}
