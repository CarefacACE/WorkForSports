package com.zhixun.erp.feedback.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zhixun.erp.common.response.Result;
import com.zhixun.erp.feedback.dto.FeedbackRequest;
import com.zhixun.erp.feedback.dto.SubmitComplaintRequest;
import com.zhixun.erp.feedback.entity.CoachComplaint;
import com.zhixun.erp.feedback.service.CoachComplaintService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/feedback")
public class ComplaintController {

    private final CoachComplaintService complaintService;

    /** 会员提交投诉 */
    @PostMapping("/complaint")
    public Result<CoachComplaint> submitComplaint(@RequestBody SubmitComplaintRequest request) {
        return Result.success("投诉提交成功", complaintService.submitComplaint(request));
    }

    /** 会员查看自己的投诉列表 */
    @GetMapping("/complaint/my")
    public Result<IPage<CoachComplaint>> getMyComplaints(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(complaintService.getMyComplaints(userId, pageNum, pageSize));
    }

    /** 管理员查看所有投诉 */
    @GetMapping("/complaint/list")
    public Result<IPage<CoachComplaint>> listComplaints(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String status) {
        return Result.success(complaintService.listComplaints(pageNum, pageSize, null, null, status));
    }

    /** 管理员获取待处理投诉数量 */
    @GetMapping("/complaint/pending-count")
    public Result<Long> countPendingComplaints() {
        return Result.success(complaintService.countPending());
    }

    /** 管理员处理投诉 */
    @PutMapping("/complaint/{id}/process")
    public Result<CoachComplaint> processComplaint(@PathVariable Long id, @RequestBody FeedbackRequest request) {
        return Result.success("处理成功", complaintService.processComplaint(id, request));
    }
}
