package com.zhixun.erp.system.controller;

import com.zhixun.erp.common.response.Result;
import com.zhixun.erp.system.service.DbService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/db")
public class DbController {

    private final DbService dbService;

    @GetMapping("/tables")
    public Result<List<String>> getTables() {
        return Result.success(dbService.getTables());
    }

    @GetMapping("/table/{tableName}")
    public Result<List<Map<String, Object>>> getTableStructure(@PathVariable String tableName) {
        return Result.success(dbService.getTableStructure(tableName));
    }

    @GetMapping("/table/{tableName}/data")
    public Result<Map<String, Object>> getTableData(
            @PathVariable String tableName,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String keyword) {
        return Result.success(dbService.getTableData(tableName, pageNum, pageSize, keyword));
    }

    @PostMapping("/table/{tableName}/data")
    public Result<Void> insertRow(@PathVariable String tableName, @RequestBody Map<String, Object> row) {
        dbService.insertRow(tableName, row);
        return Result.success("新增成功", null);
    }

    @PutMapping("/table/{tableName}/data")
    public Result<Void> updateRow(
            @PathVariable String tableName,
            @RequestBody Map<String, Object> body) {
        String pkColumn = (String) body.remove("_pkColumn");
        Object pkValue = body.remove("_pkValue");
        if (pkColumn == null || pkValue == null) {
            return Result.fail(400, "缺少主键信息");
        }
        dbService.updateRow(tableName, body, pkColumn, pkValue);
        return Result.success("更新成功", null);
    }

    @DeleteMapping("/table/{tableName}/data/{pkColumn}/{pkValue}")
    public Result<Void> deleteRow(
            @PathVariable String tableName,
            @PathVariable String pkColumn,
            @PathVariable String pkValue) {
        dbService.deleteRow(tableName, pkColumn, pkValue);
        return Result.success("删除成功", null);
    }

    @DeleteMapping("/table/{tableName}/data/batch")
    public Result<Void> deleteRows(
            @PathVariable String tableName,
            @RequestBody Map<String, Object> body) {
        String pkColumn = (String) body.get("pkColumn");
        @SuppressWarnings("unchecked")
        List<Object> pkValues = (List<Object>) body.get("pkValues");
        if (pkColumn == null || pkValues == null) {
            return Result.fail(400, "缺少主键信息");
        }
        dbService.deleteRows(tableName, pkColumn, pkValues);
        return Result.success("批量删除成功", null);
    }
}
