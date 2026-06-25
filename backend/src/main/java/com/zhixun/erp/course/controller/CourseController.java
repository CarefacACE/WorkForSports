package com.zhixun.erp.course.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zhixun.erp.common.response.Result;
import com.zhixun.erp.course.dto.CreateCourseRequest;
import com.zhixun.erp.course.dto.UpdateCourseRequest;
import com.zhixun.erp.course.entity.Course;
import com.zhixun.erp.course.service.CourseService;
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

import java.math.BigDecimal;
import java.util.List;

@CrossOrigin
@RestController
@RequiredArgsConstructor
@RequestMapping("/course")
public class CourseController {

    private final CourseService courseService;

    @PostMapping
    public Result<Course> createCourse(@RequestParam Long coachId, @RequestBody CreateCourseRequest request) {
        return Result.success("课程创建成功", courseService.createCourse(coachId, request));
    }

    @PutMapping
    public Result<Course> updateCourse(@RequestParam Long coachId, @RequestBody UpdateCourseRequest request) {
        return Result.success("课程更新成功", courseService.updateCourse(coachId, request));
    }

    @PutMapping("/price")
    public Result<Course> updatePrice(@RequestParam Long coachId, @RequestParam Long courseId, @RequestParam BigDecimal newPrice) {
        return Result.success("价格更新成功", courseService.updateCoursePrice(coachId, courseId, newPrice));
    }

    @GetMapping("/list")
    public Result<IPage<Course>> listCourses(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(courseService.listCourses(type, keyword, pageNum, pageSize));
    }

    @GetMapping("/{id}")
    public Result<Course> getCourseDetail(@PathVariable Long id) {
        return Result.success(courseService.getCourseDetail(id));
    }

    @GetMapping("/my")
    public Result<IPage<Course>> getMyCourses(
            @RequestParam Long coachId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(courseService.getCoachCourses(coachId, pageNum, pageSize));
    }

    /* ─── 管理员：课程审批 ─── */

    @GetMapping("/admin/pending")
    public Result<List<Course>> getPendingCourses() {
        return Result.success(courseService.getPendingCourses());
    }

    @GetMapping("/admin/all")
    public Result<List<Course>> getAllCourses(
            @RequestParam(required = false) String status) {
        return Result.success(courseService.getAllCoursesByStatus(status));
    }

    @PutMapping("/admin/approve/{id}")
    public Result<Course> approveCourse(@PathVariable Long id) {
        return Result.success("审批通过", courseService.approveCourse(id));
    }

    @PutMapping("/admin/reject/{id}")
    public Result<Course> rejectCourse(
            @PathVariable Long id,
            @RequestParam(required = false) String reason) {
        return Result.success("已驳回", courseService.rejectCourse(id, reason));
    }
}
