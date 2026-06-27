package com.zhixun.erp.feedback.dto;

/**
 * 提交投诉请求
 */
public class SubmitComplaintRequest {

    private Long userId;
    private String username;
    private Long coachId;
    private String coachUsername;
    private String content;

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
}
