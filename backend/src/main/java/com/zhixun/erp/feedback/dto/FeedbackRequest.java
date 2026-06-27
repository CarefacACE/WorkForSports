package com.zhixun.erp.feedback.dto;

/**
 * 管理员反馈请求（处理报修/投诉）
 */
public class FeedbackRequest {

    private String status;
    private String feedback;

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }
}
