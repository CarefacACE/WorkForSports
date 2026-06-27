package com.zhixun.erp.chat.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhixun.erp.chat.entity.ChatMessage;
import com.zhixun.erp.chat.service.ChatService;
import com.zhixun.erp.chat.service.FriendService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ChatWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(ChatWebSocketHandler.class);
    private final ChatService chatService;
    private final FriendService friendService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final ConcurrentHashMap<Long, WebSocketSession> onlineUsers = new ConcurrentHashMap<>();

    public ChatWebSocketHandler(ChatService chatService, FriendService friendService) {
        this.chatService = chatService;
        this.friendService = friendService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long userId = getUserIdFromSession(session);
        log.info("WebSocket connection established, userId={}, sessionId={}", userId, session.getId());
        if (userId != null) {
            onlineUsers.put(userId, session);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long userId = getUserIdFromSession(session);
        log.info("WebSocket connection closed, userId={}, status={}", userId, status);
        if (userId != null) {
            onlineUsers.remove(userId);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Long senderId = getUserIdFromSession(session);
        log.info("WebSocket message received, senderId={}, payload={}", senderId, message.getPayload());
        if (senderId == null) return;

        Map<String, Object> payload = objectMapper.readValue(message.getPayload(), Map.class);
        String action = (String) payload.get("action");

        if ("send".equals(action)) {
            Long conversationId = Long.valueOf(payload.get("conversationId").toString());
            String content = (String) payload.get("content");
            String msgType = payload.get("msgType") != null ? (String) payload.get("msgType") : "TEXT";

            if (!chatService.isMember(conversationId, senderId)) {
                sendMessage(session, Map.of("type", "error", "message", "你不是该会话成员"));
                return;
            }

            // 保存消息（会检查禁言，禁言抛出异常）
            ChatMessage msg;
            try {
                msg = chatService.saveMessage(conversationId, senderId, content, msgType);
            } catch (RuntimeException e) {
                String errMsg = e.getMessage();
                // 如果是禁言，补充剩余时长
                if (errMsg != null && errMsg.contains("已被禁言")) {
                    try {
                        String remaining = chatService.getMuteRemaining(conversationId, senderId);
                        if (remaining != null) errMsg += "，剩余" + remaining;
                    } catch (Exception ignored) {}
                }
                sendMessage(session, Map.of("type", "error", "message", errMsg));
                return;
            }

            Map<String, Object> wsMsg = Map.of(
                    "type", "message",
                    "conversationId", conversationId,
                    "senderId", senderId,
                    "content", content,
                    "msgType", msgType,
                    "messageId", msg.getId(),
                    "createTime", msg.getCreateTime().toString()
            );

            String json = objectMapper.writeValueAsString(wsMsg);
            chatService.getMemberIds(conversationId).forEach(memberId -> {
                // 被拉黑方收不到消息（接收方拉黑了发送方）
                if (friendService.isBlocked(memberId, senderId)) return;
                WebSocketSession memberSession = onlineUsers.get(memberId);
                if (memberSession != null && memberSession.isOpen()) {
                    try {
                        memberSession.sendMessage(new TextMessage(json));
                    } catch (Exception ignored) {}
                }
            });
        } else if ("ping".equals(action)) {
            sendMessage(session, Map.of("type", "pong"));
        }
    }

    public void sendToUser(Long userId, Map<String, Object> data) {
        WebSocketSession session = onlineUsers.get(userId);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(data)));
            } catch (Exception ignored) {}
        }
    }

    private Long getUserIdFromSession(WebSocketSession session) {
        try {
            String query = session.getUri() != null ? session.getUri().getQuery() : null;
            if (query != null && query.contains("userId=")) {
                String userIdStr = query.replaceAll(".*userId=([^&]+).*", "$1");
                return Long.parseLong(userIdStr);
            }
        } catch (Exception ignored) {}
        return null;
    }

    private void sendMessage(WebSocketSession session, Map<String, Object> data) {
        try {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(data)));
        } catch (Exception ignored) {}
    }
}
