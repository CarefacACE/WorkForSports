package com.zhixun.erp.agent.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhixun.erp.agent.dto.PlanGenerateRequest;
import com.zhixun.erp.course.entity.Course;
import com.zhixun.erp.course.entity.Enrollment;
import com.zhixun.erp.course.entity.PrivateCoachProfile;
import com.zhixun.erp.course.mapper.CourseMapper;
import com.zhixun.erp.course.mapper.EnrollmentMapper;
import com.zhixun.erp.course.mapper.PrivateCoachProfileMapper;
import com.zhixun.erp.exercise.entity.ExerciseRecord;
import com.zhixun.erp.exercise.mapper.ExerciseRecordMapper;
import com.zhixun.erp.health.entity.MemberHealthProfile;
import com.zhixun.erp.health.mapper.MemberHealthProfileMapper;
import com.zhixun.erp.plan.dto.PlanSubmitRequest;
import com.zhixun.erp.plan.entity.TrainingPlan;
import com.zhixun.erp.plan.service.PlanService;
import com.zhixun.erp.user.entity.User;
import com.zhixun.erp.user.mapper.UserMapper;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlanGeneratorService {

    private final ChatLanguageModel chatLanguageModel;
    private final MemberHealthProfileMapper healthProfileMapper;
    private final EnrollmentMapper enrollmentMapper;
    private final CourseMapper courseMapper;
    private final PrivateCoachProfileMapper privateCoachProfileMapper;
    private final ExerciseRecordMapper exerciseRecordMapper;
    private final UserMapper userMapper;
    private final PlanService planService;
    private final ObjectMapper objectMapper;

    private static final Pattern JSON_PATTERN = Pattern.compile(
            "```(?:json)?\\s*\\n?([\\s\\S]*?)\\n?```|(\\{[\\s\\S]*\\})",
            Pattern.MULTILINE);

    /**
     * 根据用户的健康档案和已报课程，通过AI生成结构化训练计划并自动提交。
     * 如果 request 为 null 则使用默认值。
     */
    public TrainingPlan generateAndSubmitPlan(Long userId, PlanGenerateRequest request) {
        // 1. 收集用户数据
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        MemberHealthProfile health = healthProfileMapper.selectOne(
                new LambdaQueryWrapper<MemberHealthProfile>()
                        .eq(MemberHealthProfile::getUserId, userId));

        List<Enrollment> enrollments = enrollmentMapper.selectList(
                new LambdaQueryWrapper<Enrollment>()
                        .eq(Enrollment::getUserId, userId)
                        .ne(Enrollment::getStatus, "CANCELLED"));

        List<ExerciseRecord> recentExercises = exerciseRecordMapper.selectList(
                new LambdaQueryWrapper<ExerciseRecord>()
                        .eq(ExerciseRecord::getUserId, userId)
                        .ge(ExerciseRecord::getExerciseDate, LocalDate.now().minusDays(30))
                        .orderByDesc(ExerciseRecord::getExerciseDate));

        // 2. 解析请求参数（null 时使用默认值）
        String goal = (request != null && request.getGoal() != null && !request.getGoal().isBlank())
                ? request.getGoal().trim() : getDefaultGoal(health);
        int durationDays = (request != null && request.getDurationDays() != null && request.getDurationDays() > 0)
                ? request.getDurationDays() : 28;
        LocalDate startDate;
        if (request != null && request.getStartDate() != null && !request.getStartDate().isBlank()) {
            startDate = LocalDate.parse(request.getStartDate());
        } else {
            startDate = LocalDate.now().plusDays(1);
        }
        LocalDate endDate = startDate.plusDays(durationDays - 1);
        String description = (request != null && request.getDescription() != null)
                ? request.getDescription().trim() : "";

        // 3. 构建用户数据摘要
        String userDataSummary = buildUserDataSummary(user, health, enrollments, recentExercises);

        // 4. 构建 prompt
        String prompt = buildPrompt(userDataSummary, goal, durationDays, startDate, endDate, description);

        // 5. 调用 LLM
        log.info("Generating training plan for userId={}, goal={}, durationDays={}, startDate={}",
                userId, goal, durationDays, startDate);
        String llmResponse = chatLanguageModel.chat(prompt);
        log.info("LLM response length={}", llmResponse.length());

        // 6. 解析 JSON
        String json = extractJson(llmResponse);
        log.info("Extracted JSON length={}", json.length());

        // 7. 转换并提交
        PlanSubmitRequest submitRequest = parseToRequest(json, goal, durationDays, startDate, endDate);

        return planService.submitPlan(userId, submitRequest);
    }

    private String getDefaultGoal(MemberHealthProfile health) {
        if (health != null && health.getFitnessGoal() != null) {
            return switch (health.getFitnessGoal()) {
                case "MUSCLE_GAIN" -> "增肌塑形";
                case "FAT_LOSS" -> "减脂燃脂";
                case "BODY_SHAPING" -> "塑形美体";
                case "FITNESS" -> "体能提升";
                default -> "综合训练";
            };
        }
        return "综合训练";
    }

    /**
     * 构建用户数据摘要
     */
    private String buildUserDataSummary(User user, MemberHealthProfile health,
                                         List<Enrollment> enrollments,
                                         List<ExerciseRecord> recentExercises) {
        StringBuilder summary = new StringBuilder();
        summary.append(String.format("用户基本信息：姓名=%s，性别=%s，年龄=",
                user.getRealName() != null ? user.getRealName() : "未填写",
                user.getGender() != null ? user.getGender() : "未填写"));
        if (user.getBirthday() != null && !user.getBirthday().isEmpty()) {
            try {
                int age = LocalDate.now().getYear() - Integer.parseInt(user.getBirthday().substring(0, 4));
                summary.append(age);
            } catch (Exception e) {
                summary.append("未知");
            }
        } else {
            summary.append("未知");
        }
        summary.append("\n");

        if (health != null) {
            summary.append("=== 健康档案 ===\n");
            if (health.getHeight() != null && health.getWeight() != null) {
                double heightM = health.getHeight().doubleValue() / 100.0;
                double bmi = health.getWeight().doubleValue() / (heightM * heightM);
                summary.append(String.format("身高：%.1f cm，体重：%.1f kg，BMI：%.1f\n",
                        health.getHeight(), health.getWeight(), bmi));
            }
            if (health.getBodyFat() != null) {
                summary.append(String.format("体脂率：%.1f%%\n", health.getBodyFat()));
            }
            if (health.getMuscleMass() != null) {
                summary.append(String.format("肌肉量：%.1f kg\n", health.getMuscleMass()));
            }
            if (health.getFitnessGoal() != null) {
                String goal = switch (health.getFitnessGoal()) {
                    case "MUSCLE_GAIN" -> "增肌";
                    case "FAT_LOSS" -> "减脂";
                    case "BODY_SHAPING" -> "塑形";
                    case "FITNESS" -> "体能提升";
                    default -> health.getFitnessGoal();
                };
                summary.append(String.format("健身目标：%s\n", goal));
            }
            if (health.getWeeklyWorkoutFreq() != null) {
                summary.append(String.format("每周训练频率目标：%d次\n", health.getWeeklyWorkoutFreq()));
            }
            if (health.getTargetWeight() != null) {
                summary.append(String.format("目标体重：%.1f kg\n", health.getTargetWeight()));
            }
        } else {
            summary.append("健康档案：未填写\n");
        }

        summary.append("\n=== 已报名课程 ===\n");
        if (enrollments.isEmpty()) {
            summary.append("暂无已报名课程\n");
        } else {
            for (Enrollment e : enrollments) {
                if (e.getCoachId() != null) {
                    User coach = userMapper.selectById(e.getCoachId());
                    PrivateCoachProfile cp = privateCoachProfileMapper.selectOne(
                            new LambdaQueryWrapper<PrivateCoachProfile>()
                                    .eq(PrivateCoachProfile::getCoachId, e.getCoachId()));
                    summary.append(String.format("【私教】教练：%s", coach != null ? coach.getRealName() : "未知"));
                    if (cp != null && cp.getSpecialties() != null) {
                        summary.append("，专长：").append(cp.getSpecialties());
                    }
                    summary.append("\n");
                } else if (e.getCourseId() != null) {
                    Course course = courseMapper.selectById(e.getCourseId());
                    if (course != null) {
                        summary.append(String.format("【公共课】%s，类别：%s，难度：%s，频率：%s\n",
                                course.getName(),
                                course.getCategory() != null ? course.getCategory() : "其他",
                                course.getDifficulty() != null ? course.getDifficulty() : "未知",
                                course.getFrequency() != null ? course.getFrequency() : "未指定"));
                    }
                }
            }
        }

        summary.append("\n=== 近30天运动记录 ===\n");
        if (recentExercises.isEmpty()) {
            summary.append("暂无运动记录\n");
        } else {
            int count = Math.min(recentExercises.size(), 10);
            for (int i = 0; i < count; i++) {
                ExerciseRecord r = recentExercises.get(i);
                summary.append(String.format("%s：%s，%d分钟",
                        r.getExerciseDate(),
                        r.getType() != null ? r.getType() : "未知",
                        r.getDuration() != null ? r.getDuration() : 0));
                if (r.getDistance() != null) {
                    summary.append(String.format("，%.2f km", r.getDistance()));
                }
                if (r.getCalories() != null) {
                    summary.append(String.format("，%d kcal", r.getCalories()));
                }
                summary.append("\n");
            }
            if (recentExercises.size() > 10) {
                summary.append("...共" + recentExercises.size() + "条记录\n");
            }
        }

        return summary.toString();
    }

    private String buildPrompt(String userData, String goal, int durationDays,
                               LocalDate startDate, LocalDate endDate, String description) {
        String descLine = (description != null && !description.isBlank())
                ? "\n- 用户指定描述：" + description : "";
        return """
                你是一位资深的专业健身教练和国家认证的运动营养师，拥有10年以上的训练计划制定经验。

                ## 用户数据

                %s

                ## 计划要求

                - 训练目标：%s
                - 持续天数：%d 天
                - 开始日期：%s
                - 结束日期：%s
                - 每周训练5天，休息2天（合理安排休息日，避免连续高强度训练）%s

                ## 制定原则

                1. 根据用户的BMI、体脂率和健身目标制定个性化计划
                2. 结合用户已报名的课程类型安排训练内容（例如报了瑜伽课就多安排柔韧拉伸，报了力量课就安排力量训练）
                3. 根据用户近30天的运动历史调整难度和时长
                4. 训练类型多样化：有氧运动、力量训练、HIIT、瑜伽、拉伸、休息等
                5. 强度级别：LOW（低）、MEDIUM（中）、HIGH（高），根据用户体能合理安排
                6. 每项训练要有具体的动作指导和组数/次数/时长

                ## 输出格式

                请只输出以下JSON格式，不要输出任何其他内容，不要用```json```包裹：

                {
                  "goal": "%s",
                  "durationDays": %d,
                  "startDate": "%s",
                  "endDate": "%s",
                  "description": "简要描述训练计划的目标和安排（30字以内）",
                  "details": [
                    {
                      "dayNumber": 1,
                      "trainingType": "跑步",
                      "content": "具体训练内容描述，如：慢跑5公里，配速6:00/km，热身10分钟，拉伸15分钟",
                      "durationMinutes": 60,
                      "intensity": "MEDIUM"
                    }
                  ]
                }

                重要：必须包含所有%d天的训练计划（含休息日），dayNumber从1到%d连续递增。
                """.formatted(
                userData,
                goal, durationDays, startDate, endDate, descLine,
                goal, durationDays, startDate, endDate,
                durationDays, durationDays);
    }

    /**
     * 从 LLM 响应中提取 JSON 字符串
     */
    String extractJson(String llmResponse) {
        // 先尝试直接解析
        String trimmed = llmResponse.trim();
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            return trimmed;
        }

        // 尝试从 ```json ... ``` 代码块中提取
        Matcher matcher = JSON_PATTERN.matcher(llmResponse);
        if (matcher.find()) {
            String g1 = matcher.group(1);
            String g2 = matcher.group(2);
            String extracted = (g1 != null && !g1.isBlank()) ? g1 : g2;
            if (extracted != null && !extracted.isBlank()) {
                extracted = extracted.trim();
                int start = extracted.indexOf('{');
                int end = extracted.lastIndexOf('}');
                if (start >= 0 && end > start) {
                    return extracted.substring(start, end + 1);
                }
            }
        }

        throw new RuntimeException("无法从AI响应中提取有效的JSON训练计划，请重试");
    }

    /**
     * 解析 JSON 为 PlanSubmitRequest，缺失字段使用默认值
     */
    PlanSubmitRequest parseToRequest(String json, String defaultGoal, int durationDays,
                                     LocalDate startDate, LocalDate endDate) {
        try {
            JsonNode root = objectMapper.readTree(json);

            PlanSubmitRequest request = new PlanSubmitRequest();
            request.setGoal(root.has("goal") && !root.get("goal").isNull()
                    ? root.get("goal").asText() : defaultGoal);
            request.setDurationDays(root.has("durationDays") && !root.get("durationDays").isNull()
                    ? root.get("durationDays").asInt() : durationDays);
            request.setStartDate(root.has("startDate") && !root.get("startDate").isNull()
                    ? root.get("startDate").asText() : startDate.toString());
            request.setEndDate(root.has("endDate") && !root.get("endDate").isNull()
                    ? root.get("endDate").asText() : endDate.toString());
            request.setDescription(root.has("description") && !root.get("description").isNull()
                    ? root.get("description").asText() : "");

            JsonNode detailsNode = root.get("details");
            if (detailsNode == null || !detailsNode.isArray() || detailsNode.isEmpty()) {
                throw new RuntimeException("AI生成的训练计划明细为空，请重试");
            }

            List<PlanSubmitRequest.DetailItem> details = new ArrayList<>();
            for (JsonNode item : detailsNode) {
                PlanSubmitRequest.DetailItem detail = new PlanSubmitRequest.DetailItem();

                if (item.has("dayNumber") && !item.get("dayNumber").isNull()) {
                    detail.setDayNumber(item.get("dayNumber").asInt());
                } else {
                    detail.setDayNumber(details.size() + 1);
                }

                detail.setTrainingType(item.has("trainingType") && !item.get("trainingType").isNull()
                        ? item.get("trainingType").asText() : "训练");
                detail.setContent(item.has("content") && !item.get("content").isNull()
                        ? item.get("content").asText() : "根据自身情况安排训练");

                if (item.has("durationMinutes") && !item.get("durationMinutes").isNull()) {
                    detail.setDurationMinutes(item.get("durationMinutes").asInt());
                } else {
                    detail.setDurationMinutes(60);
                }

                if (item.has("intensity") && !item.get("intensity").isNull()) {
                    String intensity = item.get("intensity").asText().toUpperCase();
                    detail.setIntensity(
                            intensity.equals("HIGH") || intensity.equals("MEDIUM") || intensity.equals("LOW")
                                    ? intensity : "MEDIUM");
                } else {
                    detail.setIntensity("MEDIUM");
                }

                details.add(detail);
            }

            // 验证天数
            if (details.size() != durationDays) {
                log.warn("LLM returned {} days but expected {}. Using returned count.", details.size(), durationDays);
                request.setDurationDays(details.size());
            }

            request.setDetails(details);
            return request;

        } catch (JsonProcessingException e) {
            log.error("Failed to parse LLM JSON response", e);
            throw new RuntimeException("AI返回的训练计划格式异常，请重试。错误详情：" + e.getMessage());
        }
    }
}
