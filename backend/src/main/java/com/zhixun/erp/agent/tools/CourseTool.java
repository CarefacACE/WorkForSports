package com.zhixun.erp.agent.tools;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhixun.erp.course.entity.Course;
import com.zhixun.erp.course.entity.Enrollment;
import com.zhixun.erp.course.entity.PrivateCoachProfile;
import com.zhixun.erp.course.mapper.CourseMapper;
import com.zhixun.erp.course.mapper.EnrollmentMapper;
import com.zhixun.erp.course.mapper.PrivateCoachProfileMapper;
import com.zhixun.erp.user.entity.User;
import com.zhixun.erp.user.mapper.UserMapper;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CourseTool {

    private final EnrollmentMapper enrollmentMapper;
    private final CourseMapper courseMapper;
    private final PrivateCoachProfileMapper privateCoachProfileMapper;
    private final UserMapper userMapper;

    @Tool(name = "query_available_courses", value = "查询系统上已发布的活跃课程（公共课），可按类别、类型、难度、关键字筛选。用于向用户推荐和介绍课程。返回课程名称、类别、难度、价格、地点、频率等信息。")
    public String queryAvailableCourses(
            @P("课程类别（可选，如 YOGA/STRENGTH/CARDIO/HIIT/DANCE/SWIMMING/CYCLING/RUNNING/BOXING）") String category,
            @P("课程类型（可选，PUBLIC/PRIVATE）") String type,
            @P("难度级别（可选，BEGINNER/INTERMEDIATE/ADVANCED）") String difficulty,
            @P("搜索关键字（可选，匹配课程名称和描述）") String keyword) {
        LambdaQueryWrapper<Course> wrapper = new LambdaQueryWrapper<Course>()
                .eq(Course::getStatus, "ACTIVE");

        if (category != null && !category.trim().isEmpty()) {
            wrapper.eq(Course::getCategory, category.trim().toUpperCase());
        }
        if (type != null && !type.trim().isEmpty()) {
            wrapper.eq(Course::getType, type.trim().toUpperCase());
        }
        if (difficulty != null && !difficulty.trim().isEmpty()) {
            wrapper.eq(Course::getDifficulty, difficulty.trim().toUpperCase());
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(Course::getName, kw)
                    .or().like(Course::getDescription, kw));
        }

        wrapper.orderByDesc(Course::getCreateTime).last("LIMIT 10");
        List<Course> courses = courseMapper.selectList(wrapper);

        if (courses.isEmpty()) {
            return "目前没有找到符合条件的课程。";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("## 系统上架课程\n\n");
        sb.append("| 序号 | 课程名称 | 类别 | 难度 | 价格 | 频率 | 地点 |\n");
        sb.append("|:---:|---|---|---|---|---|---|\n");
        for (int i = 0; i < courses.size(); i++) {
            Course c = courses.get(i);
            sb.append(String.format("| %d | %s | %s | %s | %.2f 元 | %s | %s |\n",
                    i + 1,
                    c.getName(),
                    translateCategory(c.getCategory()),
                    translateDifficulty(c.getDifficulty()),
                    c.getPrice() != null ? c.getPrice() : 0,
                    translateFrequency(c.getFrequency()),
                    c.getLocation() != null ? c.getLocation() : "—"));
        }
        sb.append("\n💡 如需了解某个课程的更多详情或报名，可以告诉我课程名称！");
        return sb.toString();
    }

    @Tool(name = "query_enrolled_courses", value = "查询用户已报名的所有课程（公共课和私教课），返回课程名称、类型、难度、频率、教练等信息。")
    public String queryEnrolledCourses(@P("用户ID") Long userId) {
        List<Enrollment> enrollments = enrollmentMapper.selectList(
                new LambdaQueryWrapper<Enrollment>()
                        .eq(Enrollment::getUserId, userId)
                        .ne(Enrollment::getStatus, "CANCELLED"));

        if (enrollments.isEmpty()) {
            return "您当前没有已报名的课程。";
        }

        StringBuilder sb = new StringBuilder("=== 已报名课程 ===\n");

        for (Enrollment e : enrollments) {
            if (e.getCoachId() != null) {
                // 私教课
                User coach = userMapper.selectById(e.getCoachId());
                PrivateCoachProfile coachProfile = privateCoachProfileMapper.selectOne(
                        new LambdaQueryWrapper<PrivateCoachProfile>()
                                .eq(PrivateCoachProfile::getCoachId, e.getCoachId()));

                sb.append(String.format("【私教课】教练：%s", coach != null ? coach.getRealName() : "未知"));
                if (coachProfile != null && coachProfile.getSpecialties() != null) {
                    sb.append("，专长：").append(coachProfile.getSpecialties());
                }
                if (coachProfile != null) {
                    sb.append(String.format("，单节%.0f分钟",
                            coachProfile.getSessionDuration() != null ? coachProfile.getSessionDuration() : 60));
                }
                sb.append(String.format("，剩余%d节\n",
                        e.getRemainingSessions() != null ? e.getRemainingSessions() : 0));
            } else if (e.getCourseId() != null) {
                // 公共课
                Course course = courseMapper.selectById(e.getCourseId());
                if (course != null) {
                    sb.append(String.format("【公共课】%s", course.getName()));
                    sb.append(String.format("，类型：%s", translateCategory(course.getCategory())));
                    sb.append(String.format("，难度：%s", translateDifficulty(course.getDifficulty())));
                    sb.append(String.format("，频率：%s", translateFrequency(course.getFrequency())));
                    if (course.getLocation() != null) sb.append("，地点：").append(course.getLocation());
                    sb.append("\n");
                }
            }
        }

        return sb.toString();
    }

    private String translateCategory(String category) {
        if (category == null) return "其他";
        return switch (category) {
            case "YOGA" -> "瑜伽";
            case "STRENGTH" -> "力量训练";
            case "CARDIO" -> "有氧运动";
            case "HIIT" -> "HIIT";
            case "DANCE" -> "舞蹈";
            case "SWIMMING" -> "游泳";
            case "CYCLING" -> "骑行";
            case "RUNNING" -> "跑步";
            case "BOXING" -> "拳击";
            default -> category;
        };
    }

    private String translateDifficulty(String difficulty) {
        if (difficulty == null) return "未知";
        return switch (difficulty) {
            case "BEGINNER" -> "入门";
            case "INTERMEDIATE" -> "中级";
            case "ADVANCED" -> "高级";
            default -> difficulty;
        };
    }

    private String translateFrequency(String frequency) {
        if (frequency == null) return "未知";
        return switch (frequency) {
            case "DAILY" -> "每天";
            case "WEEKLY_1" -> "每周1次";
            case "WEEKLY_2" -> "每周2次";
            case "WEEKLY_3" -> "每周3次";
            case "BIWEEKLY" -> "每两周1次";
            default -> frequency;
        };
    }
}
