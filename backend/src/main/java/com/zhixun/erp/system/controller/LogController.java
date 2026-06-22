package com.zhixun.erp.system.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zhixun.erp.common.response.Result;
import com.zhixun.erp.system.entity.SysLog;
import com.zhixun.erp.system.service.LogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/log")
public class LogController {

    private final LogService logService;

    @GetMapping("/list")
    public Result<IPage<SysLog>> listLogs(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String operationType,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        IPage<SysLog> page = logService.listLogs(pageNum, pageSize, keyword, role, operationType, startTime, endTime);
        return Result.success(page);
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteLog(@PathVariable Long id) {
        logService.deleteLog(id);
        return Result.success("删除成功", null);
    }

    @DeleteMapping("/batch")
    public Result<Void> deleteLogs(@RequestBody List<Long> ids) {
        logService.deleteLogs(ids);
        return Result.success("批量删除成功", null);
    }
}
