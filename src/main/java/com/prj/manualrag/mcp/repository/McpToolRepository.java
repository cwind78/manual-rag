package com.prj.manualrag.mcp.repository;

import com.prj.manualrag.mcp.domain.McpServerEntity;
import com.prj.manualrag.mcp.domain.McpToolEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface McpToolRepository extends JpaRepository<McpToolEntity, Long> {
    List<McpToolEntity> findAllByServerOrderByNameAsc(McpServerEntity server);
    void deleteAllByServer(McpServerEntity server);
}
