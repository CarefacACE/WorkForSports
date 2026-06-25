package com.zhixun.erp.gym.service;

import com.zhixun.erp.gym.mapper.ProductPurchaseRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class GymFinanceService {

    private final ProductPurchaseRecordMapper purchaseRecordMapper;

    /**
     * 总览数据
     */
    public Map<String, Object> getOverview(String startDate, String endDate) {
        LocalDateTime[] range = parseDateRange(startDate, endDate);

        Map<String, Object> stats = purchaseRecordMapper.selectOverviewStats(range[0], range[1]);

        long productCount = ((Number) stats.getOrDefault("productCount", 0)).longValue();
        long totalSoldQty = ((Number) stats.getOrDefault("totalSoldQuantity", 0)).longValue();
        BigDecimal totalRevenue = toBigDecimal(stats.get("totalRevenue"));

        // 用 product-breakdown 的数据计算总成本
        List<Map<String, Object>> breakdown = purchaseRecordMapper.selectProductFinance(range[0], range[1]);
        BigDecimal totalCost = BigDecimal.ZERO;
        for (Map<String, Object> row : breakdown) {
            long qty = ((Number) row.getOrDefault("soldQuantity", 0)).longValue();
            BigDecimal unitCost = toBigDecimal(row.get("unitCost"));
            totalCost = totalCost.add(unitCost.multiply(BigDecimal.valueOf(qty)));
        }

        BigDecimal grossProfit = totalRevenue.subtract(totalCost);
        BigDecimal profitMargin = totalRevenue.compareTo(BigDecimal.ZERO) > 0
                ? grossProfit.divide(totalRevenue, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalRevenue", totalRevenue.setScale(2, RoundingMode.HALF_UP));
        result.put("totalCost", totalCost.setScale(2, RoundingMode.HALF_UP));
        result.put("grossProfit", grossProfit.setScale(2, RoundingMode.HALF_UP));
        result.put("profitMargin", profitMargin.setScale(2, RoundingMode.HALF_UP));
        result.put("totalSoldQuantity", totalSoldQty);
        result.put("productCount", productCount);
        return result;
    }

    /**
     * 商品维度分析
     */
    public List<Map<String, Object>> getProductBreakdown(String startDate, String endDate) {
        LocalDateTime[] range = parseDateRange(startDate, endDate);
        List<Map<String, Object>> rows = purchaseRecordMapper.selectProductFinance(range[0], range[1]);

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            long qty = ((Number) row.getOrDefault("soldQuantity", 0)).longValue();
            BigDecimal unitCost = toBigDecimal(row.get("unitCost"));
            BigDecimal totalRevenue = toBigDecimal(row.get("totalRevenue"));
            BigDecimal totalCost = unitCost.multiply(BigDecimal.valueOf(qty));
            BigDecimal profit = totalRevenue.subtract(totalCost);
            BigDecimal margin = totalRevenue.compareTo(BigDecimal.ZERO) > 0
                    ? profit.divide(totalRevenue, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                    : BigDecimal.ZERO;

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("productId", row.get("productId"));
            item.put("productName", row.get("productName"));
            item.put("soldQuantity", qty);
            item.put("totalRevenue", totalRevenue.setScale(2, RoundingMode.HALF_UP));
            item.put("unitCost", unitCost.setScale(2, RoundingMode.HALF_UP));
            item.put("totalCost", totalCost.setScale(2, RoundingMode.HALF_UP));
            item.put("profit", profit.setScale(2, RoundingMode.HALF_UP));
            item.put("profitMargin", margin.setScale(2, RoundingMode.HALF_UP));
            result.add(item);
        }
        return result;
    }

    /**
     * 月度趋势
     */
    public List<Map<String, Object>> getMonthlyTrend() {
        List<Map<String, Object>> rows = purchaseRecordMapper.selectMonthlyTrend();

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            BigDecimal revenue = toBigDecimal(row.get("revenue"));
            BigDecimal cost = toBigDecimal(row.get("cost"));
            BigDecimal profit = revenue.subtract(cost);

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("month", row.get("month"));
            item.put("revenue", revenue.setScale(2, RoundingMode.HALF_UP));
            item.put("cost", cost.setScale(2, RoundingMode.HALF_UP));
            item.put("profit", profit.setScale(2, RoundingMode.HALF_UP));
            result.add(item);
        }
        return result;
    }

    // ─── 工具方法 ───

    private LocalDateTime[] parseDateRange(String startDate, String endDate) {
        LocalDateTime start;
        LocalDateTime end;
        try {
            start = (startDate != null && !startDate.isEmpty())
                    ? LocalDate.parse(startDate, DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay()
                    : LocalDateTime.of(2000, 1, 1, 0, 0);
        } catch (Exception e) {
            start = LocalDateTime.of(2000, 1, 1, 0, 0);
        }
        try {
            end = (endDate != null && !endDate.isEmpty())
                    ? LocalDate.parse(endDate, DateTimeFormatter.ISO_LOCAL_DATE).atTime(LocalTime.MAX)
                    : LocalDateTime.of(2099, 12, 31, 23, 59, 59);
        } catch (Exception e) {
            end = LocalDateTime.of(2099, 12, 31, 23, 59, 59);
        }
        return new LocalDateTime[]{start, end};
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal) return (BigDecimal) value;
        if (value instanceof Number) return BigDecimal.valueOf(((Number) value).doubleValue());
        try {
            return new BigDecimal(value.toString());
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }
}
