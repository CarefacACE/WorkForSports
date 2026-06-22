package com.zhixun.erp.csv.service;

import com.zhixun.erp.csv.vo.CsvAnalysisResult;
import com.zhixun.erp.csv.vo.NumericColumn;
import com.zhixun.erp.file.service.FileService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CsvAnalysisService {

    private final FileService fileService;

    /** 判定某列为数值列的阈值：有效数字比例 > 50% */
    private static final double NUMERIC_THRESHOLD = 0.5;

    public CsvAnalysisResult analyze(MultipartFile file, Long userId, String username) throws Exception {
        // 自动保存文件
        if (userId != null && username != null) {
            fileService.uploadFile(file, userId, username);
        }

        // 第一遍读取：收集所有行（因为 CSV 流只能读一次，先全部缓存）
        List<CSVRecord> allRecords;
        List<String> headers;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT
                     .withFirstRecordAsHeader()
                     .withIgnoreHeaderCase()
                     .withTrim())) {

            headers = parser.getHeaderNames();
            allRecords = parser.getRecords();
        }

        if (allRecords.isEmpty()) {
            throw new RuntimeException("CSV 文件为空，无可分析数据");
        }

        // 能力探测：对每个表头列，检测是否为数值列
        List<NumericColumn> columns = new ArrayList<>();
        int maxRowCount = 0;

        for (String header : headers) {
            List<String> rawValues = new ArrayList<>();
            for (CSVRecord record : allRecords) {
                rawValues.add(record.get(header));
            }

            List<Double> numericData = tryParseNumericColumn(rawValues);
            if (numericData == null) {
                continue; // 不是数值列，跳过
            }

            maxRowCount = Math.max(maxRowCount, numericData.size());

            // 计算统计值
            double sum = 0;
            double max = Double.MIN_VALUE;
            double min = Double.MAX_VALUE;
            for (double v : numericData) {
                sum += v;
                if (v > max) max = v;
                if (v < min) min = v;
            }
            double avg = Math.round(sum / numericData.size() * 10.0) / 10.0;

            NumericColumn col = new NumericColumn();
            col.setTitle(header);
            col.setData(numericData);
            col.setAvg(avg);
            col.setMax(max);
            col.setMin(min);

            // 心率区间：仅当列名包含 heart 或 hr 时计算
            if (isHeartRateColumn(header)) {
                col.setZones(calculateZonesInt(numericData));
            }

            columns.add(col);
        }

        if (columns.isEmpty()) {
            throw new RuntimeException("未在 CSV 中找到任何数值列，请检查文件内容");
        }

        CsvAnalysisResult result = new CsvAnalysisResult();
        result.setTotalRecords(maxRowCount);
        result.setDurationSeconds(maxRowCount);
        result.setDurationFormatted(formatDuration(maxRowCount));
        result.setColumns(columns);
        return result;
    }

    /**
     * 尝试将一列字符串解析为数值列。
     * 如果有效数字占比 > NUMERIC_THRESHOLD，返回数值列表；否则返回 null。
     */
    private List<Double> tryParseNumericColumn(List<String> rawValues) {
        int nonEmptyCount = 0;
        int numericCount = 0;
        List<Double> result = new ArrayList<>();

        for (String raw : rawValues) {
            if (raw == null || raw.trim().isEmpty()) {
                continue;
            }
            nonEmptyCount++;
            try {
                result.add(Double.parseDouble(raw.trim()));
                numericCount++;
            } catch (NumberFormatException e) {
                // 非数字值，跳过
            }
        }

        if (nonEmptyCount == 0) {
            return null;
        }

        double ratio = (double) numericCount / nonEmptyCount;
        if (ratio > NUMERIC_THRESHOLD) {
            return result;
        }
        return null;
    }

    /**
     * 判断列名是否为心率相关列
     */
    private boolean isHeartRateColumn(String header) {
        String lower = header.toLowerCase();
        return lower.contains("heart") || lower.contains("hr");
    }

    /**
     * 心率区间计算（输入为 Double，转 int 处理）
     */
    private Map<String, Integer> calculateZonesInt(List<Double> data) {
        Map<String, Integer> zones = new LinkedHashMap<>();
        zones.put("热身区 (<120)", 0);
        zones.put("燃脂区 (120-140)", 0);
        zones.put("有氧区 (140-160)", 0);
        zones.put("无氧区 (160-180)", 0);
        zones.put("极限区 (>=180)", 0);

        for (double d : data) {
            int hr = (int) d;
            if (hr < 120) {
                zones.merge("热身区 (<120)", 1, Integer::sum);
            } else if (hr < 140) {
                zones.merge("燃脂区 (120-140)", 1, Integer::sum);
            } else if (hr < 160) {
                zones.merge("有氧区 (140-160)", 1, Integer::sum);
            } else if (hr < 180) {
                zones.merge("无氧区 (160-180)", 1, Integer::sum);
            } else {
                zones.merge("极限区 (>=180)", 1, Integer::sum);
            }
        }

        return zones;
    }

    private String formatDuration(int seconds) {
        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        int secs = seconds % 60;

        if (hours > 0) {
            return String.format("%d小时%d分%d秒", hours, minutes, secs);
        } else if (minutes > 0) {
            return String.format("%d分%d秒", minutes, secs);
        } else {
            return String.format("%d秒", secs);
        }
    }
}
