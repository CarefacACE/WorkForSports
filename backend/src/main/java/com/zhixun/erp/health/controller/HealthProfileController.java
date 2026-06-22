package com.zhixun.erp.health.controller;

import com.zhixun.erp.common.response.Result;
import com.zhixun.erp.health.entity.MemberHealthProfile;
import com.zhixun.erp.health.service.HealthProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@CrossOrigin
@RestController
@RequiredArgsConstructor
@RequestMapping("/health")
public class HealthProfileController {

    private final HealthProfileService healthProfileService;

    @GetMapping("/profile")
    public Result<MemberHealthProfile> getProfile(@RequestParam Long userId) {
        MemberHealthProfile profile = healthProfileService.getByUserId(userId);
        return Result.success(profile);
    }

    @PutMapping("/profile")
    public Result<MemberHealthProfile> saveProfile(
            @RequestParam Long userId,
            @RequestBody MemberHealthProfile profile) {
        MemberHealthProfile saved = healthProfileService.saveOrUpdate(userId, profile);
        return Result.success("保存成功", saved);
    }
}
