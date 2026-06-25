package com.zhixun.erp.gym.controller;

import com.zhixun.erp.common.response.Result;
import com.zhixun.erp.gym.service.GymFinanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@CrossOrigin
@RestController
@RequiredArgsConstructor
@RequestMapping("/gym/finance")
public class GymFinanceController {

    private final GymFinanceService gymFinanceService;

    /**
     * 收支总览
     */
    @GetMapping("/overview")
    public Result<Map<String, Object>> getOverview(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return Result.success(gymFinanceService.getOverview(startDate, endDate));
    }

    /**
     * 商品维度收支分析
     */
    @GetMapping("/product-breakdown")
    public Result<List<Map<String, Object>>> getProductBreakdown(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return Result.success(gymFinanceService.getProductBreakdown(startDate, endDate));
    }

    /**
     * 月度趋势
     */
    @GetMapping("/monthly-trend")
    public Result<List<Map<String, Object>>> getMonthlyTrend() {
        return Result.success(gymFinanceService.getMonthlyTrend());
    }
}
