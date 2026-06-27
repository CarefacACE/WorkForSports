package com.zhixun.erp.chat.config;

import com.zhixun.erp.chat.handler.ChatWebSocketHandler;
import com.zhixun.erp.chat.service.ChatService;
import com.zhixun.erp.chat.service.FriendService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final ChatService chatService;
    private final FriendService friendService;

    public WebSocketConfig(ChatService chatService, FriendService friendService) {
        this.chatService = chatService;
        this.friendService = friendService;
    }

    @Bean
    public ChatWebSocketHandler chatWebSocketHandler() {
        return new ChatWebSocketHandler(chatService, friendService);
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(chatWebSocketHandler(), "/ws/chat")
                .setAllowedOrigins("*");
    }
}
