package com.zhixun.erp.exercise.controller;

import com.zhixun.erp.common.response.Result;
import com.zhixun.erp.exercise.entity.ExerciseRecord;
import com.zhixun.erp.exercise.service.ExerciseService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@CrossOrigin
@RestController
@RequiredArgsConstructor
@RequestMapping("/exercise")
public class ExerciseController {

    private final ExerciseService exerciseService;

    @GetMapping("/list")
    public Result<List<ExerciseRecord>> getRecords(
            @RequestParam Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return Result.success(exerciseService.getRecords(userId, from, to));
    }

    @GetMapping("/stats")
    public Result<Map<String, Object>> getStats(
            @RequestParam Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return Result.success(exerciseService.getStats(userId, from, to));
    }

    @GetMapping("/trend")
    public Result<List<Map<String, Object>>> getTrend(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "30") int days) {
        return Result.success(exerciseService.getTrend(userId, days));
    }

    @PostMapping
    public Result<ExerciseRecord> addRecord(
            @RequestParam Long userId,
            @RequestBody ExerciseRecord record) {
        return Result.success("记录已添加", exerciseService.addRecord(userId, record));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteRecord(@PathVariable Long id, @RequestParam Long userId) {
        exerciseService.deleteRecord(userId, id);
        return Result.success("已删除", null);
    }
}
