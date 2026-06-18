package com.zhixun.erp.user.dto;

public class ResetPasswordByCodeRequest {
    private String contact;
    private String code;
    private String newPassword;

    public String getContact() { return contact; }
    public void setContact(String contact) { this.contact = contact; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
}
