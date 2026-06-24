package com.zhixun.erp.agent.tools;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhixun.erp.course.entity.Enrollment;
import com.zhixun.erp.course.mapper.EnrollmentMapper;
import com.zhixun.erp.schedule.entity.CourseSchedule;
import com.zhixun.erp.schedule.mapper.CourseScheduleMapper;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ScheduleTool {

    private final CourseScheduleMapper scheduleMapper;
    private final EnrollmentMapper enrollmentMapper;

    @Tool(name = "query_my_schedule", value = "查询用户的排课日程安排。返回本周或下周的所有排课信息，包括课程名称、时间、地点。")
    public String queryMySchedule(
            @P("用户ID") Long userId,
            @P("用户角色：COACH或MEMBER") String role,
            @P("查询范围：THIS_WEEK或NEXT_WEEK，默认THIS_WEEK") String week) {
        LocalDate today = LocalDate.now();
        LocalDate weekStart, weekEnd;

        if ("NEXT_WEEK".equalsIgnoreCase(week)) {
            weekStart = today.with(TemporalAdjusters.next(DayOfWeek.MONDAY));
            weekEnd = weekStart.plusDays(6);
        } else {
            weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            weekEnd = weekStart.plusDays(6);
        }

        LocalDateTime from = weekStart.atStartOfDay();
        LocalDateTime to = weekEnd.atTime(LocalTime.MAX);

        List<CourseSchedule> schedules;
        if ("COACH".equalsIgnoreCase(role)) {
            schedules = scheduleMapper.selectList(
                    new LambdaQueryWrapper<CourseSchedule>()
                            .eq(CourseSchedule::getCoachId, userId)
                            .ge(CourseSchedule::getStartTime, from)
                            .le(CourseSchedule::getEndTime, to)
                            .orderByAsc(CourseSchedule::getStartTime));
        } else {
            List<Long> courseIds = enrollmentMapper.selectList(
                    new LambdaQueryWrapper<Enrollment>()
                            .eq(Enrollment::getUserId, userId)
                            .in(Enrollment::getStatus, "PAID", "CONFIRMED"))
                    .stream().map(Enrollment::getCourseId).collect(Collectors.toList());

            if (courseIds.isEmpty()) return "暂无排课安排，请先选课。";

            schedules = scheduleMapper.selectList(
                    new LambdaQueryWrapper<CourseSchedule>()
                            .in(CourseSchedule::getCourseId, courseIds)
                            .ge(CourseSchedule::getStartTime, from)
                            .le(CourseSchedule::getEndTime, to)
                            .orderByAsc(CourseSchedule::getStartTime));
        }

        if (schedules.isEmpty()) {
            return (week == null || "THIS_WEEK".equalsIgnoreCase(week)) ? "本周暂无排课安排。" : "下周暂无排课安排。";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("=== ").append(week == null || "THIS_WEEK".equalsIgnoreCase(week) ? "本周" : "下周").append("排课安排 ===\n");
        for (CourseSchedule s : schedules) {
            sb.append(String.format("【%s】%s %s-%s 地点：%s\n",
                    s.getTitle(),
                    s.getStartTime().toLocalDate(),
                    s.getStartTime().toLocalTime().toString().substring(0, 5),
                    s.getEndTime().toLocalTime().toString().substring(0, 5),
                    s.getLocation() != null ? s.getLocation() : "待定"));
        }
        return sb.toString();
    }

    @Tool(name = "query_schedule_by_date", value = "查询指定日期的排课安排")
    public String queryScheduleByDate(
            @P("用户ID") Long userId,
            @P("用户角色：COACH或MEMBER") String role,
            @P("查询日期，格式：yyyy-MM-dd") String date) {
        LocalDate queryDate = LocalDate.parse(date);
        LocalDateTime from = queryDate.atStartOfDay();
        LocalDateTime to = queryDate.atTime(LocalTime.MAX);

        List<CourseSchedule> schedules;
        if ("COACH".equalsIgnoreCase(role)) {
            schedules = scheduleMapper.selectList(
                    new LambdaQueryWrapper<CourseSchedule>()
                            .eq(CourseSchedule::getCoachId, userId)
                            .ge(CourseSchedule::getStartTime, from)
                            .le(CourseSchedule::getEndTime, to)
                            .orderByAsc(CourseSchedule::getStartTime));
        } else {
            List<Long> courseIds = enrollmentMapper.selectList(
                    new LambdaQueryWrapper<Enrollment>()
                            .eq(Enrollment::getUserId, userId)
                            .in(Enrollment::getStatus, "PAID", "CONFIRMED"))
                    .stream().map(Enrollment::getCourseId).collect(Collectors.toList());

            if (courseIds.isEmpty()) return "该日暂无排课安排。";

            schedules = scheduleMapper.selectList(
                    new LambdaQueryWrapper<CourseSchedule>()
                            .in(CourseSchedule::getCourseId, courseIds)
                            .ge(CourseSchedule::getStartTime, from)
                            .le(CourseSchedule::getEndTime, to)
                            .orderByAsc(CourseSchedule::getStartTime));
        }

        if (schedules.isEmpty()) {
            return date + " 暂无排课安排。";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("=== ").append(date).append(" 排课安排 ===\n");
        for (CourseSchedule s : schedules) {
            sb.append(String.format("【%s】%s-%s 地点：%s\n",
                    s.getTitle(),
                    s.getStartTime().toLocalTime().toString().substring(0, 5),
                    s.getEndTime().toLocalTime().toString().substring(0, 5),
                    s.getLocation() != null ? s.getLocation() : "待定"));
        }
        return sb.toString();
    }
}
