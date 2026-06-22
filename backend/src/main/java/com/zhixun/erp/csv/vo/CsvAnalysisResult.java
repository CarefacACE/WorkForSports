package com.zhixun.erp.csv.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CsvAnalysisResult {

    /** CSV 总行数（以第一个数值列的行数为准） */
    private Integer totalRecords;

    /** 时长（秒），等价于 totalRecords（每秒一条记录） */
    private Integer durationSeconds;

    /** 格式化时长，如 "12分34秒" */
    private String durationFormatted;

    /** 所有探测到的数值列，前端据此动态渲染图表 */
    private List<NumericColumn> columns;
}
