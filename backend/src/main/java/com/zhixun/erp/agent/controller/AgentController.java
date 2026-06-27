package com.zhixun.erp.agent.controller;

import com.zhixun.erp.agent.service.AgentChatService;
import com.zhixun.erp.user.entity.User;
import com.zhixun.erp.user.mapper.UserMapper;
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
    private final UserMapper userMapper;

    @GetMapping(value = "/chat", produces = "text/event-stream;charset=UTF-8")
    public Flux<String> chat(
            @RequestParam String message,
            @RequestParam Long userId,
            @RequestParam String role) {
        // Build user identity context injected into every message
        User user = userMapper.selectById(userId);
        String userName = (user != null && user.getRealName() != null)
                ? user.getRealName() : ("用户" + userId);
        String roleLabel = "COACH".equalsIgnoreCase(role) ? "教练" : "会员";
        String identityContext = String.format(
                "[用户身份信息] 用户ID=%d，姓名=%s，角色=%s。调用任何工具时，请直接使用 userId=%d 和 role=\"%s\" 作为参数，不要再询问用户。",
                userId, userName, roleLabel, userId, role);

        String fullMessage = identityContext + "\n\n" + message;
        String memoryId = userId + "_" + role;
        return agentChatService.chat(memoryId, fullMessage);
    }
}
