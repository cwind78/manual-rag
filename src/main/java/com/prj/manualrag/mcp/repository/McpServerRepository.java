package com.prj.manualrag.mcp.repository;

import com.prj.manualrag.mcp.domain.McpServerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface McpServerRepository extends JpaRepository<McpServerEntity, Long> {
    List<McpServerEntity> findAllByOrderByNameAsc();
    List<McpServerEntity> findAllByEnabledTrue();
}
