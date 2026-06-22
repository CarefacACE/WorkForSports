package com.zhixun.erp.user.vo;

public class UserProfileResponse {

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

    public UserProfileResponse(Long id, String username, String realName, String role,
                               String phone, String email, String gender, String birthday,
                               String avatar, String remark) {
        this.id = id;
        this.username = username;
        this.realName = realName;
        this.role = role;
        this.phone = phone;
        this.email = email;
        this.gender = gender;
        this.birthday = birthday;
        this.avatar = avatar;
        this.remark = remark;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getRealName() { return realName; }
    public void setRealName(String realName) { this.realName = realName; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getBirthday() { return birthday; }
    public void setBirthday(String birthday) { this.birthday = birthday; }

    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
