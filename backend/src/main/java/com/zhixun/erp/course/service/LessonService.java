package com.zhixun.erp.course.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhixun.erp.course.dto.CreateLessonRequest;
import com.zhixun.erp.course.entity.Course;
import com.zhixun.erp.course.entity.Lesson;
import com.zhixun.erp.course.mapper.CourseMapper;
import com.zhixun.erp.course.mapper.LessonMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LessonService {

    private final LessonMapper lessonMapper;
    private final CourseMapper courseMapper;

    @Transactional
    public Lesson addLesson(Long coachId, CreateLessonRequest request) {
        Course course = courseMapper.selectById(request.getCourseId());
        if (course == null) {
            throw new RuntimeException("课程不存在");
        }
        if (!course.getCoachId().equals(coachId)) {
            throw new RuntimeException("只能为自己的课程添加课时");
        }

        Lesson lesson = new Lesson();
        lesson.setCourseId(request.getCourseId());
        lesson.setTitle(request.getTitle());
        lesson.setVideoUrl(request.getVideoUrl());
        lesson.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        lesson.setIsTrial(request.getIsTrial() == null ? 0 : request.getIsTrial());
        lesson.setCreateTime(LocalDateTime.now());
        lessonMapper.insert(lesson);
        return lesson;
    }

    @Transactional
    public Lesson updateLesson(Long coachId, Long lessonId, String title, String videoUrl, Integer sortOrder, Integer isTrial) {
        Lesson lesson = lessonMapper.selectById(lessonId);
        if (lesson == null) {
            throw new RuntimeException("课时不存在");
        }

        Course course = courseMapper.selectById(lesson.getCourseId());
        if (course == null || !course.getCoachId().equals(coachId)) {
            throw new RuntimeException("只能修改自己课程的课时");
        }

        if (title != null) lesson.setTitle(title);
        if (videoUrl != null) lesson.setVideoUrl(videoUrl);
        if (sortOrder != null) lesson.setSortOrder(sortOrder);
        if (isTrial != null) lesson.setIsTrial(isTrial);
        lesson.setUpdateTime(LocalDateTime.now());
        lessonMapper.updateById(lesson);
        return lesson;
    }

    @Transactional
    public void deleteLesson(Long coachId, Long lessonId) {
        Lesson lesson = lessonMapper.selectById(lessonId);
        if (lesson == null) {
            throw new RuntimeException("课时不存在");
        }

        Course course = courseMapper.selectById(lesson.getCourseId());
        if (course == null || !course.getCoachId().equals(coachId)) {
            throw new RuntimeException("只能删除自己课程的课时");
        }

        lessonMapper.deleteById(lessonId);
    }

    public List<Lesson> getLessons(Long courseId) {
        return lessonMapper.selectList(new LambdaQueryWrapper<Lesson>()
                .eq(Lesson::getCourseId, courseId)
                .orderByAsc(Lesson::getSortOrder));
    }
}
