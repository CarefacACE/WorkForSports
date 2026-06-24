package com.zhixun.erp.agent.service;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;
import reactor.core.publisher.Flux;

@AiService(
        streamingChatModel = "openAiStreamingChatModel",
        chatMemoryProvider = "chatMemoryProvider",
        tools = {"scheduleTool", "financeTool", "exerciseTool", "healthTool", "checkInTool"}
)
public interface AgentChatService {

    @SystemMessage(fromResource = "fitness-assistant.md")
    Flux<String> chat(@MemoryId String memoryId, @UserMessage String message);
}
