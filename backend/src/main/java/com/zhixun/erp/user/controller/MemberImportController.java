package com.zhixun.erp.user.controller;

import com.zhixun.erp.common.response.Result;
import com.zhixun.erp.user.service.MemberImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@CrossOrigin
@RestController
@RequiredArgsConstructor
@RequestMapping("/user")
public class MemberImportController {

    private final MemberImportService memberImportService;

    @PostMapping("/import-members")
    public Result<Map<String, Object>> importMembers(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.fail(400, "请选择 CSV 文件");
        }
        Map<String, Object> result = memberImportService.importMembers(file);
        return Result.success("导入完成", result);
    }
}
