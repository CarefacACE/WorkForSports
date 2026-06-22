package com.zhixun.erp.health.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhixun.erp.health.entity.MemberHealthProfile;
import com.zhixun.erp.health.mapper.MemberHealthProfileMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class HealthProfileService {

    private final MemberHealthProfileMapper healthProfileMapper;

    public MemberHealthProfile getByUserId(Long userId) {
        return healthProfileMapper.selectOne(
                new LambdaQueryWrapper<MemberHealthProfile>()
                        .eq(MemberHealthProfile::getUserId, userId)
        );
    }

    public MemberHealthProfile saveOrUpdate(Long userId, MemberHealthProfile input) {
        MemberHealthProfile existing = getByUserId(userId);

        if (existing == null) {
            input.setUserId(userId);
            input.setCreateTime(LocalDateTime.now());
            healthProfileMapper.insert(input);
            return input;
        }

        input.setId(existing.getId());
        input.setUserId(userId);
        input.setCreateTime(existing.getCreateTime());
        input.setUpdateTime(LocalDateTime.now());
        healthProfileMapper.updateById(input);
        return input;
    }
}
