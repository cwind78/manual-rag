package com.prj.manualrag.agent.port;

import org.springframework.ai.tool.ToolCallback;

import java.util.List;

/** Agent-side port for tools supplied by external capability providers. */
public interface ExternalToolProvider {
    List<ToolCallback> activeTools();
}
