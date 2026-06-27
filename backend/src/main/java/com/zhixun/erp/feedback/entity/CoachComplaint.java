package com.zhixun.erp.feedback.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 会员投诉实体
 */
@TableName("coach_complaint")
public class CoachComplaint {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 提交投诉的会员ID */
    private Long userId;

    /** 会员用户名 */
    private String username;

    /** 被投诉的教练ID */
    private Long coachId;

    /** 教练用户名 */
    private String coachUsername;

    /** 投诉内容 */
    private String content;

    /** 状态：PENDING（待处理）、PROCESSING（处理中）、RESOLVED（已解决）、REJECTED（已驳回） */
    private String status;

    /** 管理员反馈/备注 */
    private String feedback;

    /** 处理时间 */
    private LocalDateTime processedTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public Long getCoachId() { return coachId; }
    public void setCoachId(Long coachId) { this.coachId = coachId; }

    public String getCoachUsername() { return coachUsername; }
    public void setCoachUsername(String coachUsername) { this.coachUsername = coachUsername; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }

    public LocalDateTime getProcessedTime() { return processedTime; }
    public void setProcessedTime(LocalDateTime processedTime) { this.processedTime = processedTime; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }

    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
