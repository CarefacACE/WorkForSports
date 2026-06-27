package com.zhixun.erp.course.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zhixun.erp.common.response.Result;
import com.zhixun.erp.course.dto.EnrollRequest;
import com.zhixun.erp.course.entity.Enrollment;
import com.zhixun.erp.course.service.EnrollmentService;
import com.zhixun.erp.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin
@RestController
@RequiredArgsConstructor
@RequestMapping("/enrollment")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    @PostMapping
    public Result<Enrollment> enroll(@RequestParam Long userId, @RequestBody EnrollRequest request) {
        return Result.success("选课成功", enrollmentService.enroll(userId, request.getCourseId()));
    }

    @PostMapping("/pay")
    public Result<Enrollment> payCourse(@RequestParam Long userId, @RequestParam Long courseId) {
        return Result.success("付费成功", enrollmentService.payCourse(userId, courseId));
    }

    @PutMapping("/confirm/{id}")
    public Result<Enrollment> confirmEnrollment(@PathVariable Long id, @RequestParam Long coachId) {
        return Result.success("确认成功", enrollmentService.confirmEnrollment(coachId, id));
    }

    @GetMapping("/my")
    public Result<IPage<Enrollment>> getMyEnrollments(
            @RequestParam Long userId,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(enrollmentService.getMyEnrollments(userId, type, pageNum, pageSize));
    }

    @GetMapping("/students")
    public Result<IPage<User>> getCourseStudents(
            @RequestParam Long coachId,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(enrollmentService.getCourseStudents(coachId, courseId, keyword, pageNum, pageSize));
    }

    @GetMapping("/coach")
    public Result<IPage<Enrollment>> getCoachEnrollments(
            @RequestParam Long coachId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(enrollmentService.getCoachEnrollments(coachId, keyword, pageNum, pageSize));
    }

    /* ─── 退出课程 (公共课/私教通用) ─── */

    @PutMapping("/quit/{enrollmentId}")
    public Result<Void> quitEnrollment(@PathVariable Long enrollmentId) {
        enrollmentService.quitEnrollment(enrollmentId);
        return Result.success("已退出课程", null);
    }
}
