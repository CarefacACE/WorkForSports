package com.zhixun.erp.user.dto;

import lombok.Data;

@Data
public class ChangePasswordRequest {

    private String username;

    private String role;

    private String oldPassword;

    private String newPassword;
}
