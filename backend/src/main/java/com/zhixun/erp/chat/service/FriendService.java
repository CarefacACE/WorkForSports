package com.zhixun.erp.chat.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhixun.erp.chat.entity.ChatConversation;
import com.zhixun.erp.chat.entity.ChatFriendRequest;
import com.zhixun.erp.chat.mapper.ChatConversationMapper;
import com.zhixun.erp.chat.mapper.ChatFriendRequestMapper;
import com.zhixun.erp.chat.mapper.ChatConversationMemberMapper;
import com.zhixun.erp.user.entity.User;
import com.zhixun.erp.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FriendService {

    private final ChatFriendRequestMapper requestMapper;
    private final ChatConversationMapper conversationMapper;
    private final ChatConversationMemberMapper memberMapper;
    private final ChatService chatService;
    private final NotificationService notificationService;
    private final UserMapper userMapper;

    public User searchUser(String username) {
        return userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getUsername, username)
                        .ne(User::getRole, "ADMIN"));
    }

    public User searchUserById(Long id) {
        User user = userMapper.selectById(id);
        if (user != null && "ADMIN".equals(user.getRole())) {
            return null;
        }
        return user;
    }

    @Transactional
    public ChatConversation startTempChat(Long fromUserId, Long toUserId) {
        if (fromUserId.equals(toUserId)) {
            throw new RuntimeException("不能与自己聊天");
        }

        User toUser = userMapper.selectById(toUserId);
        if (toUser == null || "ADMIN".equals(toUser.getRole())) {
            throw new RuntimeException("用户不存在");
        }

        List<Long> commonConversations = chatService.getCommonPrivateConversations(fromUserId, toUserId);
        if (!commonConversations.isEmpty()) {
            return chatService.getConversation(commonConversations.get(0));
        }

        return chatService.createConversation("PRIVATE", null, null, fromUserId, List.of(toUserId));
    }

    @Transactional
    public ChatFriendRequest sendFriendRequest(Long fromUserId, Long toUserId, String message) {
        if (fromUserId.equals(toUserId)) {
            throw new RuntimeException("不能添加自己为好友");
        }

        User toUser = userMapper.selectById(toUserId);
        if (toUser == null || "ADMIN".equals(toUser.getRole())) {
            throw new RuntimeException("用户不存在");
        }

        List<Long> commonConversations = chatService.getCommonPrivateConversations(fromUserId, toUserId);
        if (!commonConversations.isEmpty()) {
            throw new RuntimeException("你们已经是好友了");
        }

        ChatFriendRequest existing = requestMapper.selectOne(
                new LambdaQueryWrapper<ChatFriendRequest>()
                        .eq(ChatFriendRequest::getFromUserId, fromUserId)
                        .eq(ChatFriendRequest::getToUserId, toUserId)
                        .eq(ChatFriendRequest::getRequestType, "FRIEND")
                        .eq(ChatFriendRequest::getStatus, "PENDING"));
        if (existing != null) {
            throw new RuntimeException("已发送过好友申请，请等待对方处理");
        }

        ChatFriendRequest request = new ChatFriendRequest();
        request.setFromUserId(fromUserId);
        request.setToUserId(toUserId);
        request.setRequestType("FRIEND");
        request.setStatus("PENDING");
        request.setMessage(message);
        request.setCreateTime(LocalDateTime.now());
        requestMapper.insert(request);

        User fromUser = userMapper.selectById(fromUserId);
        notificationService.sendNotification(toUserId,
                "好友申请",
                (fromUser != null ? fromUser.getRealName() : "") + " 请求添加你为好友",
                "FRIEND_REQUEST", request.getId());

        return request;
    }

    @Transactional
    public ChatFriendRequest sendTempChatRequest(Long fromUserId, Long toUserId, String message) {
        if (fromUserId.equals(toUserId)) {
            throw new RuntimeException("不能与自己发起临时会话");
        }

        User toUser = userMapper.selectById(toUserId);
        if (toUser == null || "ADMIN".equals(toUser.getRole())) {
            throw new RuntimeException("用户不存在");
        }

        List<Long> commonConversations = chatService.getCommonPrivateConversations(fromUserId, toUserId);
        if (!commonConversations.isEmpty()) {
            throw new RuntimeException("与该用户已有私信会话");
        }

        ChatFriendRequest existing = requestMapper.selectOne(
                new LambdaQueryWrapper<ChatFriendRequest>()
                        .eq(ChatFriendRequest::getFromUserId, fromUserId)
                        .eq(ChatFriendRequest::getToUserId, toUserId)
                        .eq(ChatFriendRequest::getRequestType, "TEMP_CHAT")
                        .eq(ChatFriendRequest::getStatus, "PENDING"));
        if (existing != null) {
            throw new RuntimeException("已发送过会话申请，请等待对方处理");
        }

        ChatFriendRequest request = new ChatFriendRequest();
        request.setFromUserId(fromUserId);
        request.setToUserId(toUserId);
        request.setRequestType("TEMP_CHAT");
        request.setStatus("PENDING");
        request.setMessage(message);
        request.setCreateTime(LocalDateTime.now());
        requestMapper.insert(request);

        User fromUser = userMapper.selectById(fromUserId);
        notificationService.sendNotification(toUserId,
                "会话申请",
                (fromUser != null ? fromUser.getRealName() : "") + " 请求与你进行私信",
                "TEMP_CHAT_REQUEST", request.getId());

        return request;
    }

    @Transactional
    public void sendJoinGroupRequest(Long userId, Long conversationId, String message) {
        ChatConversation conversation = conversationMapper.selectById(conversationId);
        if (conversation == null || !"GROUP".equals(conversation.getType())) {
            throw new RuntimeException("群聊不存在");
        }

        if (chatService.isMember(conversationId, userId)) {
            throw new RuntimeException("你已是该群聊成员");
        }

        ChatFriendRequest existing = requestMapper.selectOne(
                new LambdaQueryWrapper<ChatFriendRequest>()
                        .eq(ChatFriendRequest::getFromUserId, userId)
                        .eq(ChatFriendRequest::getConversationId, conversationId)
                        .eq(ChatFriendRequest::getRequestType, "JOIN_GROUP")
                        .eq(ChatFriendRequest::getStatus, "PENDING"));
        if (existing != null) {
            throw new RuntimeException("已发送过加群申请，请等待群主处理");
        }

        ChatFriendRequest request = new ChatFriendRequest();
        request.setFromUserId(userId);
        request.setToUserId(conversation.getOwnerId());
        request.setRequestType("JOIN_GROUP");
        request.setConversationId(conversationId);
        request.setStatus("PENDING");
        request.setMessage(message);
        request.setCreateTime(LocalDateTime.now());
        requestMapper.insert(request);

        User fromUser = userMapper.selectById(userId);
        notificationService.sendNotification(conversation.getOwnerId(),
                "加群申请",
                (fromUser != null ? fromUser.getRealName() : "") + " 申请加入群聊「" + conversation.getName() + "」",
                "JOIN_GROUP_REQUEST", request.getId());
    }

    @Transactional
    public void approveRequest(Long requestId, Long operatorId) {
        ChatFriendRequest request = requestMapper.selectById(requestId);
        if (request == null || !"PENDING".equals(request.getStatus())) {
            throw new RuntimeException("申请不存在或已处理");
        }

        if (!request.getToUserId().equals(operatorId)) {
            throw new RuntimeException("无权处理此申请");
        }

        request.setStatus("APPROVED");
        request.setUpdateTime(LocalDateTime.now());
        requestMapper.updateById(request);

        String type = request.getRequestType();
        if ("FRIEND".equals(type)) {
            ChatConversation conversation = chatService.createConversation(
                    "PRIVATE", null, null, request.getFromUserId(),
                    List.of(request.getToUserId()));
            notificationService.sendNotification(request.getFromUserId(),
                    "好友申请已通过",
                    "你与对方已成为好友，可以开始私信了",
                    "FRIEND_APPROVED", conversation.getId());
        } else if ("TEMP_CHAT".equals(type)) {
            ChatConversation conversation = chatService.createConversation(
                    "PRIVATE", null, null, request.getFromUserId(),
                    List.of(request.getToUserId()));
            notificationService.sendNotification(request.getFromUserId(),
                    "会话申请已通过",
                    "对方已同意与你进行私信",
                    "TEMP_CHAT_APPROVED", conversation.getId());
        } else if ("JOIN_GROUP".equals(type)) {
            if (request.getConversationId() != null) {
                chatService.addMember(request.getConversationId(), request.getFromUserId());
                notificationService.sendNotification(request.getFromUserId(),
                        "加群申请已通过",
                        "你已成功加入群聊",
                        "JOIN_GROUP_APPROVED", request.getConversationId());
            }
        }
    }

    @Transactional
    public void rejectRequest(Long requestId, Long operatorId) {
        ChatFriendRequest request = requestMapper.selectById(requestId);
        if (request == null || !"PENDING".equals(request.getStatus())) {
            throw new RuntimeException("申请不存在或已处理");
        }

        if (!request.getToUserId().equals(operatorId)) {
            throw new RuntimeException("无权处理此申请");
        }

        request.setStatus("REJECTED");
        request.setUpdateTime(LocalDateTime.now());
        requestMapper.updateById(request);

        notificationService.sendNotification(request.getFromUserId(),
                "申请被拒绝",
                "你的" + ("FRIEND".equals(request.getRequestType()) ? "好友" :
                        "JOIN_GROUP".equals(request.getRequestType()) ? "加群" : "会话") + "申请已被拒绝",
                "REQUEST_REJECTED", null);
    }

    public IPage<ChatFriendRequest> getMyRequests(Long userId, String status, int pageNum, int pageSize) {
        Page<ChatFriendRequest> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ChatFriendRequest> wrapper = new LambdaQueryWrapper<ChatFriendRequest>()
                .eq(ChatFriendRequest::getToUserId, userId);
        if (status != null && !status.isEmpty()) {
            wrapper.eq(ChatFriendRequest::getStatus, status);
        }
        wrapper.orderByDesc(ChatFriendRequest::getCreateTime);
        return requestMapper.selectPage(page, wrapper);
    }

    public IPage<ChatFriendRequest> getSentRequests(Long userId, int pageNum, int pageSize) {
        Page<ChatFriendRequest> page = new Page<>(pageNum, pageSize);
        return requestMapper.selectPage(page,
                new LambdaQueryWrapper<ChatFriendRequest>()
                        .eq(ChatFriendRequest::getFromUserId, userId)
                        .orderByDesc(ChatFriendRequest::getCreateTime));
    }
}
