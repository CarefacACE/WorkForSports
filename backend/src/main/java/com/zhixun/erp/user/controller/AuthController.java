package com.zhixun.erp.user.controller;

import com.zhixun.erp.common.response.Result;
import com.zhixun.erp.user.dto.ChangePasswordRequest;
import com.zhixun.erp.user.dto.LoginRequest;
import com.zhixun.erp.user.dto.RegisterRequest;
import com.zhixun.erp.user.dto.ResetPasswordRequest;
import com.zhixun.erp.user.dto.ResetPasswordByCodeRequest;
import com.zhixun.erp.user.dto.SendCodeRequest;
import com.zhixun.erp.user.dto.UpdateProfileRequest;
import com.zhixun.erp.user.entity.User;
import com.zhixun.erp.user.service.EmailService;
import com.zhixun.erp.user.service.UserService;
import com.zhixun.erp.user.vo.LoginResponse;
import com.zhixun.erp.user.vo.UserProfileResponse;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhixun.erp.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;
    private final EmailService emailService;
    private final UserMapper userMapper;

    @PostMapping("/register")
    public Result<User> register(@RequestBody RegisterRequest request) {
        return Result.success("注册成功", userService.register(request));
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody LoginRequest request) {
        return Result.success("登录成功", userService.login(request));
    }

    @PostMapping("/change-password")
    public Result<Void> changePassword(@RequestBody ChangePasswordRequest request) {
        userService.changePassword(request);
        return Result.success("密码修改成功", null);
    }

    @PostMapping("/reset-password")
    public Result<Void> resetPassword(@RequestBody ResetPasswordRequest request) {
        userService.resetPassword(request);
        return Result.success("密码重置成功", null);
    }

    @PostMapping("/send-code")
    public Result<Void> sendCode(@RequestBody SendCodeRequest request) {
        String contact = request.getContact();
        if (contact == null || contact.trim().isEmpty()) {
            throw new RuntimeException("请输入邮箱");
        }
        final String email = contact.trim();
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getEmail, email));
        if (user == null) {
            throw new RuntimeException("该邮箱未注册");
        }
        emailService.sendCode(email);
        return Result.success("验证码已发送到邮箱", null);
    }

    @PostMapping("/reset-password-by-code")
    public Result<Void> resetPasswordByCode(@RequestBody ResetPasswordByCodeRequest request) {
        if (request.getContact() == null || request.getContact().trim().isEmpty()) {
            throw new RuntimeException("请输入邮箱");
        }
        if (request.getCode() == null || request.getCode().trim().isEmpty()) {
            throw new RuntimeException("请输入验证码");
        }
        if (request.getNewPassword() == null || request.getNewPassword().isEmpty()) {
            throw new RuntimeException("请输入新密码");
        }
        String email = request.getContact().trim();
        String code = request.getCode().trim();
        if (!emailService.verifyCode(email, code)) {
            throw new RuntimeException("验证码错误或已过期");
        }
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getEmail, email));
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        user.setPassword(request.getNewPassword());
        userMapper.updateById(user);
        return Result.success("密码重置成功", null);
    }

    @GetMapping("/profile/{id}")
    public Result<UserProfileResponse> getProfile(@PathVariable Long id) {
        return Result.success(userService.getProfile(id));
    }

    @PostMapping("/profile")
    public Result<UserProfileResponse> updateProfile(@RequestBody UpdateProfileRequest request) {
        return Result.success("个人信息保存成功", userService.updateProfile(request));
    }
}
