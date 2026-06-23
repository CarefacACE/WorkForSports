package com.zhixun.erp.checkin.controller;

import com.zhixun.erp.checkin.entity.CheckInRecord;
import com.zhixun.erp.checkin.service.CheckInService;
import com.zhixun.erp.common.response.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@CrossOrigin
@RestController
@RequiredArgsConstructor
@RequestMapping("/checkin")
public class CheckInController {

    private final CheckInService checkInService;

    @PostMapping
    public Result<CheckInRecord> checkIn(
            @RequestParam Long scheduleId,
            @RequestParam Long userId,
            @RequestParam String role) {
        return Result.success("签到成功", checkInService.checkIn(scheduleId, userId, role));
    }

    @GetMapping("/status")
    public Result<CheckInRecord> getCheckInStatus(
            @RequestParam Long scheduleId,
            @RequestParam Long userId,
            @RequestParam String role) {
        return Result.success(checkInService.getCheckInStatus(scheduleId, userId, role));
    }

    @GetMapping("/schedule/{scheduleId}")
    public Result<List<CheckInRecord>> getScheduleCheckIns(@PathVariable Long scheduleId) {
        return Result.success(checkInService.getScheduleCheckIns(scheduleId));
    }

    @GetMapping("/history")
    public Result<List<CheckInRecord>> getCheckInHistory(
            @RequestParam Long userId,
            @RequestParam String role,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return Result.success(checkInService.getUserCheckInHistory(userId, role, from, to));
    }

    @GetMapping("/stats")
    public Result<Map<String, Object>> getCheckInStats(
            @RequestParam Long userId,
            @RequestParam String role) {
        return Result.success(checkInService.getCheckInStats(userId, role));
    }
}
