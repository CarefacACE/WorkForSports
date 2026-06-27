package com.zhixun.erp.feedback.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhixun.erp.feedback.dto.FeedbackRequest;
import com.zhixun.erp.feedback.dto.SubmitRepairRequest;
import com.zhixun.erp.feedback.entity.EquipmentRepair;
import com.zhixun.erp.feedback.mapper.EquipmentRepairMapper;
import com.zhixun.erp.user.entity.User;
import com.zhixun.erp.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class EquipmentRepairService {

    private final EquipmentRepairMapper repairMapper;
    private final UserMapper userMapper;

    @Transactional
    public EquipmentRepair submitRepair(SubmitRepairRequest request) {
        EquipmentRepair repair = new EquipmentRepair();
        repair.setUserId(request.getUserId());
        repair.setUsername(request.getUsername());
        repair.setEquipmentName(request.getEquipmentName());
        repair.setEquipmentLocation(request.getEquipmentLocation());
        repair.setDescription(request.getDescription());
        repair.setStatus("PENDING");
        repair.setCreateTime(LocalDateTime.now());
        repairMapper.insert(repair);
        return repair;
    }

    public IPage<EquipmentRepair> listRepairs(int pageNum, int pageSize, Long userId, String status) {
        Page<EquipmentRepair> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<EquipmentRepair> wrapper = new LambdaQueryWrapper<EquipmentRepair>()
                .orderByDesc(EquipmentRepair::getCreateTime);

        if (userId != null) {
            wrapper.eq(EquipmentRepair::getUserId, userId);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(EquipmentRepair::getStatus, status);
        }

        return repairMapper.selectPage(page, wrapper);
    }

    public Long countPending() {
        LambdaQueryWrapper<EquipmentRepair> wrapper = new LambdaQueryWrapper<EquipmentRepair>()
                .eq(EquipmentRepair::getStatus, "PENDING");
        return repairMapper.selectCount(wrapper);
    }

    /**
     * 教练查询自己的报修列表
     */
    public IPage<EquipmentRepair> getMyRepairs(Long userId, int pageNum, int pageSize) {
        return listRepairs(pageNum, pageSize, userId, null);
    }

    /**
     * 管理员处理报修（反馈）
     */
    @Transactional
    public EquipmentRepair processRepair(Long id, FeedbackRequest request) {
        EquipmentRepair repair = repairMapper.selectById(id);
        if (repair == null) {
            throw new RuntimeException("报修记录不存在");
        }
        repair.setStatus(request.getStatus());
        repair.setFeedback(request.getFeedback());
        repair.setProcessedTime(LocalDateTime.now());
        repair.setUpdateTime(LocalDateTime.now());
        repairMapper.updateById(repair);
        return repair;
    }
}
