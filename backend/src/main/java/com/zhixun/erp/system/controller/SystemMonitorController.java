package com.zhixun.erp.system.controller;

import com.zhixun.erp.common.response.Result;
import com.zhixun.erp.system.service.SystemMonitorService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/systemMonitor")
public class SystemMonitorController {

    private final SystemMonitorService monitorService;

    @GetMapping("/info")
    public Result<Map<String, Object>> getSystemInfo() {
        Map<String, Object> info = monitorService.getSystemInfo();
        return Result.success(info);
    }
}
