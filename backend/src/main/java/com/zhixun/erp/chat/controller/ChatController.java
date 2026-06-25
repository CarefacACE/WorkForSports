package com.zhixun.erp.chat.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zhixun.erp.chat.entity.ChatConversation;
import com.zhixun.erp.chat.entity.ChatGroupNotice;
import com.zhixun.erp.chat.entity.ChatMessage;
import com.zhixun.erp.chat.service.ChatService;
import com.zhixun.erp.common.response.Result;
import com.zhixun.erp.user.entity.User;
import com.zhixun.erp.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/chat")
public class ChatController {

    private final ChatService chatService;
    private final UserMapper userMapper;

    @GetMapping("/conversations")
    public Result<IPage<ChatConversation>> getConversations(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.success(chatService.getConversations(userId, pageNum, pageSize));
    }

    @GetMapping("/conversations/type")
    public Result<List<ChatConversation>> getConversationsByType(
            @RequestParam Long userId,
            @RequestParam String type) {
        return Result.success(chatService.getConversationsByType(userId, type));
    }

    @GetMapping("/conversations/{id}/messages")
    public Result<IPage<ChatMessage>> getMessages(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "50") int pageSize) {
        return Result.success(chatService.getMessages(id, pageNum, pageSize));
    }

    @PostMapping("/conversations")
    public Result<ChatConversation> createConversation(@RequestBody Map<String, Object> body) {
        String type = (String) body.get("type");
        String name = (String) body.get("name");
        Long courseId = body.get("courseId") != null ? Long.valueOf(body.get("courseId").toString()) : null;
        Long ownerId = body.get("ownerId") != null ? Long.valueOf(body.get("ownerId").toString()) : null;
        @SuppressWarnings("unchecked")
        List<Long> memberIds = body.get("memberIds") != null
                ? ((List<Number>) body.get("memberIds")).stream().map(Number::longValue).toList()
                : null;

        ChatConversation conversation = chatService.createConversation(type, name, courseId, ownerId, memberIds);
        return Result.success("创建成功", conversation);
    }

    @PostMapping("/conversations/{id}/join")
    public Result<Void> joinConversation(@PathVariable Long id, @RequestParam Long userId) {
        chatService.addMember(id, userId);
        return Result.success("加入成功", null);
    }

    @GetMapping("/unread")
    public Result<Long> getUnreadCount(@RequestParam Long userId) {
        return Result.success(chatService.getUnreadCount(userId));
    }

    @GetMapping("/unread-conversations")
    public Result<List<Map<String, Object>>> getUnreadConversations(@RequestParam Long userId) {
        return Result.success(chatService.getUnreadConversations(userId));
    }

    @PostMapping("/conversations/{id}/read")
    public Result<Void> markConversationRead(@PathVariable Long id, @RequestParam Long userId) {
        chatService.markConversationRead(id, userId);
        return Result.success("已标记已读", null);
    }

    @GetMapping("/available-groups")
    public Result<List<ChatConversation>> getAvailableGroups(@RequestParam Long userId) {
        return Result.success(chatService.getAvailableGroups(userId));
    }

    @GetMapping("/search-groups")
    public Result<List<ChatConversation>> searchGroups(@RequestParam String keyword, @RequestParam Long userId) {
        return Result.success(chatService.searchGroups(keyword, userId));
    }

    @GetMapping("/search-group-by-id")
    public Result<ChatConversation> searchGroupById(@RequestParam Long id, @RequestParam Long userId) {
        ChatConversation group = chatService.searchGroupById(id, userId);
        if (group == null) {
            return Result.fail(404, "群聊不存在");
        }
        return Result.success(group);
    }

    @GetMapping("/conversations/{id}/members")
    public Result<List<Map<String, Object>>> getGroupMembers(@PathVariable Long id) {
        List<Long> memberIds = chatService.getMemberIds(id);
        List<Map<String, Object>> members = memberIds.stream().map(uid -> {
            User user = userMapper.selectById(uid);
            Map<String, Object> info = new java.util.LinkedHashMap<>();
            info.put("userId", uid);
            info.put("username", user != null ? user.getUsername() : "");
            info.put("realName", user != null ? user.getRealName() : "");
            info.put("role", user != null ? user.getRole() : "");
            return info;
        }).collect(Collectors.toList());
        return Result.success(members);
    }

    @PostMapping("/conversations/{id}/members")
    public Result<Void> addGroupMember(@PathVariable Long id, @RequestParam Long userId) {
        chatService.addMember(id, userId);
        return Result.success("添加成功", null);
    }

    @DeleteMapping("/conversations/{id}/members/{userId}")
    public Result<Void> removeGroupMember(@PathVariable Long id, @PathVariable Long userId) {
        chatService.removeMember(id, userId);
        return Result.success("移除成功", null);
    }

    @PutMapping("/conversations/{id}")
    public Result<Void> updateConversation(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        chatService.updateConversationName(id, name);
        return Result.success("修改成功", null);
    }

    @PostMapping("/conversations/{id}/leave")
    public Result<Void> leaveGroup(@PathVariable Long id, @RequestParam Long userId) {
        chatService.removeMember(id, userId);
        return Result.success("已退出群聊", null);
    }

    @GetMapping("/conversations/{id}/notice")
    public Result<ChatGroupNotice> getGroupNotice(@PathVariable Long id) {
        return Result.success(chatService.getGroupNotice(id));
    }

    @PostMapping("/conversations/{id}/notice")
    public Result<Void> publishGroupNotice(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long publisherId = Long.valueOf(body.get("publisherId").toString());
        String content = (String) body.get("content");
        chatService.publishGroupNotice(id, publisherId, content);
        return Result.success("公告发布成功", null);
    }

    @DeleteMapping("/notice/{id}")
    public Result<Void> deleteGroupNotice(@PathVariable Long id) {
        chatService.deleteGroupNotice(id);
        return Result.success("公告已删除", null);
    }

    /** 发送消息（REST 接口） */
    @PostMapping("/messages")
    public Result<ChatMessage> sendMessage(@RequestBody Map<String, Object> body) {
        Long conversationId = Long.valueOf(body.get("conversationId").toString());
        Long senderId = Long.valueOf(body.get("senderId").toString());
        String content = (String) body.get("content");
        String msgType = body.get("msgType") != null ? (String) body.get("msgType") : "TEXT";
        return Result.success(chatService.sendMessage(conversationId, senderId, content, msgType));
    }

    /* ─── 管理员：聊天管理 ─── */

    /** 管理员查看所有对话 */
    @GetMapping("/admin/all-conversations")
    public Result<List<Map<String, Object>>> getAllConversationsForAdmin() {
        return Result.success(chatService.getAllConversationsForAdmin());
    }

    /** 管理员加入任意对话 */
    @PostMapping("/admin/join/{id}")
    public Result<Void> adminJoinConversation(@PathVariable Long id, @RequestParam Long userId) {
        chatService.adminJoinConversation(id, userId);
        return Result.success("已加入对话", null);
    }

    /** 管理员查看所有用户（用于发起私聊） */
    @GetMapping("/admin/all-users")
    public Result<List<Map<String, Object>>> getAllUsersForAdmin() {
        List<User> users = userMapper.selectList(null);
        List<Map<String, Object>> result = users.stream().map(u -> {
            Map<String, Object> info = new java.util.LinkedHashMap<>();
            info.put("id", u.getId());
            info.put("username", u.getUsername());
            info.put("realName", u.getRealName());
            info.put("role", u.getRole());
            return info;
        }).collect(Collectors.toList());
        return Result.success(result);
    }
}
