package com.zhixun.erp.chat.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zhixun.erp.chat.entity.ChatConversation;
import com.zhixun.erp.chat.entity.ChatFriendRequest;
import com.zhixun.erp.chat.service.FriendService;
import com.zhixun.erp.common.response.Result;
import com.zhixun.erp.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/friend")
public class FriendController {

    private final FriendService friendService;

    @GetMapping("/search")
    public Result<User> searchUser(@RequestParam(required = false) String username,
                                   @RequestParam(required = false) Long id) {
        User user;
        if (id != null) {
            user = friendService.searchUserById(id);
        } else if (username != null && !username.isEmpty()) {
            user = friendService.searchUser(username);
        } else {
            return Result.fail(400, "请输入用户名或ID");
        }
        if (user == null) {
            return Result.fail(404, "用户不存在");
        }
        user.setPassword(null);
        return Result.success(user);
    }

    @PostMapping("/request")
    public Result<ChatFriendRequest> sendRequest(@RequestBody Map<String, Object> body) {
        Long fromUserId = Long.valueOf(body.get("fromUserId").toString());
        Long toUserId = Long.valueOf(body.get("toUserId").toString());
        String type = (String) body.get("type");
        String message = (String) body.getOrDefault("message", "");

        ChatFriendRequest request;
        if ("FRIEND".equals(type)) {
            request = friendService.sendFriendRequest(fromUserId, toUserId, message);
        } else if ("TEMP_CHAT".equals(type)) {
            request = friendService.sendTempChatRequest(fromUserId, toUserId, message);
        } else {
            return Result.fail(400, "无效的申请类型");
        }
        return Result.success("申请已发送", request);
    }

    @PostMapping("/temp-chat")
    public Result<ChatConversation> startTempChat(@RequestBody Map<String, Object> body) {
        Long fromUserId = Long.valueOf(body.get("fromUserId").toString());
        Long toUserId = Long.valueOf(body.get("toUserId").toString());
        ChatConversation conversation = friendService.startTempChat(fromUserId, toUserId);
        return Result.success("会话已创建", conversation);
    }

    @PostMapping("/join-group")
    public Result<Void> joinGroupRequest(@RequestBody Map<String, Object> body) {
        Long userId = Long.valueOf(body.get("userId").toString());
        Long conversationId = Long.valueOf(body.get("conversationId").toString());
        String message = (String) body.getOrDefault("message", "");
        friendService.sendJoinGroupRequest(userId, conversationId, message);
        return Result.success("加群申请已发送", null);
    }

    @PostMapping("/approve/{id}")
    public Result<Void> approveRequest(@PathVariable Long id, @RequestParam Long operatorId) {
        friendService.approveRequest(id, operatorId);
        return Result.success("已同意", null);
    }

    @PostMapping("/reject/{id}")
    public Result<Void> rejectRequest(@PathVariable Long id, @RequestParam Long operatorId) {
        friendService.rejectRequest(id, operatorId);
        return Result.success("已拒绝", null);
    }

    @GetMapping("/requests")
    public Result<IPage<ChatFriendRequest>> getMyRequests(
            @RequestParam Long userId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.success(friendService.getMyRequests(userId, status, pageNum, pageSize));
    }

    @GetMapping("/sent-requests")
    public Result<IPage<ChatFriendRequest>> getSentRequests(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.success(friendService.getSentRequests(userId, pageNum, pageSize));
    }

    /* ─── 拉黑 / 删除好友 ─── */

    @PostMapping("/block")
    public Result<Void> blockUser(@RequestBody Map<String, Object> body) {
        Long userId = Long.valueOf(body.get("userId").toString());
        Long blockedUserId = Long.valueOf(body.get("blockedUserId").toString());
        friendService.blockUser(userId, blockedUserId);
        return Result.success("已拉黑", null);
    }

    @DeleteMapping("/block")
    public Result<Void> unblockUser(@RequestBody Map<String, Object> body) {
        Long userId = Long.valueOf(body.get("userId").toString());
        Long blockedUserId = Long.valueOf(body.get("blockedUserId").toString());
        friendService.unblockUser(userId, blockedUserId);
        return Result.success("已取消拉黑", null);
    }

    @GetMapping("/blocks")
    public Result<List<Map<String, Object>>> getBlockedUsers(@RequestParam Long userId) {
        return Result.success(friendService.getBlockedUsers(userId));
    }

    @DeleteMapping("/{friendUserId}")
    public Result<Void> deleteFriend(@RequestParam Long userId, @PathVariable Long friendUserId) {
        friendService.deleteFriend(userId, friendUserId);
        return Result.success("已删除好友", null);
    }
}
