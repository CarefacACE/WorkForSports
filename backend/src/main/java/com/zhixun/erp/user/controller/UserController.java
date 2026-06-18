package com.zhixun.erp.user.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zhixun.erp.common.response.Result;
import com.zhixun.erp.user.dto.RegisterRequest;
import com.zhixun.erp.user.dto.UpdateUserRequest;
import com.zhixun.erp.user.entity.User;
import com.zhixun.erp.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/user")
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    public Result<User> registerUser(@RequestBody RegisterRequest request) {
        return Result.success("注册成功", userService.register(request));
    }

    @GetMapping("/list")
    public Result<IPage<User>> listUsers(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam String role) {
        IPage<User> page = userService.listUsers(pageNum, pageSize, keyword, role);
        return Result.success(page);
    }

    @PutMapping
    public Result<Void> updateUser(@RequestBody UpdateUserRequest request) {
        userService.updateUser(request.getId(), request.getUsername(), request.getPassword(),
                request.getRole(), request.getRealName(), request.getPhone(),
                request.getEmail(), request.getGender(), request.getBirthday(),
                request.getAvatar(), request.getRemark());
        return Result.success("更新成功", null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return Result.success("删除成功", null);
    }

    @DeleteMapping("/batch")
    public Result<Void> deleteUsers(@RequestBody List<Long> ids) {
        userService.deleteUsers(ids);
        return Result.success("批量删除成功", null);
    }
}
