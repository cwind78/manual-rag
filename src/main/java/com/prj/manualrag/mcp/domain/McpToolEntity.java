package com.prj.manualrag.mcp.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "mcp_tools", uniqueConstraints = @UniqueConstraint(columnNames = {"server_id", "name"}))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class McpToolEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "server_id") private McpServerEntity server;
    @Column(nullable = false) private String name;
    @Column(length = 2000) private String description;
    // PostgreSQL Large Object(oid)가 아닌 일반 text로 저장해야
    // 트랜잭션 밖에서도 도구 스키마를 읽을 수 있다.
    @Column(columnDefinition = "text")
    private String inputSchema;
    private boolean requiresApproval;
}
