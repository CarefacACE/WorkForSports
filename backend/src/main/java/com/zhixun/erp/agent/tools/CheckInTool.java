package com.zhixun.erp.agent.tools;

import com.zhixun.erp.checkin.service.CheckInService;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class CheckInTool {

    private final CheckInService checkInService;

    @Tool(name = "query_checkin_stats", value = "查询用户的签到统计数据，包括总排课数、已签到次数、缺勤次数、签到率等。")
    public String queryCheckInStats(
            @P("用户ID") Long userId,
            @P("用户角色：COACH或MEMBER") String role) {
        Map<String, Object> stats = checkInService.getCheckInStats(userId, role);

        long totalRecords = (long) stats.getOrDefault("totalRecords", 0L);
        long signedCount = (long) stats.getOrDefault("signedCount", 0L);
        long absentCount = (long) stats.getOrDefault("absentCount", 0L);
        long pendingCount = (long) stats.getOrDefault("pendingCount", 0L);
        double checkInRate = (double) stats.getOrDefault("checkInRate", 0.0);

        if (totalRecords == 0) {
            return "暂无签到记录。";
        }

        StringBuilder sb = new StringBuilder("=== 签到统计 ===\n");
        sb.append(String.format("总排课数：%d 次\n", totalRecords));
        sb.append(String.format("已签到：%d 次\n", signedCount));
        sb.append(String.format("缺勤：%d 次\n", absentCount));
        sb.append(String.format("待签到：%d 次\n", pendingCount));
        sb.append(String.format("签到率：%.1f%%\n", checkInRate));

        if (checkInRate >= 90) {
            sb.append("\n表现优秀，继续保持！");
        } else if (checkInRate >= 70) {
            sb.append("\n签到率不错，还有提升空间。");
        } else {
            sb.append("\n签到率偏低，建议提高出勤率以获得更好的训练效果。");
        }

        return sb.toString();
    }
}
