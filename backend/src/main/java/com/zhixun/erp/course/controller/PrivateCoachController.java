package com.zhixun.erp.course.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zhixun.erp.common.response.Result;
import com.zhixun.erp.course.entity.Enrollment;
import com.zhixun.erp.course.entity.PrivateCoachProfile;
import com.zhixun.erp.course.service.PrivateCoachService;
import com.zhixun.erp.schedule.entity.CourseSchedule;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@CrossOrigin
@RestController
@RequiredArgsConstructor
@RequestMapping("/private-coach")
public class PrivateCoachController {

    private final PrivateCoachService privateCoachService;

    @GetMapping("/list")
    public Result<IPage<Map<String, Object>>> listCoaches(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "12") int pageSize) {
        return Result.success(privateCoachService.listCoaches(keyword, pageNum, pageSize));
    }

    @GetMapping("/{coachId}")
    public Result<Map<String, Object>> getCoachDetail(@PathVariable Long coachId) {
        return Result.success(privateCoachService.getCoachDetail(coachId));
    }

    @GetMapping("/profile")
    public Result<PrivateCoachProfile> getMyProfile(@RequestParam Long coachId) {
        PrivateCoachProfile profile = privateCoachService.getCoachProfile(coachId);
        return Result.success(profile);
    }

    @PutMapping("/profile")
    public Result<PrivateCoachProfile> saveMyProfile(
            @RequestParam Long coachId,
            @RequestBody PrivateCoachProfile profile) {
        return Result.success("保存成功", privateCoachService.saveCoachProfile(coachId, profile));
    }

    /* ─── New: Enroll (join coach, no upfront payment) ─── */

    @PostMapping("/enroll")
    public Result<Enrollment> enrollCoach(
            @RequestParam Long userId,
            @RequestParam Long coachId,
            @RequestParam(defaultValue = "0") Integer autoDeductAgreed) {
        return Result.success("加入成功", privateCoachService.enrollCoach(userId, coachId, autoDeductAgreed));
    }

    /* ─── New: Request session (member → coach approval) ─── */

    @PostMapping("/request-session")
    public Result<CourseSchedule> requestSession(
            @RequestParam Long userId,
            @RequestParam Long coachId,
            @RequestParam String startTime,
            @RequestParam String endTime) {
        LocalDateTime start = LocalDateTime.parse(startTime);
        LocalDateTime end = LocalDateTime.parse(endTime);
        return Result.success("已发送预约请求，等待教练确认",
                privateCoachService.requestSession(userId, coachId, start, end));
    }

    /* ─── New: Approve session ─── */

    @PutMapping("/approve-session/{scheduleId}")
    public Result<Void> approveSession(
            @RequestParam Long coachId,
            @PathVariable Long scheduleId) {
        privateCoachService.approveSession(coachId, scheduleId);
        return Result.success("已通过预约", null);
    }

    /* ─── New: Reject session ─── */

    @PutMapping("/reject-session/{scheduleId}")
    public Result<Void> rejectSession(
            @RequestParam Long coachId,
            @PathVariable Long scheduleId,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.get("reason") : null;
        privateCoachService.rejectSession(coachId, scheduleId, reason);
        return Result.success("已拒绝预约", null);
    }

    /* ─── New: Quit coach ─── */

    @DeleteMapping("/quit/{coachId}")
    public Result<Void> quitCoach(
            @RequestParam Long userId,
            @PathVariable Long coachId) {
        privateCoachService.quitCoach(userId, coachId);
        return Result.success("已退出私教课程", null);
    }

    /* ─── My Coaches ─── */

    @GetMapping("/my-coaches")
    public Result<List<Map<String, Object>>> getMyCoaches(@RequestParam Long userId) {
        return Result.success(privateCoachService.getMyCoaches(userId));
    }

    /* ─── Book Direct (member clicks empty slot → directly book) ─── */

    @PostMapping("/book-direct")
    public Result<CourseSchedule> bookDirect(
            @RequestParam Long userId,
            @RequestParam Long coachId,
            @RequestParam String startTime,
            @RequestParam String endTime) {
        LocalDateTime start = LocalDateTime.parse(startTime);
        LocalDateTime end = LocalDateTime.parse(endTime);
        return Result.success("预约成功", privateCoachService.bookDirect(userId, coachId, start, end));
    }

    /* ─── Cancel Booking ─── */

    @DeleteMapping("/cancel-booking/{scheduleId}")
    public Result<Void> cancelBooking(
            @RequestParam Long userId,
            @PathVariable Long scheduleId) {
        privateCoachService.cancelBooking(userId, scheduleId);
        return Result.success("取消成功", null);
    }
}
