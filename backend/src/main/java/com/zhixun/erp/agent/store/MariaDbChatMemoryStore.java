package com.zhixun.erp.agent.store;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MariaDbChatMemoryStore implements ChatMemoryStore {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void init() {
        try {
            // Widen the role column to accommodate TOOL_EXECUTION_RESULT (22 chars)
            jdbcTemplate.execute("ALTER TABLE agent_chat_message MODIFY COLUMN role VARCHAR(32) NOT NULL " +
                    "COMMENT 'SYSTEM/USER/ASSISTANT/AI/TOOL_EXECUTION_RESULT'");
        } catch (Exception e) {
            log.debug("agent_chat_message.role column may already be VARCHAR(32): {}", e.getMessage());
        }
        try {
            // Purge all stale memory rows that were corrupted by the old VARCHAR(16) truncation.
            // This prevents "insufficient tool messages following tool_calls message" errors from
            // DeepSeek when it reads broken message chains.
            int deleted = jdbcTemplate.update("DELETE FROM agent_chat_message");
            if (deleted > 0) {
                log.warn("Purged {} stale agent_chat_message rows to fix corrupted memory (role column was too narrow)", deleted);
            }
        } catch (Exception e) {
            log.warn("Failed to purge agent_chat_message: {}", e.getMessage());
        }
    }

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        List<String> jsonList = jdbcTemplate.queryForList(
                "SELECT content FROM agent_chat_message WHERE memory_id = ? ORDER BY id ASC",
                String.class, memoryId.toString());

        List<ChatMessage> messages = new ArrayList<>();
        for (String json : jsonList) {
            try {
                messages.add(ChatMessageDeserializer.messageFromJson(json));
            } catch (Exception ignored) {
            }
        }
        return messages;
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        jdbcTemplate.update("DELETE FROM agent_chat_message WHERE memory_id = ?", memoryId.toString());
        for (ChatMessage message : messages) {
            String json = ChatMessageSerializer.messageToJson(message);
            String role = message.type().name();
            jdbcTemplate.update(
                    "INSERT INTO agent_chat_message (memory_id, role, content, create_time) VALUES (?, ?, ?, NOW())",
                    memoryId.toString(), role, json);
        }
    }

    @Override
    public void deleteMessages(Object memoryId) {
        jdbcTemplate.update("DELETE FROM agent_chat_message WHERE memory_id = ?", memoryId.toString());
    }
}
