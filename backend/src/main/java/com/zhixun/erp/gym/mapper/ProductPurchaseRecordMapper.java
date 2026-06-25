package com.zhixun.erp.gym.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhixun.erp.gym.entity.ProductPurchaseRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface ProductPurchaseRecordMapper extends BaseMapper<ProductPurchaseRecord> {

    /**
     * 按商品维度聚合：销量、营收（关联成本价）
     */
    @Select("SELECT " +
            "  p.id AS productId, " +
            "  p.name AS productName, " +
            "  p.cost AS unitCost, " +
            "  SUM(pr.quantity) AS soldQuantity, " +
            "  SUM(pr.total_price) AS totalRevenue " +
            "FROM product_purchase_record pr " +
            "JOIN gym_product p ON pr.product_id = p.id " +
            "WHERE pr.deleted = 0 AND p.deleted = 0 " +
            "  AND pr.create_time >= #{start} AND pr.create_time < #{end} " +
            "GROUP BY p.id, p.name, p.cost " +
            "ORDER BY totalRevenue DESC")
    List<Map<String, Object>> selectProductFinance(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    /**
     * 月度趋势：按月聚合营收与成本
     */
    @Select("SELECT " +
            "  DATE_FORMAT(pr.create_time, '%Y-%m') AS month, " +
            "  SUM(pr.total_price) AS revenue, " +
            "  SUM(pr.quantity * COALESCE(p.cost, 0)) AS cost " +
            "FROM product_purchase_record pr " +
            "JOIN gym_product p ON pr.product_id = p.id " +
            "WHERE pr.deleted = 0 AND p.deleted = 0 " +
            "GROUP BY DATE_FORMAT(pr.create_time, '%Y-%m') " +
            "ORDER BY month ASC")
    List<Map<String, Object>> selectMonthlyTrend();

    /**
     * 日期范围内的总营收、总销量
     */
    @Select("SELECT " +
            "  COUNT(DISTINCT pr.product_id) AS productCount, " +
            "  COALESCE(SUM(pr.quantity), 0) AS totalSoldQuantity, " +
            "  COALESCE(SUM(pr.total_price), 0) AS totalRevenue " +
            "FROM product_purchase_record pr " +
            "WHERE pr.deleted = 0 " +
            "  AND pr.create_time >= #{start} AND pr.create_time < #{end}")
    Map<String, Object> selectOverviewStats(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);
}
