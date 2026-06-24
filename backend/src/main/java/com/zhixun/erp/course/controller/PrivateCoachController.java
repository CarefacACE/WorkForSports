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

    @PostMapping("/purchase")
    public Result<Enrollment> purchase(
            @RequestParam Long userId,
            @RequestParam Long coachId,
            @RequestParam Integer sessions) {
        return Result.success("购买成功", privateCoachService.purchaseSessions(userId, coachId, sessions));
    }

    @GetMapping("/my-coaches")
    public Result<List<Map<String, Object>>> getMyCoaches(@RequestParam Long userId) {
        return Result.success(privateCoachService.getMyCoaches(userId));
    }

    @PostMapping("/book-session")
    public Result<Void> bookSession(
            @RequestParam Long userId,
            @RequestParam Long scheduleId) {
        privateCoachService.bookSession(userId, scheduleId);
        return Result.success("预约成功", null);
    }

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

    @DeleteMapping("/cancel-booking/{scheduleId}")
    public Result<Void> cancelBooking(
            @RequestParam Long userId,
            @PathVariable Long scheduleId) {
        privateCoachService.cancelBooking(userId, scheduleId);
        return Result.success("取消成功", null);
    }

    @PutMapping("/reschedule")
    public Result<Void> reschedule(
            @RequestParam Long userId,
            @RequestParam Long currentScheduleId,
            @RequestParam String targetStart,
            @RequestParam String targetEnd) {
        LocalDateTime start = LocalDateTime.parse(targetStart);
        LocalDateTime end = LocalDateTime.parse(targetEnd);
        privateCoachService.rescheduleBooking(userId, currentScheduleId, start, end);
        return Result.success("改期成功", null);
    }
}
