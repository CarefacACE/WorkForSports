package com.zhixun.erp.gym.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhixun.erp.finance.entity.WalletTransaction;
import com.zhixun.erp.finance.mapper.WalletTransactionMapper;
import com.zhixun.erp.gym.entity.GymProduct;
import com.zhixun.erp.gym.entity.ProductPurchaseRecord;
import com.zhixun.erp.gym.entity.StockNotification;
import com.zhixun.erp.gym.mapper.GymProductMapper;
import com.zhixun.erp.gym.mapper.ProductPurchaseRecordMapper;
import com.zhixun.erp.gym.mapper.StockNotificationMapper;
import com.zhixun.erp.user.entity.User;
import com.zhixun.erp.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GymProductService {

    private final GymProductMapper gymProductMapper;
    private final ProductPurchaseRecordMapper purchaseRecordMapper;
    private final StockNotificationMapper stockNotificationMapper;
    private final UserMapper userMapper;
    private final WalletTransactionMapper walletTransactionMapper;

    /* ═══════════════════════════════════════════════════════════
       商品 CRUD（管理员）
       ═══════════════════════════════════════════════════════════ */

    public List<GymProduct> listProducts(String status) {
        LambdaQueryWrapper<GymProduct> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty()) {
            wrapper.eq(GymProduct::getStatus, status);
        }
        wrapper.orderByAsc(GymProduct::getCreateTime);
        return gymProductMapper.selectList(wrapper);
    }

    @Transactional
    public GymProduct createProduct(GymProduct input) {
        input.setCreateTime(LocalDateTime.now());
        if (input.getStatus() == null) input.setStatus("ACTIVE");
        if (input.getStock() == null) input.setStock(0);
        gymProductMapper.insert(input);
        return input;
    }

    @Transactional
    public GymProduct updateProduct(Long id, GymProduct input) {
        GymProduct existing = gymProductMapper.selectById(id);
        if (existing == null) throw new RuntimeException("商品不存在");
        if (input.getName() != null) existing.setName(input.getName());
        if (input.getDescription() != null) existing.setDescription(input.getDescription());
        if (input.getPrice() != null) existing.setPrice(input.getPrice());
        if (input.getImage() != null) existing.setImage(input.getImage());
        if (input.getStock() != null) existing.setStock(input.getStock());
        if (input.getStatus() != null) existing.setStatus(input.getStatus());
        if (input.getCost() != null) existing.setCost(input.getCost());
        existing.setUpdateTime(LocalDateTime.now());
        gymProductMapper.updateById(existing);
        return existing;
    }

    @Transactional
    public void deleteProduct(Long id) {
        GymProduct existing = gymProductMapper.selectById(id);
        if (existing == null) throw new RuntimeException("商品不存在");
        gymProductMapper.deleteById(id);
    }

    /* ═══════════════════════════════════════════════════════════
       会员/教练：购买商品
       ═══════════════════════════════════════════════════════════ */

    @Transactional
    public ProductPurchaseRecord purchaseProduct(Long userId, Long productId, Integer quantity) {
        GymProduct product = gymProductMapper.selectById(productId);
        if (product == null) throw new RuntimeException("商品不存在");
        if (!"ACTIVE".equals(product.getStatus())) throw new RuntimeException("该商品已下架");

        if (quantity == null || quantity < 1) quantity = 1;

        // 检查库存
        if (product.getStock() < quantity) {
            throw new RuntimeException("库存不足，当前库存 " + product.getStock() + "，需要 " + quantity);
        }

        User user = userMapper.selectById(userId);
        if (user == null) throw new RuntimeException("用户不存在");

        BigDecimal totalPrice = product.getPrice().multiply(BigDecimal.valueOf(quantity));
        if (user.getBalance() == null || user.getBalance().compareTo(totalPrice) < 0) {
            throw new RuntimeException("余额不足，需要 " + totalPrice + " 元，当前余额 " + user.getBalance());
        }

        // 扣款
        user.setBalance(user.getBalance().subtract(totalPrice));
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);

        // 扣减库存
        product.setStock(product.getStock() - quantity);
        product.setUpdateTime(LocalDateTime.now());
        gymProductMapper.updateById(product);

        // 记录交易流水
        WalletTransaction tx = new WalletTransaction();
        tx.setUserId(userId);
        tx.setAmount(totalPrice.negate());
        tx.setType("GYM_CONSUME");
        tx.setRemark("购买商品 - " + product.getName() + " x" + quantity);
        tx.setCreateTime(LocalDateTime.now());
        walletTransactionMapper.insert(tx);

        // 记录购买记录
        ProductPurchaseRecord record = new ProductPurchaseRecord();
        record.setUserId(userId);
        record.setProductId(productId);
        record.setProductName(product.getName());
        record.setQuantity(quantity);
        record.setUnitPrice(product.getPrice());
        record.setTotalPrice(totalPrice);
        record.setCreateTime(LocalDateTime.now());
        purchaseRecordMapper.insert(record);

        return record;
    }

    /* ═══════════════════════════════════════════════════════════
       缺货通知
       ═══════════════════════════════════════════════════════════ */

    @Transactional
    public StockNotification submitStockNotification(Long userId, Long productId) {
        GymProduct product = gymProductMapper.selectById(productId);
        if (product == null) throw new RuntimeException("商品不存在");

        // 检查是否已经提交过通知
        StockNotification existing = stockNotificationMapper.selectOne(
                new LambdaQueryWrapper<StockNotification>()
                        .eq(StockNotification::getUserId, userId)
                        .eq(StockNotification::getProductId, productId)
                        .eq(StockNotification::getDeleted, 0));
        if (existing != null) {
            throw new RuntimeException("您已经提交过该商品的缺货通知，请耐心等待补货");
        }

        StockNotification notification = new StockNotification();
        notification.setUserId(userId);
        notification.setProductId(productId);
        notification.setStatus("PENDING");
        notification.setCreateTime(LocalDateTime.now());
        stockNotificationMapper.insert(notification);
        return notification;
    }

    /**
     * 查询用户是否已对该商品提交缺货通知
     */
    public Map<String, Object> checkNotification(Long userId, Long productId) {
        StockNotification existing = stockNotificationMapper.selectOne(
                new LambdaQueryWrapper<StockNotification>()
                        .eq(StockNotification::getUserId, userId)
                        .eq(StockNotification::getProductId, productId)
                        .eq(StockNotification::getDeleted, 0));
        return java.util.Collections.singletonMap("submitted", existing != null);
    }

    /* ═══════════════════════════════════════════════════════════
       管理员：查看 / 处理缺货通知
       ═══════════════════════════════════════════════════════════ */

    /**
     * 管理员查看所有待处理的缺货通知（关联商品名 + 用户名）
     */
    public List<Map<String, Object>> listPendingNotifications() {
        List<StockNotification> list = stockNotificationMapper.selectList(
                new LambdaQueryWrapper<StockNotification>()
                        .eq(StockNotification::getStatus, "PENDING")
                        .eq(StockNotification::getDeleted, 0)
                        .orderByDesc(StockNotification::getCreateTime));

        return list.stream().map(n -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", n.getId());
            m.put("userId", n.getUserId());
            m.put("productId", n.getProductId());
            m.put("status", n.getStatus());
            m.put("createTime", n.getCreateTime());

            // 关联商品名
            GymProduct product = gymProductMapper.selectById(n.getProductId());
            m.put("productName", product != null ? product.getName() : "未知商品");

            // 关联用户信息
            User user = userMapper.selectById(n.getUserId());
            m.put("userName", user != null ? user.getRealName() : "未知用户");
            m.put("userRole", user != null ? user.getRole() : "");

            return m;
        }).collect(Collectors.toList());
    }

    /**
     * 管理员标记缺货通知为已处理（补货后调用）
     */
    @Transactional
    public void markNotified(Long notificationId) {
        StockNotification n = stockNotificationMapper.selectById(notificationId);
        if (n == null) throw new RuntimeException("通知不存在");
        n.setStatus("NOTIFIED");
        n.setUpdateTime(LocalDateTime.now());
        stockNotificationMapper.updateById(n);
    }

    /**
     * 获取我的购买记录
     */
    public List<ProductPurchaseRecord> getMyPurchaseRecords(Long userId) {
        return purchaseRecordMapper.selectList(
                new LambdaQueryWrapper<ProductPurchaseRecord>()
                        .eq(ProductPurchaseRecord::getUserId, userId)
                        .orderByDesc(ProductPurchaseRecord::getCreateTime));
    }
}
