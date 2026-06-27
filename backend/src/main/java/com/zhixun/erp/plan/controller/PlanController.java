package com.zhixun.erp.plan.controller;

import com.zhixun.erp.common.response.Result;
import com.zhixun.erp.plan.dto.PlanSubmitRequest;
import com.zhixun.erp.plan.entity.PlanDetail;
import com.zhixun.erp.plan.entity.TrainingPlan;
import com.zhixun.erp.plan.service.PlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin
@RestController
@RequiredArgsConstructor
@RequestMapping("/plan")
public class PlanController {

    private final PlanService planService;

    /**
     * 提交训练计划（JSON body + userId 参数）
     */
    @PostMapping("/submit")
    public Result<TrainingPlan> submitPlan(
            @RequestParam Long userId,
            @RequestBody PlanSubmitRequest request) {
        return Result.success("计划提交成功", planService.submitPlan(userId, request));
    }

    /**
     * 获取计划明细列表（按天数排序）
     */
    @GetMapping("/{planId}/details")
    public Result<List<PlanDetail>> getPlanDetails(@PathVariable Long planId) {
        return Result.success(planService.getPlanDetails(planId));
    }

    /**
     * 获取用户所有训练计划
     */
    @GetMapping("/my-plans")
    public Result<List<TrainingPlan>> getMyPlans(@RequestParam Long userId) {
        return Result.success(planService.getMyPlans(userId));
    }

    /**
     * 打卡：标记某天训练已完成
     */
    @PostMapping("/{planId}/checkin/{detailId}")
    public Result<PlanDetail> checkInDetail(
            @PathVariable Long planId,
            @PathVariable Long detailId) {
        return Result.success("打卡成功", planService.checkInDetail(planId, detailId));
    }

    /**
     * 删除训练计划（级联删除明细）
     */
    @DeleteMapping("/{planId}")
    public Result<Void> deletePlan(
            @PathVariable Long planId,
            @RequestParam Long userId) {
        planService.deletePlan(planId, userId);
        return Result.success("删除成功", null);
    }

    /**
     * 修改训练计划名称
     */
    @PutMapping("/{planId}/goal")
    public Result<TrainingPlan> updatePlanGoal(
            @PathVariable Long planId,
            @RequestParam Long userId,
            @RequestBody java.util.Map<String, String> body) {
        String goal = body.get("goal");
        return Result.success("修改成功", planService.updatePlanGoal(planId, userId, goal));
    }
}
