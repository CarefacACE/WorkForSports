package com.zhixun.erp.csv.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NumericColumn {

    /** 列标题，即 CSV 表头原名，如 "HeartRate", "Cadence", "Elevation" */
    private String title;

    /** 该列的原始数值列表 */
    private List<Double> data;

    /** 平均值，保留 1 位小数 */
    private Double avg;

    /** 最大值 */
    private Double max;

    /** 最小值 */
    private Double min;

    /** 心率区间分布（仅当 title 包含 "heart" 或 "hr" 时填充，非心率列为 null） */
    private Map<String, Integer> zones;
}
