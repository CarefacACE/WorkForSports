package com.zhixun.erp.course.controller;

import com.zhixun.erp.common.response.Result;
import com.zhixun.erp.course.dto.CreateLessonRequest;
import com.zhixun.erp.course.entity.Lesson;
import com.zhixun.erp.course.service.LessonService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@CrossOrigin
@RestController
@RequiredArgsConstructor
@RequestMapping("/lesson")
public class LessonController {

    private final LessonService lessonService;

    @PostMapping
    public Result<Lesson> addLesson(@RequestParam Long coachId, @RequestBody CreateLessonRequest request) {
        return Result.success("课时添加成功", lessonService.addLesson(coachId, request));
    }

    @PutMapping("/{id}")
    public Result<Lesson> updateLesson(
            @PathVariable Long id,
            @RequestParam Long coachId,
            @RequestBody CreateLessonRequest request) {
        return Result.success("课时更新成功", lessonService.updateLesson(coachId, id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteLesson(@PathVariable Long id, @RequestParam Long coachId) {
        lessonService.deleteLesson(coachId, id);
        return Result.success("课时删除成功", null);
    }

    @GetMapping("/list")
    public Result<List<Lesson>> getLessons(@RequestParam Long courseId) {
        return Result.success(lessonService.getLessons(courseId));
    }
}
