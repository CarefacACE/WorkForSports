package com.zhixun.erp.chat.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhixun.erp.chat.entity.ChatBlock;
import com.zhixun.erp.chat.entity.ChatConversation;
import com.zhixun.erp.chat.entity.ChatConversationMember;
import com.zhixun.erp.chat.entity.ChatGroupNotice;
import com.zhixun.erp.chat.entity.ChatMessage;
import com.zhixun.erp.chat.entity.ChatReadStatus;
import com.zhixun.erp.chat.mapper.ChatBlockMapper;
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

import java.time.Duration;
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
    private final ChatBlockMapper chatBlockMapper;

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
        // 管理员无视禁言和拉黑
        User sender = userMapper.selectById(senderId);
        boolean isAdmin = sender != null && "ADMIN".equals(sender.getRole());

        ChatConversation conv = conversationMapper.selectById(conversationId);

        // 检查是否被禁言（仅群聊，管理员跳过禁言检查）
        if (!isAdmin && conv != null && "GROUP".equals(conv.getType())) {
            ChatConversationMember member = memberMapper.selectOne(
                    new LambdaQueryWrapper<ChatConversationMember>()
                            .eq(ChatConversationMember::getConversationId, conversationId)
                            .eq(ChatConversationMember::getUserId, senderId));
            if (member != null && member.getIsMuted() != null && member.getIsMuted() == 1) {
                if (member.getMutedUntil() == null || member.getMutedUntil().isAfter(LocalDateTime.now())) {
                    String remaining = getMuteRemaining(conversationId, senderId);
                    throw new RuntimeException("你已被禁言，无法发送消息" + (remaining != null ? "，剩余" + remaining : ""));
                } else {
                    // 禁言已到期，自动解禁
                    member.setIsMuted(0);
                    member.setMutedUntil(null);
                    memberMapper.updateById(member);
                }
            }
        }

        // 拉黑检查（私聊）：被拉黑方无法向拉黑方发送消息（微信模式）
        if (!isAdmin && conv != null && "PRIVATE".equals(conv.getType())) {
            List<Long> memberIds = memberMapper.selectList(
                    new LambdaQueryWrapper<ChatConversationMember>()
                            .eq(ChatConversationMember::getConversationId, conversationId))
                    .stream().map(ChatConversationMember::getUserId).collect(Collectors.toList());
            Long otherUserId = memberIds.stream().filter(id -> !id.equals(senderId)).findFirst().orElse(null);
            if (otherUserId != null) {
                User otherUser = userMapper.selectById(otherUserId);
                boolean otherIsAdmin = otherUser != null && "ADMIN".equals(otherUser.getRole());
                if (!otherIsAdmin) {
                    long blockCount = chatBlockMapper.selectCount(
                            new LambdaQueryWrapper<ChatBlock>()
                                    .eq(ChatBlock::getUserId, otherUserId)
                                    .eq(ChatBlock::getBlockedUserId, senderId));
                    if (blockCount > 0) {
                        throw new RuntimeException("消息已发出，但被对方拒收了");
                    }
                }
            }
        }

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

    /* ─── 群管理：禁言 / 群昵称 ─── */

    public void muteAllMembers(Long conversationId, Long operatorId) {
        ChatConversation conv = conversationMapper.selectById(conversationId);
        if (conv == null || !"GROUP".equals(conv.getType())) {
            throw new RuntimeException("群聊不存在");
        }
        if (!operatorId.equals(conv.getOwnerId())) {
            throw new RuntimeException("只有群主可以全员禁言");
        }
        List<ChatConversationMember> members = memberMapper.selectList(
                new LambdaQueryWrapper<ChatConversationMember>()
                        .eq(ChatConversationMember::getConversationId, conversationId));
        for (ChatConversationMember m : members) {
            if (m.getUserId().equals(operatorId)) continue; // 不给自己禁言
            m.setIsMuted(1);
            m.setMutedUntil(null); // 永久禁言
            memberMapper.updateById(m);
        }

        User operator = userMapper.selectById(operatorId);
        String opName = operator != null ? (operator.getRealName() != null ? operator.getRealName() : operator.getUsername()) : "群主";
        ChatMessage sysMsg = new ChatMessage();
        sysMsg.setConversationId(conversationId);
        sysMsg.setSenderId(0L);
        sysMsg.setContent(opName + " 已开启全员禁言");
        sysMsg.setMsgType("SYSTEM");
        sysMsg.setCreateTime(LocalDateTime.now());
        messageMapper.insert(sysMsg);
    }

    public void unmuteAllMembers(Long conversationId, Long operatorId) {
        ChatConversation conv = conversationMapper.selectById(conversationId);
        if (conv == null || !"GROUP".equals(conv.getType())) {
            throw new RuntimeException("群聊不存在");
        }
        if (!operatorId.equals(conv.getOwnerId())) {
            throw new RuntimeException("只有群主可以解除全员禁言");
        }
        List<ChatConversationMember> members = memberMapper.selectList(
                new LambdaQueryWrapper<ChatConversationMember>()
                        .eq(ChatConversationMember::getConversationId, conversationId));
        for (ChatConversationMember m : members) {
            m.setIsMuted(0);
            m.setMutedUntil(null);
            memberMapper.updateById(m);
        }

        User operator = userMapper.selectById(operatorId);
        String opName = operator != null ? (operator.getRealName() != null ? operator.getRealName() : operator.getUsername()) : "群主";
        ChatMessage sysMsg = new ChatMessage();
        sysMsg.setConversationId(conversationId);
        sysMsg.setSenderId(0L);
        sysMsg.setContent(opName + " 已关闭全员禁言");
        sysMsg.setMsgType("SYSTEM");
        sysMsg.setCreateTime(LocalDateTime.now());
        messageMapper.insert(sysMsg);
    }

    public void setMemberNickname(Long conversationId, Long operatorId, Long targetUserId, String nickname) {
        ChatConversation conv = conversationMapper.selectById(conversationId);
        if (conv == null || !"GROUP".equals(conv.getType())) {
            throw new RuntimeException("群聊不存在");
        }
        if (!operatorId.equals(conv.getOwnerId())) {
            throw new RuntimeException("只有群主可以设置群昵称");
        }
        ChatConversationMember member = memberMapper.selectOne(
                new LambdaQueryWrapper<ChatConversationMember>()
                        .eq(ChatConversationMember::getConversationId, conversationId)
                        .eq(ChatConversationMember::getUserId, targetUserId));
        if (member == null) {
            throw new RuntimeException("该成员不在群聊中");
        }
        member.setNickname(nickname);
        memberMapper.updateById(member);
    }

    public void muteMember(Long conversationId, Long operatorId, Long targetUserId, Integer durationMinutes) {
        ChatConversation conv = conversationMapper.selectById(conversationId);
        if (conv == null || !"GROUP".equals(conv.getType())) {
            throw new RuntimeException("群聊不存在");
        }
        if (!operatorId.equals(conv.getOwnerId())) {
            throw new RuntimeException("只有群主可以禁言成员");
        }
        if (targetUserId.equals(conv.getOwnerId())) {
            throw new RuntimeException("不能禁言群主");
        }
        ChatConversationMember member = memberMapper.selectOne(
                new LambdaQueryWrapper<ChatConversationMember>()
                        .eq(ChatConversationMember::getConversationId, conversationId)
                        .eq(ChatConversationMember::getUserId, targetUserId));
        if (member == null) {
            throw new RuntimeException("该成员不在群聊中");
        }
        member.setIsMuted(1);
        member.setMutedUntil(durationMinutes != null && durationMinutes > 0
                ? LocalDateTime.now().plusMinutes(durationMinutes) : null);
        memberMapper.updateById(member);

        // 在群里发送系统消息通知
        User operator = userMapper.selectById(operatorId);
        User target = userMapper.selectById(targetUserId);
        String opName = operator != null ? (operator.getRealName() != null ? operator.getRealName() : operator.getUsername()) : "群主";
        String targetName = target != null ? (target.getRealName() != null ? target.getRealName() : target.getUsername()) : "该成员";
        String muteDesc = durationMinutes != null && durationMinutes > 0 ? durationMinutes + "分钟" : "永久";
        String sysContent = opName + " 将 " + targetName + " 禁言" + muteDesc;
        ChatMessage sysMsg = new ChatMessage();
        sysMsg.setConversationId(conversationId);
        sysMsg.setSenderId(0L);
        sysMsg.setContent(sysContent);
        sysMsg.setMsgType("SYSTEM");
        sysMsg.setCreateTime(LocalDateTime.now());
        messageMapper.insert(sysMsg);
    }

    public String getMuteRemaining(Long conversationId, Long userId) {
        ChatConversationMember member = memberMapper.selectOne(
                new LambdaQueryWrapper<ChatConversationMember>()
                        .eq(ChatConversationMember::getConversationId, conversationId)
                        .eq(ChatConversationMember::getUserId, userId));
        if (member == null || member.getIsMuted() == null || member.getIsMuted() != 1) return null;
        if (member.getMutedUntil() == null) return "永久";
        long remaining = Duration.between(LocalDateTime.now(), member.getMutedUntil()).toMinutes();
        if (remaining <= 0) return null;
        if (remaining >= 60) return (remaining / 60) + "小时" + (remaining % 60 > 0 ? (remaining % 60) + "分钟" : "");
        return remaining + "分钟";
    }

    public void unmuteMember(Long conversationId, Long operatorId, Long targetUserId) {
        ChatConversation conv = conversationMapper.selectById(conversationId);
        if (conv == null || !"GROUP".equals(conv.getType())) {
            throw new RuntimeException("群聊不存在");
        }
        if (!operatorId.equals(conv.getOwnerId())) {
            throw new RuntimeException("只有群主可以取消禁言");
        }
        ChatConversationMember member = memberMapper.selectOne(
                new LambdaQueryWrapper<ChatConversationMember>()
                        .eq(ChatConversationMember::getConversationId, conversationId)
                        .eq(ChatConversationMember::getUserId, targetUserId));
        if (member == null) {
            throw new RuntimeException("该成员不在群聊中");
        }
        member.setIsMuted(0);
        member.setMutedUntil(null);
        memberMapper.updateById(member);

        // 在群里发送系统消息通知
        User operator = userMapper.selectById(operatorId);
        User target = userMapper.selectById(targetUserId);
        String opName = operator != null ? (operator.getRealName() != null ? operator.getRealName() : operator.getUsername()) : "群主";
        String targetName = target != null ? (target.getRealName() != null ? target.getRealName() : target.getUsername()) : "该成员";
        ChatMessage sysMsg = new ChatMessage();
        sysMsg.setConversationId(conversationId);
        sysMsg.setSenderId(0L);
        sysMsg.setContent(opName + " 已解除对 " + targetName + " 的禁言");
        sysMsg.setMsgType("SYSTEM");
        sysMsg.setCreateTime(LocalDateTime.now());
        messageMapper.insert(sysMsg);
    }

    public List<Map<String, Object>> getGroupMembersWithDetails(Long conversationId) {
        List<ChatConversationMember> members = memberMapper.selectList(
                new LambdaQueryWrapper<ChatConversationMember>()
                        .eq(ChatConversationMember::getConversationId, conversationId));
        ChatConversation conv = conversationMapper.selectById(conversationId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (ChatConversationMember m : members) {
            User user = userMapper.selectById(m.getUserId());
            if (user == null) continue;
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("userId", user.getId());
            item.put("username", user.getUsername());
            item.put("realName", user.getRealName());
            item.put("role", user.getRole());
            item.put("nickname", m.getNickname());
            item.put("isMuted", m.getIsMuted());
            item.put("mutedUntil", m.getMutedUntil());
            item.put("isOwner", conv != null && conv.getOwnerId() != null && conv.getOwnerId().equals(user.getId()));
            result.add(item);
        }
        return result;
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
            info.put("ownerId", conv.getOwnerId());
            info.put("courseId", conv.getCourseId());
            info.put("createTime", conv.getCreateTime());
            info.put("updateTime", conv.getUpdateTime());

            // 获取成员信息
            List<Long> memberIds = getMemberIds(conv.getId());
            int memberCount = memberIds.size();
            info.put("memberCount", memberCount);

            // 私聊：拼双方姓名作为显示名称
            if ("PRIVATE".equals(conv.getType()) && memberCount >= 2) {
                List<String> names = new ArrayList<>();
                for (Long uid : memberIds) {
                    User u = userMapper.selectById(uid);
                    if (u != null) names.add(u.getRealName() != null && !u.getRealName().isEmpty()
                            ? u.getRealName() : u.getUsername());
                }
                if (names.size() == 2) {
                    info.put("name", names.get(0) + " & " + names.get(1) + " 的私聊");
                } else {
                    info.put("name", String.join("、", names) + " 的私聊");
                }
                info.put("memberNames", names);
            } else {
                info.put("name", conv.getName());
            }

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
