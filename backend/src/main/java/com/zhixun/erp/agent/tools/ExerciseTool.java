package com.zhixun.erp.agent.tools;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhixun.erp.exercise.entity.ExerciseRecord;
import com.zhixun.erp.exercise.mapper.ExerciseRecordMapper;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ExerciseTool {

    private final ExerciseRecordMapper exerciseRecordMapper;

    @Tool(name = "query_exercise_stats", value = "查询用户的运动统计数据，包括总运动时长、总距离、总热量消耗、运动天数等。可指定时间范围。")
    public String queryExerciseStats(
            @P("用户ID") Long userId,
            @P("开始日期，格式yyyy-MM-dd，可选") String fromDate,
            @P("结束日期，格式yyyy-MM-dd，可选") String toDate) {
        LambdaQueryWrapper<ExerciseRecord> wrapper = new LambdaQueryWrapper<ExerciseRecord>()
                .eq(ExerciseRecord::getUserId, userId);
        if (fromDate != null) wrapper.ge(ExerciseRecord::getExerciseDate, LocalDate.parse(fromDate));
        if (toDate != null) wrapper.le(ExerciseRecord::getExerciseDate, LocalDate.parse(toDate));

        List<ExerciseRecord> records = exerciseRecordMapper.selectList(wrapper);
        if (records.isEmpty()) return "暂无运动记录。";

        int totalDuration = records.stream().mapToInt(r -> r.getDuration() != null ? r.getDuration() : 0).sum();
        double totalDistance = records.stream().mapToDouble(r -> r.getDistance() != null ? r.getDistance().doubleValue() : 0).sum();
        int totalCalories = records.stream().mapToInt(r -> r.getCalories() != null ? r.getCalories() : 0).sum();
        long totalDays = records.stream().map(r -> r.getExerciseDate()).distinct().count();
        int totalSessions = records.size();

        return String.format("""
                === 运动统计 ===
                运动次数：%d 次
                运动天数：%d 天
                总运动时长：%d 分钟（约 %.1f 小时）
                总运动距离：%.2f 公里
                总热量消耗：%d 千卡
                平均每次时长：%d 分钟
                平均每次消耗：%d 千卡""",
                totalSessions, totalDays, totalDuration, totalDuration / 60.0,
                totalDistance, totalCalories,
                totalSessions > 0 ? totalDuration / totalSessions : 0,
                totalSessions > 0 ? totalCalories / totalSessions : 0);
    }

    @Tool(name = "analyze_calorie_consumption", value = "分析用户近期的热量消耗情况，按运动类型分组统计，并给出建议。")
    public String analyzeCalorieConsumption(
            @P("用户ID") Long userId,
            @P("分析天数，默认30天") Integer days) {
        int analyzeDays = days != null ? days : 30;
        LocalDate fromDate = LocalDate.now().minusDays(analyzeDays);

        List<ExerciseRecord> records = exerciseRecordMapper.selectList(
                new LambdaQueryWrapper<ExerciseRecord>()
                        .eq(ExerciseRecord::getUserId, userId)
                        .ge(ExerciseRecord::getExerciseDate, fromDate));

        if (records.isEmpty()) return "近" + analyzeDays + "天暂无运动记录。";

        int totalCalories = records.stream().mapToInt(r -> r.getCalories() != null ? r.getCalories() : 0).sum();
        int totalDuration = records.stream().mapToInt(r -> r.getDuration() != null ? r.getDuration() : 0).sum();

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("=== 近%d天热量消耗分析 ===\n", analyzeDays));
        sb.append(String.format("总消耗：%d 千卡\n", totalCalories));
        sb.append(String.format("总时长：%d 分钟\n", totalDuration));
        sb.append(String.format("日均消耗：%.0f 千卡\n\n", (double) totalCalories / analyzeDays));

        sb.append("按运动类型统计：\n");
        records.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        ExerciseRecord::getType,
                        java.util.stream.Collectors.summingInt(r -> r.getCalories() != null ? r.getCalories() : 0)))
                .entrySet().stream()
                .sorted((a, b) -> b.getValue() - a.getValue())
                .forEach(e -> sb.append(String.format("  %s：%d 千卡\n", e.getKey(), e.getValue())));

        if (totalCalories < analyzeDays * 200) {
            sb.append("\n建议：当前运动量偏低，建议每天至少消耗200千卡，可增加有氧运动如跑步、骑行等。");
        } else if (totalCalories > analyzeDays * 500) {
            sb.append("\n提示：运动量充足，注意适当休息和营养补充。");
        } else {
            sb.append("\n状态：运动量适中，保持当前节奏即可。");
        }

        return sb.toString();
    }

    @Tool(name = "query_exercise_records", value = "查询用户最近的运动记录列表")
    public String queryExerciseRecords(
            @P("用户ID") Long userId,
            @P("查询条数，默认5条") Integer limit) {
        int count = limit != null ? limit : 5;
        List<ExerciseRecord> records = exerciseRecordMapper.selectList(
                new LambdaQueryWrapper<ExerciseRecord>()
                        .eq(ExerciseRecord::getUserId, userId)
                        .orderByDesc(ExerciseRecord::getExerciseDate)
                        .last("LIMIT " + count));

        if (records.isEmpty()) return "暂无运动记录。";

        StringBuilder sb = new StringBuilder("=== 最近运动记录 ===\n");
        for (ExerciseRecord r : records) {
            sb.append(String.format("%s %s %d分钟 %.1fkm %dkcal\n",
                    r.getExerciseDate(),
                    r.getType(),
                    r.getDuration() != null ? r.getDuration() : 0,
                    r.getDistance() != null ? r.getDistance() : 0,
                    r.getCalories() != null ? r.getCalories() : 0));
        }
        return sb.toString();
    }
}
