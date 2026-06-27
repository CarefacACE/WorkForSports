package com.zhixun.erp.chat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("chat_block")
public class ChatBlock {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long blockedUserId;

    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getBlockedUserId() { return blockedUserId; }
    public void setBlockedUserId(Long blockedUserId) { this.blockedUserId = blockedUserId; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
