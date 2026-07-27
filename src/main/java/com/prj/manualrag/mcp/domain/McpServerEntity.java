package com.prj.manualrag.mcp.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "mcp_servers")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class McpServerEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private String name;
    private String description;
    @Column(nullable = false, length = 1000) private String endpoint;
    @Column(nullable = false) private String authType;
    @Column(length = 4000) private String accessToken;
    private boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate void onUpdate() { updatedAt = LocalDateTime.now(); }
}
