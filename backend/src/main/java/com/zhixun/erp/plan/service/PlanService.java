package com.zhixun.erp.plan.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhixun.erp.plan.dto.PlanSubmitRequest;
import com.zhixun.erp.plan.entity.PlanDetail;
import com.zhixun.erp.plan.entity.TrainingPlan;
import com.zhixun.erp.plan.mapper.PlanDetailMapper;
import com.zhixun.erp.plan.mapper.TrainingPlanMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PlanService {

    private final TrainingPlanMapper trainingPlanMapper;
    private final PlanDetailMapper planDetailMapper;

    /**
     * 提交训练计划：先保存主表获取主键，再批量保存明细
     */
    @Transactional
    public TrainingPlan submitPlan(Long userId, PlanSubmitRequest request) {
        if (request.getDetails() == null || request.getDetails().isEmpty()) {
            throw new RuntimeException("训练计划明细不能为空");
        }
        if (request.getGoal() == null || request.getGoal().isBlank()) {
            throw new RuntimeException("训练目标不能为空");
        }

        // 1. 构建并插入主表
        TrainingPlan plan = new TrainingPlan();
        plan.setUserId(userId);
        plan.setGoal(request.getGoal());
        plan.setDurationDays(request.getDurationDays());
        plan.setDescription(request.getDescription());
        plan.setStatus("ACTIVE");

        if (request.getStartDate() != null && !request.getStartDate().isBlank()) {
            plan.setStartDate(LocalDate.parse(request.getStartDate()));
        }
        if (request.getEndDate() != null && !request.getEndDate().isBlank()) {
            plan.setEndDate(LocalDate.parse(request.getEndDate()));
        }
        plan.setCreateTime(LocalDateTime.now());
        trainingPlanMapper.insert(plan);
        // insert 后 plan.getId() 已自动回填

        // 2. 批量插入明细
        LocalDateTime now = LocalDateTime.now();
        for (PlanSubmitRequest.DetailItem item : request.getDetails()) {
            PlanDetail detail = new PlanDetail();
            detail.setPlanId(plan.getId());
            detail.setDayNumber(item.getDayNumber());
            detail.setTrainingType(item.getTrainingType() != null ? item.getTrainingType() : "");
            detail.setContent(item.getContent() != null ? item.getContent() : "");
            detail.setDurationMinutes(item.getDurationMinutes() != null ? item.getDurationMinutes() : 0);
            detail.setIntensity(item.getIntensity() != null ? item.getIntensity() : "MEDIUM");
            detail.setIsChecked(0);
            detail.setCreateTime(now);
            planDetailMapper.insert(detail);
        }

        return plan;
    }

    /**
     * 根据计划ID查询明细列表（按天数排序）
     */
    public List<PlanDetail> getPlanDetails(Long planId) {
        return planDetailMapper.selectList(
                new LambdaQueryWrapper<PlanDetail>()
                        .eq(PlanDetail::getPlanId, planId)
                        .orderByAsc(PlanDetail::getDayNumber));
    }

    /**
     * 获取用户所有训练计划（按创建时间倒序）
     */
    public List<TrainingPlan> getMyPlans(Long userId) {
        return trainingPlanMapper.selectList(
                new LambdaQueryWrapper<TrainingPlan>()
                        .eq(TrainingPlan::getUserId, userId)
                        .orderByDesc(TrainingPlan::getCreateTime));
    }

    /**
     * 打卡：标记某天训练已完成
     */
    @Transactional
    public PlanDetail checkInDetail(Long planId, Long detailId) {
        PlanDetail detail = planDetailMapper.selectById(detailId);
        if (detail == null) {
            throw new RuntimeException("训练明细不存在");
        }
        if (!detail.getPlanId().equals(planId)) {
            throw new RuntimeException("训练明细与计划不匹配");
        }
        if (detail.getIsChecked() == 1) {
            throw new RuntimeException("该训练已打卡，请勿重复操作");
        }
        detail.setIsChecked(1);
        detail.setCheckTime(LocalDateTime.now());
        detail.setUpdateTime(LocalDateTime.now());
        planDetailMapper.updateById(detail);
        return detail;
    }

    /**
     * 删除训练计划（级联删除明细）
     */
    @Transactional
    public void deletePlan(Long planId, Long userId) {
        TrainingPlan plan = trainingPlanMapper.selectById(planId);
        if (plan == null) {
            throw new RuntimeException("训练计划不存在");
        }
        if (!plan.getUserId().equals(userId)) {
            throw new RuntimeException("无权删除他人计划");
        }
        // 删除主表（逻辑删除）
        trainingPlanMapper.deleteById(planId);
        // 级联删除明细
        List<PlanDetail> details = planDetailMapper.selectList(
                new LambdaQueryWrapper<PlanDetail>()
                        .eq(PlanDetail::getPlanId, planId));
        for (PlanDetail detail : details) {
            planDetailMapper.deleteById(detail.getId());
        }
    }

    /**
     * 修改训练计划名称
     */
    public TrainingPlan updatePlanGoal(Long planId, Long userId, String goal) {
        if (goal == null || goal.isBlank()) {
            throw new RuntimeException("训练目标不能为空");
        }
        TrainingPlan plan = trainingPlanMapper.selectById(planId);
        if (plan == null) {
            throw new RuntimeException("训练计划不存在");
        }
        if (!plan.getUserId().equals(userId)) {
            throw new RuntimeException("无权修改他人计划");
        }
        plan.setGoal(goal.trim());
        plan.setUpdateTime(LocalDateTime.now());
        trainingPlanMapper.updateById(plan);
        return plan;
    }
}
