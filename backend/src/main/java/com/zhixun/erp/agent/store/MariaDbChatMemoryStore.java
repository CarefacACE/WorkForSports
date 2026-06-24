package com.zhixun.erp.agent.store;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class MariaDbChatMemoryStore implements ChatMemoryStore {

    private final JdbcTemplate jdbcTemplate;

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
