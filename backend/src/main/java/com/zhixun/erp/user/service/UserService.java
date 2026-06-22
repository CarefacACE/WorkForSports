package com.zhixun.erp.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhixun.erp.user.dto.ChangePasswordRequest;
import com.zhixun.erp.user.dto.LoginRequest;
import com.zhixun.erp.user.dto.RegisterRequest;
import com.zhixun.erp.user.dto.ResetPasswordRequest;
import com.zhixun.erp.user.dto.UpdateProfileRequest;
import com.zhixun.erp.user.entity.User;
import com.zhixun.erp.user.mapper.UserMapper;
import com.zhixun.erp.user.vo.LoginResponse;
import com.zhixun.erp.user.vo.UserProfileResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;

    public User register(RegisterRequest request) {
        if ("ADMIN".equals(request.getRole())) {
            throw new RuntimeException("不允许注册管理员账号");
        }

        User exists = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername()));
        if (exists != null) {
            throw new RuntimeException("用户名已存在");
        }

        if (request.getEmail() != null && !request.getEmail().isEmpty()) {
            User emailExists = userMapper.selectOne(new LambdaQueryWrapper<User>()
                    .eq(User::getEmail, request.getEmail()));
            if (emailExists != null) {
                throw new RuntimeException("该邮箱已被其他用户绑定");
            }
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(request.getPassword());
        user.setRealName(request.getRealName());
        user.setRole(request.getRole());
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setCreateTime(LocalDateTime.now());
        userMapper.insert(user);
        return user;
    }

    public LoginResponse login(LoginRequest request) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername())
                .eq(User::getPassword, request.getPassword())
                .eq(User::getRole, request.getRole()));
        if (user == null) {
            throw new RuntimeException("用户名、密码或角色错误");
        }

        return new LoginResponse(
                user.getId(),
                user.getUsername(),
                user.getRealName(),
                user.getRole(),
                user.getPhone(),
                user.getEmail(),
                user.getGender(),
                user.getBirthday(),
                user.getAvatar(),
                user.getRemark(),
                UUID.randomUUID().toString()
        );
    }

    public void changePassword(ChangePasswordRequest request) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername())
                .eq(User::getRole, request.getRole())
                .eq(User::getPassword, request.getOldPassword()));
        if (user == null) {
            throw new RuntimeException("用户名、角色或原密码错误");
        }

        user.setPassword(request.getNewPassword());
        userMapper.updateById(user);
    }

    public void resetPassword(ResetPasswordRequest request) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername())
                .eq(User::getRole, request.getRole()));
        if (user == null) {
            throw new RuntimeException("用户不存在或身份不匹配");
        }

        user.setPassword(request.getNewPassword());
        userMapper.updateById(user);
    }

    public UserProfileResponse getProfile(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        return toProfileResponse(user);
    }

    public UserProfileResponse updateProfile(UpdateProfileRequest request) {
        User user = userMapper.selectById(request.getId());
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        user.setRealName(request.getRealName());
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setGender(request.getGender());
        user.setBirthday(request.getBirthday());
        user.setAvatar(request.getAvatar());
        user.setRemark(request.getRemark());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
        return toProfileResponse(user);
    }

    private UserProfileResponse toProfileResponse(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getRealName(),
                user.getRole(),
                user.getPhone(),
                user.getEmail(),
                user.getGender(),
                user.getBirthday(),
                user.getAvatar(),
                user.getRemark()
        );
    }

    public IPage<User> listUsers(int pageNum, int pageSize, String keyword, String role) {
        Page<User> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .eq(User::getRole, role);

        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(User::getUsername, kw)
                    .or().like(User::getRealName, kw)
                    .or().like(User::getPhone, kw));
        }

        wrapper.orderByDesc(User::getCreateTime);
        return userMapper.selectPage(page, wrapper);
    }

    public void updateUser(Long id, String username, String password, String role,
                           String realName, String phone, String email,
                           String gender, String birthday, String avatar, String remark) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        if (username != null && !username.equals(user.getUsername())) {
            User exists = userMapper.selectOne(new LambdaQueryWrapper<User>()
                    .eq(User::getUsername, username));
            if (exists != null) {
                throw new RuntimeException("用户名已存在");
            }
            user.setUsername(username);
        }
        if (email != null && !email.equals(user.getEmail())) {
            User emailExists = userMapper.selectOne(new LambdaQueryWrapper<User>()
                    .eq(User::getEmail, email));
            if (emailExists != null) {
                throw new RuntimeException("该邮箱已被其他用户绑定");
            }
            user.setEmail(email);
        }
        if (password != null && !password.isEmpty()) {
            user.setPassword(password);
        }
        if (role != null) {
            user.setRole(role);
        }
        user.setRealName(realName);
        user.setPhone(phone);
        user.setGender(gender);
        user.setBirthday(birthday);
        user.setAvatar(avatar);
        user.setRemark(remark);
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
    }

    public void deleteUser(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        userMapper.deleteById(id);
    }

    public void deleteUsers(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new RuntimeException("请选择要删除的用户");
        }
        userMapper.deleteBatchIds(ids);
    }
}
