package com.zhixun.erp.gym.controller;

import com.zhixun.erp.common.response.Result;
import com.zhixun.erp.gym.entity.GymProduct;
import com.zhixun.erp.gym.entity.ProductPurchaseRecord;
import com.zhixun.erp.gym.entity.StockNotification;
import com.zhixun.erp.gym.service.GymProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@CrossOrigin
@RestController
@RequiredArgsConstructor
@RequestMapping("/gym/product")
public class GymProductController {

    private final GymProductService gymProductService;

    /* ─── 商品 CRUD（管理员） ─── */

    @GetMapping("/list")
    public Result<List<GymProduct>> listProducts(
            @RequestParam(required = false) String status) {
        return Result.success(gymProductService.listProducts(status));
    }

    @PostMapping("/create")
    public Result<GymProduct> createProduct(@RequestBody GymProduct product) {
        return Result.success("创建成功", gymProductService.createProduct(product));
    }

    @PutMapping("/update")
    public Result<GymProduct> updateProduct(@RequestBody GymProduct product) {
        return Result.success("更新成功", gymProductService.updateProduct(product.getId(), product));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteProduct(@PathVariable Long id) {
        gymProductService.deleteProduct(id);
        return Result.success("删除成功", null);
    }

    /* ─── 会员/教练：购买商品 ─── */

    @PostMapping("/purchase")
    public Result<ProductPurchaseRecord> purchaseProduct(
            @RequestParam Long userId,
            @RequestParam Long productId,
            @RequestParam(defaultValue = "1") Integer quantity) {
        return Result.success("购买成功", gymProductService.purchaseProduct(userId, productId, quantity));
    }

    /* ─── 缺货通知 ─── */

    @PostMapping("/notify-stock")
    public Result<StockNotification> submitStockNotification(
            @RequestParam Long userId,
            @RequestParam Long productId) {
        return Result.success("缺货通知已提交，补货后将第一时间通知您",
                gymProductService.submitStockNotification(userId, productId));
    }

    @GetMapping("/check-notification")
    public Result<Map<String, Object>> checkNotification(
            @RequestParam Long userId,
            @RequestParam Long productId) {
        return Result.success(gymProductService.checkNotification(userId, productId));
    }

    /* ─── 管理员：查看 / 处理缺货通知 ─── */

    @GetMapping("/pending-notifications")
    public Result<List<Map<String, Object>>> listPendingNotifications() {
        return Result.success(gymProductService.listPendingNotifications());
    }

    @PutMapping("/notify-stock/{id}")
    public Result<Void> markNotified(@PathVariable Long id) {
        gymProductService.markNotified(id);
        return Result.success("已标记为已通知", null);
    }

    /* ─── 购买记录 ─── */

    @GetMapping("/my-purchases")
    public Result<List<ProductPurchaseRecord>> getMyPurchaseRecords(@RequestParam Long userId) {
        return Result.success(gymProductService.getMyPurchaseRecords(userId));
    }
}
