package com.zhixun.erp.schedule.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhixun.erp.checkin.service.CheckInService;
import com.zhixun.erp.chat.service.NotificationService;
import com.zhixun.erp.course.entity.Course;
import com.zhixun.erp.course.entity.Enrollment;
import com.zhixun.erp.course.mapper.CourseMapper;
import com.zhixun.erp.course.mapper.EnrollmentMapper;
import com.zhixun.erp.schedule.entity.CourseSchedule;
import com.zhixun.erp.schedule.mapper.CourseScheduleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CourseScheduleService {

    private final CourseScheduleMapper scheduleMapper;
    private final CourseMapper courseMapper;
    private final EnrollmentMapper enrollmentMapper;
    private final CheckInService checkInService;
    private final NotificationService notificationService;

    private static final String[] TIME_SLOTS = {
            "08:00","09:00","10:00","11:00","12:00","13:00",
            "14:00","15:00","16:00","17:00","18:00","19:00","20:00","21:00"
    };

    private static final String[] COLORS = {
            "#3056d3","#e6a23c","#67c23a","#f56c6c","#909399","#9b59b6","#1abc9c","#e74c3c"
    };

    public List<CourseSchedule> getCoachSchedules(Long coachId, LocalDateTime from, LocalDateTime to) {
        LambdaQueryWrapper<CourseSchedule> wrapper = new LambdaQueryWrapper<CourseSchedule>()
                .eq(CourseSchedule::getCoachId, coachId);
        if (from != null) wrapper.ge(CourseSchedule::getStartTime, from);
        if (to != null) wrapper.le(CourseSchedule::getEndTime, to);
        wrapper.orderByAsc(CourseSchedule::getStartTime);
        return scheduleMapper.selectList(wrapper);
    }

    /** 管理员：查看所有日程（不区分教练/学员） */
    public List<CourseSchedule> getAllSchedules(LocalDateTime from, LocalDateTime to) {
        LambdaQueryWrapper<CourseSchedule> wrapper = new LambdaQueryWrapper<CourseSchedule>();
        if (from != null) wrapper.ge(CourseSchedule::getStartTime, from);
        if (to != null) wrapper.le(CourseSchedule::getEndTime, to);
        wrapper.orderByAsc(CourseSchedule::getStartTime);
        return scheduleMapper.selectList(wrapper);
    }

    /** 管理员：编辑任意日程 */
    @Transactional
    public CourseSchedule adminUpdateSchedule(CourseSchedule input) {
        CourseSchedule existing = scheduleMapper.selectById(input.getId());
        if (existing == null) throw new RuntimeException("日程不存在");
        if (input.getTitle() != null) existing.setTitle(input.getTitle());
        if (input.getStartTime() != null) existing.setStartTime(input.getStartTime());
        if (input.getEndTime() != null) existing.setEndTime(input.getEndTime());
        if (input.getLocation() != null) existing.setLocation(input.getLocation());
        if (input.getColor() != null) existing.setColor(input.getColor());
        existing.setUpdateTime(LocalDateTime.now());
        scheduleMapper.updateById(existing);
        return existing;
    }

    /** 管理员：删除任意日程 */
    @Transactional
    public void adminDeleteSchedule(Long scheduleId) {
        CourseSchedule existing = scheduleMapper.selectById(scheduleId);
        if (existing == null) throw new RuntimeException("日程不存在");
        scheduleMapper.deleteById(scheduleId);
    }

    public List<CourseSchedule> getMemberSchedules(Long userId, LocalDateTime from, LocalDateTime to) {
        LambdaQueryWrapper<CourseSchedule> wrapper = new LambdaQueryWrapper<CourseSchedule>()
                .and(w -> w
                        // Regular course enrollments
                        .inSql(CourseSchedule::getCourseId,
                                "SELECT course_id FROM enrollment WHERE user_id = " + userId
                                        + " AND status IN ('PAID','CONFIRMED') AND deleted = 0 AND total_sessions IS NULL")
                        // Private coaching: booked sessions
                        .or().eq(CourseSchedule::getMemberId, userId)
                );
        if (from != null) wrapper.ge(CourseSchedule::getStartTime, from);
        if (to != null) wrapper.le(CourseSchedule::getEndTime, to);
        wrapper.orderByAsc(CourseSchedule::getStartTime);
        return scheduleMapper.selectList(wrapper);
    }

    @Transactional
    public CourseSchedule createSchedule(Long coachId, CourseSchedule input) {
        if (input.getCourseId() != null) {
            Course course = courseMapper.selectById(input.getCourseId());
            if (course == null || !course.getCoachId().equals(coachId)) {
                throw new RuntimeException("只能为自己的课程排课");
            }
        }
        input.setCoachId(coachId);
        input.setCreateTime(LocalDateTime.now());
        // Private coaching available slot
        if (input.getCourseId() == null) {
            input.setBookingStatus("AVAILABLE");
            if (input.getTitle() == null || input.getTitle().isEmpty()) {
                input.setTitle("私教空闲");
            }
        }
        scheduleMapper.insert(input);
        checkInService.generatePendingRecords(input.getId());
        return input;
    }

    @Transactional
    public CourseSchedule updateSchedule(Long coachId, CourseSchedule input) {
        CourseSchedule existing = scheduleMapper.selectById(input.getId());
        if (existing == null || !existing.getCoachId().equals(coachId)) {
            throw new RuntimeException("只能修改自己的排课");
        }

        // 检查是否已经过去的课程
        if (existing.getEndTime().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("已结束的课程无法修改时间");
        }

        // 检查时间是否发生变化
        boolean timeChanged = !existing.getStartTime().equals(input.getStartTime())
                || !existing.getEndTime().equals(input.getEndTime());

        existing.setTitle(input.getTitle());
        existing.setStartTime(input.getStartTime());
        existing.setEndTime(input.getEndTime());
        existing.setLocation(input.getLocation());
        existing.setColor(input.getColor());
        existing.setUpdateTime(LocalDateTime.now());
        scheduleMapper.updateById(existing);

        // 如果时间发生变化，通知该课程的所有学员
        if (timeChanged && existing.getCourseId() != null) {
            sendTimeChangeNotification(existing);
        }

        return existing;
    }

    private void sendTimeChangeNotification(CourseSchedule schedule) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        String oldTime = schedule.getStartTime().format(formatter) + " - " + schedule.getEndTime().format(formatter);

        // 查询该课程的所有学员
        List<Enrollment> enrollments = enrollmentMapper.selectList(
                new LambdaQueryWrapper<Enrollment>()
                        .eq(Enrollment::getCourseId, schedule.getCourseId())
                        .eq(Enrollment::getDeleted, 0));

        String title = "课程时间变更通知";
        String content = String.format("课程「%s」的时间已变更，新时间：%s，请注意查看。",
                schedule.getTitle(), oldTime);

        for (Enrollment enrollment : enrollments) {
            notificationService.sendNotification(
                    enrollment.getUserId(),
                    title,
                    content,
                    "SCHEDULE_CHANGE",
                    schedule.getId());
        }
    }

    @Transactional
    public void deleteSchedule(Long coachId, Long scheduleId) {
        CourseSchedule existing = scheduleMapper.selectById(scheduleId);
        if (existing == null || !existing.getCoachId().equals(coachId)) {
            throw new RuntimeException("只能删除自己的排课");
        }
        scheduleMapper.deleteById(scheduleId);
    }

    /**
     * 自动排课算法：根据课程的频率、总课时数、开始日期和默认时间段自动生成排课
     */
    @Transactional
    public List<CourseSchedule> autoSchedule(Long coachId, Long courseId) {
        Course course = courseMapper.selectById(courseId);
        if (course == null || !course.getCoachId().equals(coachId)) {
            throw new RuntimeException("只能为自己的课程排课");
        }

        int totalLessons = course.getTotalLessons() != null ? course.getTotalLessons() : 0;
        if (totalLessons <= 0) {
            throw new RuntimeException("请先设置总课时数");
        }

        String frequency = course.getFrequency();
        if (frequency == null || frequency.isEmpty()) {
            throw new RuntimeException("请先设置上课频率");
        }

        LocalDate startDate = course.getStartDate();
        if (startDate == null) {
            throw new RuntimeException("请先设置开课日期");
        }

        String defaultSlot = course.getDefaultTimeSlot();
        if (defaultSlot == null || defaultSlot.isEmpty()) {
            defaultSlot = "10:00";
        }

        String color = COLORS[(int) (courseId % COLORS.length)];
        String location = course.getLocation();

        // 计算每周上课日（DayOfWeek）
        List<DayOfWeek> weeklyDays = calculateWeeklyDays(frequency, startDate);
        int dayStep = "DAILY".equals(frequency) ? 1 : "BIWEEKLY".equals(frequency) ? 14 : 7;

        // 获取教练现有排课
        List<CourseSchedule> existingSchedules = scheduleMapper.selectList(
                new LambdaQueryWrapper<CourseSchedule>()
                        .eq(CourseSchedule::getCoachId, coachId)
                        .ge(CourseSchedule::getStartTime, startDate.atStartOfDay()));

        List<CourseSchedule> generated = new ArrayList<>();
        LocalDate currentDate = startDate;
        int count = 0;
        int maxIterations = totalLessons * 10; // 防止无限循环
        int iteration = 0;

        while (count < totalLessons && iteration < maxIterations) {
            iteration++;

            if ("DAILY".equals(frequency)) {
                // 每天都排
            } else if ("BIWEEKLY".equals(frequency)) {
                // 只在对应星期排
                if (!weeklyDays.contains(currentDate.getDayOfWeek())) {
                    currentDate = currentDate.plusDays(1);
                    continue;
                }
            } else {
                // WEEKLY_1/2/3: 只在对应星期排
                if (!weeklyDays.contains(currentDate.getDayOfWeek())) {
                    currentDate = currentDate.plusDays(1);
                    continue;
                }
            }

            // 找到当天可用的时间段
            String slot = findAvailableSlot(currentDate, defaultSlot, existingSchedules, generated);
            if (slot != null) {
                int hour = Integer.parseInt(slot.split(":")[0]);
                LocalDateTime startTime = currentDate.atTime(hour, 0);
                LocalDateTime endTime = currentDate.atTime(hour + 1, 0);

                CourseSchedule schedule = new CourseSchedule();
                schedule.setCourseId(courseId);
                schedule.setCoachId(coachId);
                schedule.setTitle(course.getName());
                schedule.setStartTime(startTime);
                schedule.setEndTime(endTime);
                schedule.setLocation(location);
                schedule.setColor(color);
                schedule.setCreateTime(LocalDateTime.now());
                scheduleMapper.insert(schedule);
                checkInService.generatePendingRecords(schedule.getId());
                generated.add(schedule);
                count++;
            }

            // 移到下一天
            if ("DAILY".equals(frequency)) {
                currentDate = currentDate.plusDays(1);
            } else if ("BIWEEKLY".equals(frequency)) {
                // 隔周：跳到下一周的同一天
                DayOfWeek targetDay = currentDate.getDayOfWeek();
                currentDate = currentDate.plusDays(1);
                while (currentDate.getDayOfWeek() != targetDay || (currentDate.toEpochDay() - startDate.toEpochDay()) % 14 != 0) {
                    currentDate = currentDate.plusDays(1);
                    if (iteration++ > maxIterations) break;
                }
            } else {
                currentDate = currentDate.plusDays(1);
            }
        }

        return generated;
    }

    /**
     * 清除某课程的自动排课
     */
    @Transactional
    public void clearAutoSchedule(Long coachId, Long courseId) {
        Course course = courseMapper.selectById(courseId);
        if (course == null || !course.getCoachId().equals(coachId)) {
            throw new RuntimeException("只能清除自己的排课");
        }
        scheduleMapper.delete(new LambdaQueryWrapper<CourseSchedule>()
                .eq(CourseSchedule::getCoachId, coachId)
                .eq(CourseSchedule::getCourseId, courseId));
    }

    /**
     * 根据频率计算每周上课的星期
     */
    private List<DayOfWeek> calculateWeeklyDays(String frequency, LocalDate startDate) {
        List<DayOfWeek> days = new ArrayList<>();
        DayOfWeek startDay = startDate.getDayOfWeek();

        switch (frequency) {
            case "DAILY":
                for (DayOfWeek d : DayOfWeek.values()) days.add(d);
                break;
            case "WEEKLY_1":
            case "BIWEEKLY":
                days.add(startDay);
                break;
            case "WEEKLY_2":
                days.add(startDay);
                days.add(startDay.plus(3)); // 间隔3天
                break;
            case "WEEKLY_3":
                days.add(startDay);
                days.add(startDay.plus(2)); // 间隔2天
                days.add(startDay.plus(4)); // 间隔4天
                break;
            default:
                days.add(startDay);
        }
        return days;
    }

    /**
     * 找到当天可用的时间段，优先使用默认时间，冲突则遍历其他时间
     */
    private String findAvailableSlot(LocalDate date, String preferredSlot,
                                     List<CourseSchedule> existing, List<CourseSchedule> generated) {
        // 先尝试默认时间
        if (!hasConflict(date, preferredSlot, existing, generated)) {
            return preferredSlot;
        }
        // 遍历所有时间段
        for (String slot : TIME_SLOTS) {
            if (!slot.equals(preferredSlot) && !hasConflict(date, slot, existing, generated)) {
                return slot;
            }
        }
        return null; // 当天无空闲
    }

    /**
     * 检查某天的某时间段是否与已有排课冲突
     */
    private boolean hasConflict(LocalDate date, String slot,
                                List<CourseSchedule> existing, List<CourseSchedule> generated) {
        int hour = Integer.parseInt(slot.split(":")[0]);
        LocalDateTime start = date.atTime(hour, 0);
        LocalDateTime end = date.atTime(hour + 1, 0);

        for (CourseSchedule s : existing) {
            if (s.getStartTime().isBefore(end) && s.getEndTime().isAfter(start)) {
                return true;
            }
        }
        for (CourseSchedule s : generated) {
            if (s.getStartTime().isBefore(end) && s.getEndTime().isAfter(start)) {
                return true;
            }
        }
        return false;
    }
}
