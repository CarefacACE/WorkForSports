package com.zhixun.erp.system.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class DbService {

    private final JdbcTemplate jdbcTemplate;

    private static final String DB_NAME = "zhixun_erp";

    public List<String> getTables() {
        String sql = "SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_SCHEMA = ? ORDER BY TABLE_NAME";
        return jdbcTemplate.queryForList(sql, String.class, DB_NAME);
    }

    public List<Map<String, Object>> getTableStructure(String tableName) {
        validateTableName(tableName);
        String sql = "SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_KEY, COLUMN_DEFAULT, COLUMN_COMMENT " +
                "FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? ORDER BY ORDINAL_POSITION";
        return jdbcTemplate.queryForList(sql, DB_NAME, tableName);
    }

    public Map<String, Object> getTableData(String tableName, int pageNum, int pageSize, String keyword) {
        validateTableName(tableName);

        String countSql;
        String dataSql;
        List<Object> countParams = new ArrayList<>();
        List<Object> dataParams = new ArrayList<>();

        if (keyword != null && !keyword.isEmpty()) {
            List<String> searchableColumns = getSearchableColumns(tableName);
            if (searchableColumns.isEmpty()) {
                countSql = "SELECT COUNT(*) FROM `" + tableName + "`";
                dataSql = "SELECT * FROM `" + tableName + "`";
            } else {
                StringBuilder where = new StringBuilder(" WHERE ");
                for (int i = 0; i < searchableColumns.size(); i++) {
                    if (i > 0) where.append(" OR ");
                    where.append("`").append(searchableColumns.get(i)).append("` LIKE ?");
                    String likeValue = "%" + keyword + "%";
                    countParams.add(likeValue);
                    dataParams.add(likeValue);
                }
                countSql = "SELECT COUNT(*) FROM `" + tableName + "`" + where;
                dataSql = "SELECT * FROM `" + tableName + "`" + where;
            }
        } else {
            countSql = "SELECT COUNT(*) FROM `" + tableName + "`";
            dataSql = "SELECT * FROM `" + tableName + "`";
        }

        Long total = jdbcTemplate.queryForObject(countSql, Long.class, countParams.toArray());

        int offset = (pageNum - 1) * pageSize;
        dataSql += " LIMIT " + pageSize + " OFFSET " + offset;
        List<Map<String, Object>> records = jdbcTemplate.queryForList(dataSql, dataParams.toArray());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", records);
        result.put("total", total != null ? total : 0);
        result.put("size", pageSize);
        result.put("current", pageNum);
        return result;
    }

    public void insertRow(String tableName, Map<String, Object> row) {
        validateTableName(tableName);
        if (row == null || row.isEmpty()) {
            throw new RuntimeException("数据不能为空");
        }

        StringBuilder sql = new StringBuilder("INSERT INTO `").append(tableName).append("` (");
        StringBuilder values = new StringBuilder(" VALUES (");
        List<Object> params = new ArrayList<>();

        int i = 0;
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            if (i > 0) {
                sql.append(", ");
                values.append(", ");
            }
            sql.append("`").append(entry.getKey()).append("`");
            values.append("?");
            params.add(entry.getValue());
            i++;
        }
        sql.append(")").append(values).append(")");

        jdbcTemplate.update(sql.toString(), params.toArray());
    }

    public void updateRow(String tableName, Map<String, Object> row, String pkColumn, Object pkValue) {
        validateTableName(tableName);
        if (row == null || row.isEmpty()) {
            throw new RuntimeException("数据不能为空");
        }

        StringBuilder sql = new StringBuilder("UPDATE `").append(tableName).append("` SET ");
        List<Object> params = new ArrayList<>();

        int i = 0;
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            if (entry.getKey().equals(pkColumn)) continue;
            if (i > 0) sql.append(", ");
            sql.append("`").append(entry.getKey()).append("` = ?");
            params.add(entry.getValue());
            i++;
        }
        sql.append(" WHERE `").append(pkColumn).append("` = ?");
        params.add(pkValue);

        jdbcTemplate.update(sql.toString(), params.toArray());
    }

    public void deleteRow(String tableName, String pkColumn, Object pkValue) {
        validateTableName(tableName);
        String sql = "DELETE FROM `" + tableName + "` WHERE `" + pkColumn + "` = ?";
        jdbcTemplate.update(sql, pkValue);
    }

    public void deleteRows(String tableName, String pkColumn, List<Object> pkValues) {
        validateTableName(tableName);
        for (Object pkValue : pkValues) {
            deleteRow(tableName, pkColumn, pkValue);
        }
    }

    private List<String> getSearchableColumns(String tableName) {
        String sql = "SELECT COLUMN_NAME FROM information_schema.COLUMNS " +
                "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? AND DATA_TYPE IN ('varchar','char','text','longtext','mediumtext','tinytext')";
        return jdbcTemplate.queryForList(sql, String.class, DB_NAME, tableName);
    }

    private void validateTableName(String tableName) {
        List<String> tables = getTables();
        if (!tables.contains(tableName)) {
            throw new RuntimeException("表不存在: " + tableName);
        }
    }
}
