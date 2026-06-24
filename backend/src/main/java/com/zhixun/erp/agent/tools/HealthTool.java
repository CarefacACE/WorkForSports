package com.zhixun.erp.agent.tools;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhixun.erp.exercise.entity.ExerciseRecord;
import com.zhixun.erp.exercise.mapper.ExerciseRecordMapper;
import com.zhixun.erp.health.entity.MemberHealthProfile;
import com.zhixun.erp.health.mapper.MemberHealthProfileMapper;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
public class HealthTool {

    private final MemberHealthProfileMapper healthProfileMapper;
    private final ExerciseRecordMapper exerciseRecordMapper;

    @Tool(name = "query_health_profile", value = "查询用户的健康档案，包括身高、体重、BMI、体脂率、血压、健身目标等信息。")
    public String queryHealthProfile(@P("用户ID") Long userId) {
        MemberHealthProfile profile = healthProfileMapper.selectOne(
                new LambdaQueryWrapper<MemberHealthProfile>()
                        .eq(MemberHealthProfile::getUserId, userId));

        if (profile == null) {
            return "暂未建立健康档案，请先在个人信息页面填写健康数据。";
        }

        StringBuilder sb = new StringBuilder("=== 健康档案 ===\n");

        if (profile.getHeight() != null && profile.getWeight() != null) {
            double heightM = profile.getHeight().doubleValue() / 100.0;
            double bmi = profile.getWeight().doubleValue() / (heightM * heightM);
            String bmiLevel;
            if (bmi < 18.5) bmiLevel = "偏瘦";
            else if (bmi < 24) bmiLevel = "正常";
            else if (bmi < 28) bmiLevel = "偏胖";
            else bmiLevel = "肥胖";
            sb.append(String.format("身高：%.1f cm，体重：%.1f kg，BMI：%.1f（%s）\n",
                    profile.getHeight(), profile.getWeight(), bmi, bmiLevel));
        }
        if (profile.getBodyFat() != null) sb.append(String.format("体脂率：%.1f%%\n", profile.getBodyFat()));
        if (profile.getMuscleMass() != null) sb.append(String.format("肌肉量：%.1f kg\n", profile.getMuscleMass()));
        if (profile.getBpSystolic() != null && profile.getBpDiastolic() != null)
            sb.append(String.format("血压：%d/%d mmHg\n", profile.getBpSystolic(), profile.getBpDiastolic()));
        if (profile.getRestingHeartRate() != null) sb.append(String.format("静息心率：%d bpm\n", profile.getRestingHeartRate()));
        if (profile.getBloodType() != null) sb.append("血型：").append(profile.getBloodType()).append("\n");

        if (profile.getFitnessGoal() != null) {
            sb.append("\n--- 健身目标 ---\n");
            sb.append("目标：").append(profile.getFitnessGoal()).append("\n");
            if (profile.getTargetWeight() != null) sb.append(String.format("目标体重：%.1f kg\n", profile.getTargetWeight()));
            if (profile.getTargetBodyFat() != null) sb.append(String.format("目标体脂率：%.1f%%\n", profile.getTargetBodyFat()));
            if (profile.getWeeklyWorkoutFreq() != null) sb.append(String.format("每周训练频率：%d 次\n", profile.getWeeklyWorkoutFreq()));
        }

        return sb.toString();
    }

    @Tool(name = "generate_workout_plan", value = "根据用户的身体数据、健身目标和运动历史，生成个性化的锻炼计划建议。")
    public String generateWorkoutPlan(@P("用户ID") Long userId) {
        MemberHealthProfile profile = healthProfileMapper.selectOne(
                new LambdaQueryWrapper<MemberHealthProfile>()
                        .eq(MemberHealthProfile::getUserId, userId));

        List<ExerciseRecord> recentRecords = exerciseRecordMapper.selectList(
                new LambdaQueryWrapper<ExerciseRecord>()
                        .eq(ExerciseRecord::getUserId, userId)
                        .orderByDesc(ExerciseRecord::getExerciseDate)
                        .last("LIMIT 10"));

        StringBuilder sb = new StringBuilder("=== 个性化锻炼计划建议 ===\n\n");

        // 分析当前状态
        if (profile != null && profile.getHeight() != null && profile.getWeight() != null) {
            double heightM = profile.getHeight().doubleValue() / 100.0;
            double bmi = profile.getWeight().doubleValue() / (heightM * heightM);

            sb.append("【当前状态分析】\n");
            if (bmi < 18.5) {
                sb.append("体重偏轻，建议以力量训练为主，增加肌肉量。\n\n");
                sb.append("【推荐计划】\n");
                sb.append("周一：胸部+三头肌（卧推、飞鸟、臂屈伸）\n");
                sb.append("周二：休息或轻度有氧30分钟\n");
                sb.append("周三：背部+二头肌（引体向上、划船、弯举）\n");
                sb.append("周四：休息\n");
                sb.append("周五：腿部+肩部（深蹲、硬拉、推举）\n");
                sb.append("周六：有氧运动（跑步/骑行40分钟）\n");
                sb.append("周日：休息\n");
                sb.append("\n饮食建议：每日摄入热量应高于消耗，蛋白质每公斤体重1.6-2g。");
            } else if (bmi < 24) {
                sb.append("体重正常，建议力量+有氧结合，提升体能。\n\n");
                sb.append("【推荐计划】\n");
                sb.append("周一：HIIT训练30分钟\n");
                sb.append("周二：上肢力量训练\n");
                sb.append("周三：有氧运动（跑步/游泳45分钟）\n");
                sb.append("周四：休息或瑜伽\n");
                sb.append("周五：下肢力量训练\n");
                sb.append("周六：户外运动（骑行/登山）\n");
                sb.append("周日：休息\n");
            } else {
                sb.append("体重偏高，建议以有氧运动为主，控制饮食。\n\n");
                sb.append("【推荐计划】\n");
                sb.append("周一：快走/慢跑40分钟\n");
                sb.append("周二：全身力量训练（轻重量高次数）\n");
                sb.append("周三：游泳/骑行45分钟\n");
                sb.append("周四：休息\n");
                sb.append("周五：HIIT训练25分钟\n");
                sb.append("周六：户外徒步60分钟\n");
                sb.append("周日：休息或瑜伽放松\n");
                sb.append("\n饮食建议：控制碳水摄入，增加蔬菜和蛋白质比例，避免高糖高脂食物。");
            }
        } else {
            sb.append("暂无健康档案数据，建议先填写身体信息以获得更精准的计划。\n\n");
            sb.append("【通用建议】\n");
            sb.append("每周3-5次运动，每次30-60分钟，有氧+力量结合。");
        }

        // 结合运动历史给出建议
        if (!recentRecords.isEmpty()) {
            int avgDuration = recentRecords.stream().mapToInt(r -> r.getDuration() != null ? r.getDuration() : 0).sum() / recentRecords.size();
            sb.append(String.format("\n\n【基于运动历史】您近期平均每次运动%d分钟", avgDuration));
            if (avgDuration < 30) {
                sb.append("，建议逐步增加到30-45分钟以获得更好的效果。");
            } else if (avgDuration > 90) {
                sb.append("，运动时间较长，注意避免过度训练。");
            } else {
                sb.append("，运动时长适中，保持即可。");
            }
        }

        return sb.toString();
    }
}
