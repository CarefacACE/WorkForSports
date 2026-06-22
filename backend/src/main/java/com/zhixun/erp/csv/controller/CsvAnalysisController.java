package com.zhixun.erp.csv.controller;

import com.zhixun.erp.common.response.Result;
import com.zhixun.erp.csv.service.CsvAnalysisService;
import com.zhixun.erp.csv.vo.CsvAnalysisResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@CrossOrigin
@RestController
@RequiredArgsConstructor
@RequestMapping("/csv")
public class CsvAnalysisController {

    private final CsvAnalysisService csvAnalysisService;

    @PostMapping("/analyze")
    public Result<CsvAnalysisResult> analyze(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestParam(value = "username", required = false) String username) throws Exception {
        if (file.isEmpty()) {
            return Result.fail(400, "请选择文件");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".csv")) {
            return Result.fail(400, "仅支持CSV文件");
        }

        return Result.success("分析完成", csvAnalysisService.analyze(file, userId, username));
    }
}
