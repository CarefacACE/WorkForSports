package com.zhixun.erp.user.dto;

import lombok.Data;

@Data
public class ResetPasswordRequest {

    private String username;

    private String role;

    private String newPassword;
}
