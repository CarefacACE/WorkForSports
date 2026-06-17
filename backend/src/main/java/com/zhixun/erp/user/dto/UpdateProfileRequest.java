package com.zhixun.erp.user.dto;

import lombok.Data;

@Data
public class UpdateProfileRequest {

    private Long id;

    private String realName;

    private String phone;

    private String email;

    private String gender;

    private String birthday;

    private String avatar;

    private String remark;
}
