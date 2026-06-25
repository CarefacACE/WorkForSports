package com.zhixun.erp.chat.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhixun.erp.chat.entity.ChatConversation;
import com.zhixun.erp.chat.entity.ChatConversationMember;
import com.zhixun.erp.chat.entity.ChatGroupNotice;
import com.zhixun.erp.chat.entity.ChatMessage;
import com.zhixun.erp.chat.entity.ChatReadStatus;
import com.zhixun.erp.chat.mapper.ChatConversationMapper;
import com.zhixun.erp.chat.mapper.ChatConversationMemberMapper;
import com.zhixun.erp.chat.mapper.ChatGroupNoticeMapper;
import com.zhixun.erp.chat.mapper.ChatMessageMapper;
import com.zhixun.erp.chat.mapper.ChatReadStatusMapper;
import com.zhixun.erp.user.entity.User;
import com.zhixun.erp.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatConversationMapper conversationMapper;
    private final ChatConversationMemberMapper memberMapper;
    private final ChatGroupNoticeMapper groupNoticeMapper;
    private final ChatMessageMapper messageMapper;
    private final ChatReadStatusMapper readStatusMapper;
    private final UserMapper userMapper;

    @Transactional
    public ChatConversation createConversation(String type, String name, Long courseId, Long ownerId, List<Long> memberIds) {
        ChatConversation conversation = new ChatConversation();
        conversation.setType(type);
        conversation.setName(name);
        conversation.setCourseId(courseId);
        conversation.setOwnerId(ownerId);
        conversation.setCreateTime(LocalDateTime.now());
        conversationMapper.insert(conversation);

        if (memberIds != null) {
            for (Long userId : memberIds) {
                addMember(conversation.getId(), userId);
            }
        }
        if (ownerId != null && (memberIds == null || !memberIds.contains(ownerId))) {
            addMember(conversation.getId(), ownerId);
        }

        return conversation;
    }

    @Transactional
    public void addMember(Long conversationId, Long userId) {
        ChatConversationMember existing = memberMapper.selectOne(
                new LambdaQueryWrapper<ChatConversationMember>()
                        .eq(ChatConversationMember::getConversationId, conversationId)
                        .eq(ChatConversationMember::getUserId, userId));
        if (existing != null) return;

        ChatConversationMember member = new ChatConversationMember();
        member.setConversationId(conversationId);
        member.setUserId(userId);
        member.setJoinedAt(LocalDateTime.now());
        memberMapper.insert(member);
    }

    public boolean isMember(Long conversationId, Long userId) {
        return memberMapper.selectCount(
                new LambdaQueryWrapper<ChatConversationMember>()
                        .eq(ChatConversationMember::getConversationId, conversationId)
                        .eq(ChatConversationMember::getUserId, userId)) > 0;
    }

    public List<Long> getMemberIds(Long conversationId) {
        return memberMapper.selectList(
                new LambdaQueryWrapper<ChatConversationMember>()
                        .eq(ChatConversationMember::getConversationId, conversationId))
                .stream().map(ChatConversationMember::getUserId).collect(Collectors.toList());
    }

    public void removeMember(Long conversationId, Long userId) {
        memberMapper.delete(
                new LambdaQueryWrapper<ChatConversationMember>()
                        .eq(ChatConversationMember::getConversationId, conversationId)
                        .eq(ChatConversationMember::getUserId, userId));
    }

    public void updateConversationName(Long conversationId, String name) {
        ChatConversation conversation = conversationMapper.selectById(conversationId);
        if (conversation != null) {
            conversation.setName(name);
            conversation.setUpdateTime(LocalDateTime.now());
            conversationMapper.updateById(conversation);
        }
    }

    public ChatMessage saveMessage(Long conversationId, Long senderId, String content, String msgType) {
        ChatMessage msg = new ChatMessage();
        msg.setConversationId(conversationId);
        msg.setSenderId(senderId);
        msg.setContent(content);
        msg.setMsgType(msgType);
        msg.setCreateTime(LocalDateTime.now());
        messageMapper.insert(msg);
        return msg;
    }

    public IPage<ChatConversation> getConversations(Long userId, int pageNum, int pageSize) {
        List<Long> conversationIds = memberMapper.selectList(
                new LambdaQueryWrapper<ChatConversationMember>()
                        .eq(ChatConversationMember::getUserId, userId))
                .stream().map(ChatConversationMember::getConversationId).collect(Collectors.toList());

        if (conversationIds.isEmpty()) {
            return new Page<>(pageNum, pageSize);
        }

        Page<ChatConversation> page = new Page<>(pageNum, pageSize);
        return conversationMapper.selectPage(page,
                new LambdaQueryWrapper<ChatConversation>()
                        .in(ChatConversation::getId, conversationIds)
                        .orderByDesc(ChatConversation::getUpdateTime));
    }

    public List<ChatConversation> getConversationsByType(Long userId, String type) {
        List<Long> conversationIds = memberMapper.selectList(
                new LambdaQueryWrapper<ChatConversationMember>()
                        .eq(ChatConversationMember::getUserId, userId))
                .stream().map(ChatConversationMember::getConversationId).collect(Collectors.toList());

        if (conversationIds.isEmpty()) return List.of();

        List<ChatConversation> conversations = conversationMapper.selectList(
                new LambdaQueryWrapper<ChatConversation>()
                        .in(ChatConversation::getId, conversationIds)
                        .eq(ChatConversation::getType, type)
                        .orderByDesc(ChatConversation::getUpdateTime));

        if ("PRIVATE".equals(type)) {
            for (ChatConversation conv : conversations) {
                List<Long> memberIds = memberMapper.selectList(
                        new LambdaQueryWrapper<ChatConversationMember>()
                                .eq(ChatConversationMember::getConversationId, conv.getId()))
                        .stream().map(ChatConversationMember::getUserId).collect(Collectors.toList());
                Long otherUserId = memberIds.stream().filter(id -> !id.equals(userId)).findFirst().orElse(null);
                if (otherUserId != null) {
                    User other = userMapper.selectById(otherUserId);
                    if (other != null) {
                        conv.setName(other.getRealName() != null && !other.getRealName().isEmpty()
                                ? other.getRealName() : other.getUsername());
                    }
                }
            }
        }

        return conversations;
    }

    public IPage<ChatMessage> getMessages(Long conversationId, int pageNum, int pageSize) {
        Page<ChatMessage> page = new Page<>(pageNum, pageSize);
        return messageMapper.selectPage(page,
                new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getConversationId, conversationId)
                        .orderByAsc(ChatMessage::getCreateTime));
    }

    public long getUnreadCount(Long userId) {
        List<Long> conversationIds = memberMapper.selectList(
                new LambdaQueryWrapper<ChatConversationMember>()
                        .eq(ChatConversationMember::getUserId, userId))
                .stream().map(ChatConversationMember::getConversationId).collect(Collectors.toList());

        if (conversationIds.isEmpty()) return 0;

        long totalUnread = 0;
        for (Long convId : conversationIds) {
            ChatReadStatus readStatus = readStatusMapper.selectOne(
                    new LambdaQueryWrapper<ChatReadStatus>()
                            .eq(ChatReadStatus::getUserId, userId)
                            .eq(ChatReadStatus::getConversationId, convId));

            LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<ChatMessage>()
                    .eq(ChatMessage::getConversationId, convId)
                    .ne(ChatMessage::getSenderId, userId);

            if (readStatus != null && readStatus.getLastReadMessageId() != null && readStatus.getLastReadMessageId() > 0) {
                wrapper.gt(ChatMessage::getId, readStatus.getLastReadMessageId());
            }

            totalUnread += messageMapper.selectCount(wrapper);
        }
        return totalUnread;
    }

    public void markConversationRead(Long conversationId, Long userId) {
        ChatMessage lastMessage = messageMapper.selectOne(
                new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getConversationId, conversationId)
                        .orderByDesc(ChatMessage::getId)
                        .last("LIMIT 1"));

        if (lastMessage == null) return;

        ChatReadStatus existing = readStatusMapper.selectOne(
                new LambdaQueryWrapper<ChatReadStatus>()
                        .eq(ChatReadStatus::getUserId, userId)
                        .eq(ChatReadStatus::getConversationId, conversationId));

        if (existing != null) {
            existing.setLastReadMessageId(lastMessage.getId());
            existing.setUpdateTime(LocalDateTime.now());
            readStatusMapper.updateById(existing);
        } else {
            ChatReadStatus status = new ChatReadStatus();
            status.setUserId(userId);
            status.setConversationId(conversationId);
            status.setLastReadMessageId(lastMessage.getId());
            status.setUpdateTime(LocalDateTime.now());
            readStatusMapper.insert(status);
        }
    }

    public ChatConversation getConversation(Long conversationId) {
        return conversationMapper.selectById(conversationId);
    }

    public List<Long> getCommonPrivateConversations(Long userId1, Long userId2) {
        List<Long> user1Conversations = memberMapper.selectList(
                new LambdaQueryWrapper<ChatConversationMember>()
                        .eq(ChatConversationMember::getUserId, userId1))
                .stream().map(ChatConversationMember::getConversationId).collect(Collectors.toList());

        if (user1Conversations.isEmpty()) return List.of();

        List<Long> user2Conversations = memberMapper.selectList(
                new LambdaQueryWrapper<ChatConversationMember>()
                        .eq(ChatConversationMember::getUserId, userId2)
                        .in(ChatConversationMember::getConversationId, user1Conversations))
                .stream().map(ChatConversationMember::getConversationId).collect(Collectors.toList());

        if (user2Conversations.isEmpty()) return List.of();

        return conversationMapper.selectList(
                new LambdaQueryWrapper<ChatConversation>()
                        .in(ChatConversation::getId, user2Conversations)
                        .eq(ChatConversation::getType, "PRIVATE"))
                .stream().map(ChatConversation::getId).collect(Collectors.toList());
    }

    public List<ChatConversation> getAvailableGroups(Long userId) {
        List<Long> myConversationIds = memberMapper.selectList(
                new LambdaQueryWrapper<ChatConversationMember>()
                        .eq(ChatConversationMember::getUserId, userId))
                .stream().map(ChatConversationMember::getConversationId).collect(Collectors.toList());

        LambdaQueryWrapper<ChatConversation> wrapper = new LambdaQueryWrapper<ChatConversation>()
                .eq(ChatConversation::getType, "GROUP");
        if (!myConversationIds.isEmpty()) {
            wrapper.notIn(ChatConversation::getId, myConversationIds);
        }
        wrapper.orderByDesc(ChatConversation::getCreateTime);
        return conversationMapper.selectList(wrapper);
    }

    public List<ChatConversation> searchGroups(String keyword, Long userId) {
        List<Long> myConversationIds = memberMapper.selectList(
                new LambdaQueryWrapper<ChatConversationMember>()
                        .eq(ChatConversationMember::getUserId, userId))
                .stream().map(ChatConversationMember::getConversationId).collect(Collectors.toList());

        LambdaQueryWrapper<ChatConversation> wrapper = new LambdaQueryWrapper<ChatConversation>()
                .eq(ChatConversation::getType, "GROUP")
                .like(ChatConversation::getName, keyword);
        if (!myConversationIds.isEmpty()) {
            wrapper.notIn(ChatConversation::getId, myConversationIds);
        }
        wrapper.orderByDesc(ChatConversation::getCreateTime);
        return conversationMapper.selectList(wrapper);
    }

    public ChatConversation searchGroupById(Long id, Long userId) {
        ChatConversation conversation = conversationMapper.selectById(id);
        if (conversation == null || !"GROUP".equals(conversation.getType())) {
            return null;
        }
        return conversation;
    }

    public List<Map<String, Object>> getUnreadConversations(Long userId) {
        List<Long> myConversationIds = memberMapper.selectList(
                new LambdaQueryWrapper<ChatConversationMember>()
                        .eq(ChatConversationMember::getUserId, userId))
                .stream().map(ChatConversationMember::getConversationId).collect(Collectors.toList());

        if (myConversationIds.isEmpty()) return List.of();

        List<Map<String, Object>> result = new ArrayList<>();
        for (Long convId : myConversationIds) {
            ChatReadStatus readStatus = readStatusMapper.selectOne(
                    new LambdaQueryWrapper<ChatReadStatus>()
                            .eq(ChatReadStatus::getUserId, userId)
                            .eq(ChatReadStatus::getConversationId, convId));

            LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<ChatMessage>()
                    .eq(ChatMessage::getConversationId, convId)
                    .ne(ChatMessage::getSenderId, userId);

            if (readStatus != null && readStatus.getLastReadMessageId() != null && readStatus.getLastReadMessageId() > 0) {
                wrapper.gt(ChatMessage::getId, readStatus.getLastReadMessageId());
            }

            List<ChatMessage> unreadMessages = messageMapper.selectList(
                    wrapper.orderByDesc(ChatMessage::getCreateTime).last("LIMIT 5"));

            if (unreadMessages.isEmpty()) continue;

            ChatConversation conversation = conversationMapper.selectById(convId);
            if (conversation == null) continue;

            String convName = conversation.getName();
            if ("PRIVATE".equals(conversation.getType())) {
                List<Long> memberIds = memberMapper.selectList(
                        new LambdaQueryWrapper<ChatConversationMember>()
                                .eq(ChatConversationMember::getConversationId, convId))
                        .stream().map(ChatConversationMember::getUserId).collect(Collectors.toList());
                Long otherUserId = memberIds.stream().filter(id -> !id.equals(userId)).findFirst().orElse(null);
                if (otherUserId != null) {
                    User other = userMapper.selectById(otherUserId);
                    if (other != null) {
                        convName = other.getRealName() != null && !other.getRealName().isEmpty()
                                ? other.getRealName() : other.getUsername();
                    }
                }
            }

            ChatMessage lastMsg = unreadMessages.get(0);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("conversationId", convId);
            item.put("conversationName", convName);
            item.put("conversationType", conversation.getType());
            item.put("unreadCount", unreadMessages.size());
            item.put("lastMessage", lastMsg.getContent());
            item.put("lastSenderId", lastMsg.getSenderId());
            item.put("lastTime", lastMsg.getCreateTime());
            result.add(item);
        }

        result.sort((a, b) -> {
            java.time.LocalDateTime ta = (java.time.LocalDateTime) a.get("lastTime");
            java.time.LocalDateTime tb = (java.time.LocalDateTime) b.get("lastTime");
            return tb.compareTo(ta);
        });

        return result;
    }

    public ChatGroupNotice getGroupNotice(Long conversationId) {
        return groupNoticeMapper.selectOne(
                new LambdaQueryWrapper<ChatGroupNotice>()
                        .eq(ChatGroupNotice::getConversationId, conversationId)
                        .orderByDesc(ChatGroupNotice::getCreateTime)
                        .last("LIMIT 1"));
    }

    public void publishGroupNotice(Long conversationId, Long publisherId, String content) {
        ChatGroupNotice notice = new ChatGroupNotice();
        notice.setConversationId(conversationId);
        notice.setPublisherId(publisherId);
        notice.setContent(content);
        notice.setCreateTime(LocalDateTime.now());
        groupNoticeMapper.insert(notice);
    }

    public void deleteGroupNotice(Long noticeId) {
        groupNoticeMapper.deleteById(noticeId);
    }

    /* ─── 管理员：聊天管理 ─── */

    /** 管理员查看所有对话（群聊 + 私聊），附带成员数和最后消息 */
    public List<Map<String, Object>> getAllConversationsForAdmin() {
        List<ChatConversation> allConvs = conversationMapper.selectList(
                new LambdaQueryWrapper<ChatConversation>()
                        .orderByDesc(ChatConversation::getUpdateTime));

        List<Map<String, Object>> result = new ArrayList<>();
        for (ChatConversation conv : allConvs) {
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("id", conv.getId());
            info.put("type", conv.getType());
            info.put("name", conv.getName());
            info.put("ownerId", conv.getOwnerId());
            info.put("courseId", conv.getCourseId());
            info.put("createTime", conv.getCreateTime());
            info.put("updateTime", conv.getUpdateTime());

            // 成员数
            long memberCount = memberMapper.selectCount(
                    new LambdaQueryWrapper<ChatConversationMember>()
                            .eq(ChatConversationMember::getConversationId, conv.getId()));
            info.put("memberCount", memberCount);

            // 最后一条消息预览
            List<ChatMessage> lastMsgs = messageMapper.selectList(
                    new LambdaQueryWrapper<ChatMessage>()
                            .eq(ChatMessage::getConversationId, conv.getId())
                            .orderByDesc(ChatMessage::getCreateTime)
                            .last("LIMIT 1"));
            if (!lastMsgs.isEmpty()) {
                ChatMessage lastMsg = lastMsgs.get(0);
                String preview = lastMsg.getContent();
                if (preview.length() > 50) preview = preview.substring(0, 50) + "...";
                info.put("lastMessage", preview);
                info.put("lastMessageTime", lastMsg.getCreateTime());
            }

            result.add(info);
        }
        return result;
    }

    /** 管理员加入任意对话 */
    public void adminJoinConversation(Long conversationId, Long adminUserId) {
        ChatConversation conv = conversationMapper.selectById(conversationId);
        if (conv == null) throw new RuntimeException("对话不存在");
        // 如果已经是成员就跳过
        if (isMember(conversationId, adminUserId)) return;
        addMember(conversationId, adminUserId);
    }

    /** 发送消息（REST 接口，同时更新对话时间） */
    public ChatMessage sendMessage(Long conversationId, Long senderId, String content, String msgType) {
        if (!isMember(conversationId, senderId)) {
            throw new RuntimeException("你不是该会话的成员");
        }
        ChatMessage msg = saveMessage(conversationId, senderId, content, msgType);
        // 更新对话的最后更新时间
        ChatConversation conv = conversationMapper.selectById(conversationId);
        if (conv != null) {
            conv.setUpdateTime(LocalDateTime.now());
            conversationMapper.updateById(conv);
        }
        return msg;
    }
}
