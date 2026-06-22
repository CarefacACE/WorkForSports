package com.zhixun.erp.exercise.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhixun.erp.exercise.entity.ExerciseRecord;
import com.zhixun.erp.exercise.mapper.ExerciseRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExerciseService {

    private final ExerciseRecordMapper recordMapper;

    public List<ExerciseRecord> getRecords(Long userId, LocalDate from, LocalDate to) {
        LambdaQueryWrapper<ExerciseRecord> wrapper = new LambdaQueryWrapper<ExerciseRecord>()
                .eq(ExerciseRecord::getUserId, userId);
        if (from != null) wrapper.ge(ExerciseRecord::getExerciseDate, from);
        if (to != null) wrapper.le(ExerciseRecord::getExerciseDate, to);
        wrapper.orderByDesc(ExerciseRecord::getExerciseDate);
        return recordMapper.selectList(wrapper);
    }

    public Map<String, Object> getStats(Long userId, LocalDate from, LocalDate to) {
        List<ExerciseRecord> records = getRecords(userId, from, to);

        int totalDuration = records.stream().mapToInt(r -> r.getDuration() != null ? r.getDuration() : 0).sum();
        BigDecimal totalDistance = records.stream()
                .map(r -> r.getDistance() != null ? r.getDistance() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int totalCalories = records.stream().mapToInt(r -> r.getCalories() != null ? r.getCalories() : 0).sum();
        long totalDays = records.stream().map(ExerciseRecord::getExerciseDate).distinct().count();
        int totalSessions = records.size();

        BigDecimal outdoorRunning = records.stream()
                .filter(r -> "RUNNING_OUTDOOR".equals(r.getType()))
                .map(r -> r.getDistance() != null ? r.getDistance() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal indoorRunning = records.stream()
                .filter(r -> "RUNNING_INDOOR".equals(r.getType()))
                .map(r -> r.getDistance() != null ? r.getDistance() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalRunning = outdoorRunning.add(indoorRunning);

        // 平均配速（仅跑步类）
        String avgPace = "-";
        List<ExerciseRecord> runningRecords = records.stream()
                .filter(r -> r.getType() != null && r.getType().startsWith("RUNNING") && r.getDistance() != null
                        && r.getDistance().compareTo(BigDecimal.ZERO) > 0 && r.getDuration() != null && r.getDuration() > 0)
                .collect(Collectors.toList());
        if (!runningRecords.isEmpty()) {
            double totalMin = runningRecords.stream().mapToInt(ExerciseRecord::getDuration).sum();
            double totalKm = runningRecords.stream().map(ExerciseRecord::getDistance).reduce(BigDecimal.ZERO, BigDecimal::add).doubleValue();
            if (totalKm > 0) {
                double paceMin = totalMin / totalKm;
                int m = (int) paceMin;
                int s = (int) ((paceMin - m) * 60);
                avgPace = m + "'" + String.format("%02d", s);
            }
        }

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalDuration", totalDuration);
        stats.put("totalDistance", totalDistance);
        stats.put("totalCalories", totalCalories);
        stats.put("totalDays", totalDays);
        stats.put("totalSessions", totalSessions);
        stats.put("avgDuration", totalSessions > 0 ? totalDuration / totalSessions : 0);
        stats.put("avgCalories", totalSessions > 0 ? totalCalories / totalSessions : 0);
        stats.put("totalRunning", totalRunning);
        stats.put("outdoorRunning", outdoorRunning);
        stats.put("indoorRunning", indoorRunning);
        stats.put("avgPace", avgPace);
        return stats;
    }

    public List<Map<String, Object>> getTrend(Long userId, int days) {
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(days - 1);
        List<ExerciseRecord> records = getRecords(userId, from, to);

        Map<LocalDate, List<ExerciseRecord>> byDate = records.stream()
                .collect(Collectors.groupingBy(ExerciseRecord::getExerciseDate));

        List<Map<String, Object>> trend = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            List<ExerciseRecord> dayRecords = byDate.getOrDefault(d, Collections.emptyList());
            int duration = dayRecords.stream().mapToInt(r -> r.getDuration() != null ? r.getDuration() : 0).sum();
            int calories = dayRecords.stream().mapToInt(r -> r.getCalories() != null ? r.getCalories() : 0).sum();
            double distance = dayRecords.stream()
                    .map(r -> r.getDistance() != null ? r.getDistance() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add).doubleValue();

            Map<String, Object> point = new LinkedHashMap<>();
            point.put("date", d.toString());
            point.put("duration", duration);
            point.put("calories", calories);
            point.put("distance", distance);
            trend.add(point);
        }
        return trend;
    }

    @Transactional
    public ExerciseRecord addRecord(Long userId, ExerciseRecord input) {
        input.setUserId(userId);
        input.setCreateTime(LocalDateTime.now());
        recordMapper.insert(input);
        return input;
    }

    @Transactional
    public void deleteRecord(Long userId, Long recordId) {
        ExerciseRecord existing = recordMapper.selectById(recordId);
        if (existing == null || !existing.getUserId().equals(userId)) {
            throw new RuntimeException("只能删除自己的记录");
        }
        recordMapper.deleteById(recordId);
    }
}
