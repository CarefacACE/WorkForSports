package com.zhixun.erp.feedback.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zhixun.erp.common.response.Result;
import com.zhixun.erp.feedback.dto.FeedbackRequest;
import com.zhixun.erp.feedback.dto.SubmitRepairRequest;
import com.zhixun.erp.feedback.entity.EquipmentRepair;
import com.zhixun.erp.feedback.service.EquipmentRepairService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/feedback")
public class RepairController {

    private final EquipmentRepairService repairService;

    /** 教练提交报修 */
    @PostMapping("/repair")
    public Result<EquipmentRepair> submitRepair(@RequestBody SubmitRepairRequest request) {
        return Result.success("报修提交成功", repairService.submitRepair(request));
    }

    /** 教练查看自己的报修列表 */
    @GetMapping("/repair/my")
    public Result<IPage<EquipmentRepair>> getMyRepairs(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(repairService.getMyRepairs(userId, pageNum, pageSize));
    }

    /** 管理员查看所有报修 */
    @GetMapping("/repair/list")
    public Result<IPage<EquipmentRepair>> listRepairs(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String status) {
        return Result.success(repairService.listRepairs(pageNum, pageSize, null, status));
    }

    /** 管理员获取待处理报修数量 */
    @GetMapping("/repair/pending-count")
    public Result<Long> countPendingRepairs() {
        return Result.success(repairService.countPending());
    }

    /** 管理员处理报修 */
    @PutMapping("/repair/{id}/process")
    public Result<EquipmentRepair> processRepair(@PathVariable Long id, @RequestBody FeedbackRequest request) {
        return Result.success("处理成功", repairService.processRepair(id, request));
    }
}
