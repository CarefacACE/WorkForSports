package com.zhixun.erp.agent.controller;

import com.zhixun.erp.agent.dto.PlanGenerateRequest;
import com.zhixun.erp.agent.service.PlanGeneratorService;
import com.zhixun.erp.common.response.Result;
import com.zhixun.erp.plan.entity.TrainingPlan;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@CrossOrigin
@RestController
@RequiredArgsConstructor
@RequestMapping("/agent")
public class PlanGeneratorController {

    private final PlanGeneratorService planGeneratorService;

    /**
     * 根据用户健康档案和已报课程，AI生成训练计划并自动提交
     */
    @PostMapping("/generate-plan")
    public Result<TrainingPlan> generatePlan(
            @RequestParam Long userId,
            @RequestBody(required = false) PlanGenerateRequest request) {
        return Result.success("训练计划已生成",
                planGeneratorService.generateAndSubmitPlan(userId, request));
    }
}
