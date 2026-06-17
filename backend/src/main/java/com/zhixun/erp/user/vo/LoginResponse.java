package com.zhixun.erp.user.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponse {

    private Long id;

    private String username;

    private String realName;

    private String role;

    private String phone;

    private String email;

    private String gender;

    private String birthday;

    private String avatar;

    private String remark;

    private String token;
}
