package com.zhixun.erp.schedule.controller;

import com.zhixun.erp.common.response.Result;
import com.zhixun.erp.schedule.entity.CourseSchedule;
import com.zhixun.erp.schedule.service.CourseScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@CrossOrigin
@RestController
@RequiredArgsConstructor
@RequestMapping("/schedule")
public class CourseScheduleController {

    private final CourseScheduleService scheduleService;

    @GetMapping("/coach")
    public Result<List<CourseSchedule>> getCoachSchedules(
            @RequestParam Long coachId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return Result.success(scheduleService.getCoachSchedules(coachId, from, to));
    }

    @GetMapping("/member")
    public Result<List<CourseSchedule>> getMemberSchedules(
            @RequestParam Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return Result.success(scheduleService.getMemberSchedules(userId, from, to));
    }

    @PostMapping
    public Result<CourseSchedule> createSchedule(
            @RequestParam Long coachId,
            @RequestBody CourseSchedule schedule) {
        return Result.success("排课成功", scheduleService.createSchedule(coachId, schedule));
    }

    @PutMapping
    public Result<CourseSchedule> updateSchedule(
            @RequestParam Long coachId,
            @RequestBody CourseSchedule schedule) {
        return Result.success("更新成功", scheduleService.updateSchedule(coachId, schedule));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteSchedule(@PathVariable Long id, @RequestParam Long coachId) {
        scheduleService.deleteSchedule(coachId, id);
        return Result.success("删除成功", null);
    }

    @PostMapping("/auto")
    public Result<List<CourseSchedule>> autoSchedule(
            @RequestParam Long coachId,
            @RequestParam Long courseId) {
        return Result.success("自动排课成功", scheduleService.autoSchedule(coachId, courseId));
    }

    @DeleteMapping("/auto")
    public Result<Void> clearAutoSchedule(
            @RequestParam Long coachId,
            @RequestParam Long courseId) {
        scheduleService.clearAutoSchedule(coachId, courseId);
        return Result.success("已清除自动排课", null);
    }

    /* ─── 管理员：查看/编辑/删除所有日程 ─── */

    @GetMapping("/admin/all")
    public Result<List<CourseSchedule>> getAllSchedules(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return Result.success(scheduleService.getAllSchedules(from, to));
    }

    @PutMapping("/admin/update")
    public Result<CourseSchedule> adminUpdateSchedule(@RequestBody CourseSchedule schedule) {
        return Result.success("更新成功", scheduleService.adminUpdateSchedule(schedule));
    }

    @DeleteMapping("/admin/{id}")
    public Result<Void> adminDeleteSchedule(@PathVariable Long id) {
        scheduleService.adminDeleteSchedule(id);
        return Result.success("删除成功", null);
    }
}
