package com.zhixun.erp.course.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhixun.erp.course.dto.CreateCourseRequest;
import com.zhixun.erp.course.dto.UpdateCourseRequest;
import com.zhixun.erp.course.entity.Course;
import com.zhixun.erp.course.mapper.CourseMapper;
import com.zhixun.erp.user.entity.User;
import com.zhixun.erp.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseMapper courseMapper;
    private final UserMapper userMapper;

    @Transactional
    public Course createCourse(Long coachId, CreateCourseRequest request) {
        User coach = userMapper.selectById(coachId);
        if (coach == null || !"COACH".equals(coach.getRole())) {
            throw new RuntimeException("只有教练可以创建课程");
        }

        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new RuntimeException("课程名称不能为空");
        }

        Course course = new Course();
        course.setCoachId(coachId);
        course.setName(request.getName().trim());
        course.setDescription(request.getDescription());
        course.setType(request.getType());
        course.setPrice(request.getPrice() == null ? BigDecimal.ZERO : request.getPrice());
        course.setCoverImage(request.getCoverImage());
        course.setCategory(request.getCategory() != null ? request.getCategory() : "OTHER");
        course.setDifficulty(request.getDifficulty() != null ? request.getDifficulty() : "BEGINNER");
        course.setMaxStudents(request.getMaxStudents() != null ? request.getMaxStudents() : 0);
        course.setLocation(request.getLocation());
        if (request.getStartDate() != null && !request.getStartDate().isEmpty()) {
            course.setStartDate(java.time.LocalDate.parse(request.getStartDate()));
        }
        course.setTags(request.getTags());
        course.setTotalLessons(request.getTotalLessons() != null ? request.getTotalLessons() : 0);
        course.setFrequency(request.getFrequency());
        course.setScheduleMode(request.getScheduleMode() != null ? request.getScheduleMode() : "MANUAL");
        course.setDefaultTimeSlot(request.getDefaultTimeSlot());
        course.setStatus("ACTIVE");
        course.setCreateTime(LocalDateTime.now());
        courseMapper.insert(course);
        return course;
    }

    @Transactional
    public Course updateCourse(Long coachId, UpdateCourseRequest request) {
        Course course = courseMapper.selectById(request.getId());
        if (course == null) {
            throw new RuntimeException("课程不存在");
        }
        if (!course.getCoachId().equals(coachId)) {
            throw new RuntimeException("只能修改自己的课程");
        }

        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            course.setName(request.getName().trim());
        }
        if (request.getDescription() != null) {
            course.setDescription(request.getDescription());
        }
        if (request.getCoverImage() != null) {
            course.setCoverImage(request.getCoverImage());
        }
        if (request.getCategory() != null) {
            course.setCategory(request.getCategory());
        }
        if (request.getDifficulty() != null) {
            course.setDifficulty(request.getDifficulty());
        }
        if (request.getMaxStudents() != null) {
            course.setMaxStudents(request.getMaxStudents());
        }
        if (request.getLocation() != null) {
            course.setLocation(request.getLocation());
        }
        if (request.getStartDate() != null && !request.getStartDate().isEmpty()) {
            course.setStartDate(java.time.LocalDate.parse(request.getStartDate()));
        }
        if (request.getTags() != null) {
            course.setTags(request.getTags());
        }
        if (request.getTotalLessons() != null) {
            course.setTotalLessons(request.getTotalLessons());
        }
        if (request.getFrequency() != null) {
            course.setFrequency(request.getFrequency());
        }
        if (request.getScheduleMode() != null) {
            course.setScheduleMode(request.getScheduleMode());
        }
        if (request.getDefaultTimeSlot() != null) {
            course.setDefaultTimeSlot(request.getDefaultTimeSlot());
        }
        course.setUpdateTime(LocalDateTime.now());
        courseMapper.updateById(course);
        return course;
    }

    @Transactional
    public Course updateCoursePrice(Long coachId, Long courseId, BigDecimal newPrice) {
        if (newPrice == null || newPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("价格不能为负数");
        }

        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new RuntimeException("课程不存在");
        }
        if (!course.getCoachId().equals(coachId)) {
            throw new RuntimeException("只能修改自己的课程");
        }

        course.setPrice(newPrice);
        course.setUpdateTime(LocalDateTime.now());
        courseMapper.updateById(course);
        return course;
    }

    public IPage<Course> listCourses(String type, String keyword, int pageNum, int pageSize) {
        Page<Course> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Course> wrapper = new LambdaQueryWrapper<Course>()
                .eq(Course::getStatus, "ACTIVE");

        if (type != null && !type.isEmpty()) {
            wrapper.eq(Course::getType, type);
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(Course::getName, kw)
                    .or().like(Course::getDescription, kw));
        }

        wrapper.orderByDesc(Course::getCreateTime);
        return courseMapper.selectPage(page, wrapper);
    }

    public Course getCourseDetail(Long courseId) {
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new RuntimeException("课程不存在");
        }
        return course;
    }

    public IPage<Course> getCoachCourses(Long coachId, int pageNum, int pageSize) {
        Page<Course> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Course> wrapper = new LambdaQueryWrapper<Course>()
                .eq(Course::getCoachId, coachId)
                .orderByDesc(Course::getCreateTime);
        return courseMapper.selectPage(page, wrapper);
    }
}
