package com.zhixun.erp.agent.controller;

import com.zhixun.erp.agent.service.AgentChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@CrossOrigin
@RestController
@RequiredArgsConstructor
@RequestMapping("/agent")
public class AgentController {

    private final AgentChatService agentChatService;

    @GetMapping(value = "/chat", produces = "text/event-stream;charset=UTF-8")
    public Flux<String> chat(
            @RequestParam String message,
            @RequestParam Long userId,
            @RequestParam String role) {
        String memoryId = userId + "_" + role;
        return agentChatService.chat(memoryId, message);
    }
}
