package com.zhixun.erp.finance.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhixun.erp.finance.entity.WalletTransaction;
import com.zhixun.erp.finance.mapper.WalletTransactionMapper;
import com.zhixun.erp.user.entity.User;
import com.zhixun.erp.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FinanceService {

    private final UserMapper userMapper;
    private final WalletTransactionMapper walletTransactionMapper;

    @Transactional
    public User recharge(Long userId, BigDecimal amount, String remark) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("充值金额必须大于0");
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        BigDecimal currentBalance = user.getBalance() == null ? BigDecimal.ZERO : user.getBalance();
        user.setBalance(currentBalance.add(amount));
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);

        WalletTransaction transaction = new WalletTransaction();
        transaction.setUserId(userId);
        transaction.setAmount(amount);
        transaction.setType("RECHARGE");
        transaction.setRemark(remark);
        transaction.setCreateTime(LocalDateTime.now());
        walletTransactionMapper.insert(transaction);

        return user;
    }

    @Transactional
    public User withdraw(Long userId, BigDecimal amount, String remark) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("提现金额必须大于0");
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        BigDecimal currentBalance = user.getBalance() == null ? BigDecimal.ZERO : user.getBalance();
        if (currentBalance.compareTo(amount) < 0) {
            throw new RuntimeException("余额不足，当前余额: " + currentBalance);
        }

        user.setBalance(currentBalance.subtract(amount));
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);

        WalletTransaction transaction = new WalletTransaction();
        transaction.setUserId(userId);
        transaction.setAmount(amount);
        transaction.setType("WITHDRAW");
        transaction.setRemark(remark);
        transaction.setCreateTime(LocalDateTime.now());
        walletTransactionMapper.insert(transaction);

        return user;
    }

    public BigDecimal getBalance(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        return user.getBalance() == null ? BigDecimal.ZERO : user.getBalance();
    }

    public IPage<WalletTransaction> getTransactionList(int pageNum, int pageSize, Long userId) {
        Page<WalletTransaction> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<WalletTransaction> wrapper = new LambdaQueryWrapper<WalletTransaction>()
                .eq(WalletTransaction::getUserId, userId)
                .orderByDesc(WalletTransaction::getCreateTime);
        return walletTransactionMapper.selectPage(page, wrapper);
    }

    @Transactional
    public User updateBalance(Long userId, BigDecimal balance) {
        if (balance == null || balance.compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("余额不能为负数");
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        BigDecimal oldBalance = user.getBalance() == null ? BigDecimal.ZERO : user.getBalance();
        BigDecimal diff = balance.subtract(oldBalance);

        user.setBalance(balance);
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);

        WalletTransaction transaction = new WalletTransaction();
        transaction.setUserId(userId);
        transaction.setAmount(diff);
        transaction.setType("ADJUST");
        transaction.setRemark("管理员调整余额");
        transaction.setCreateTime(LocalDateTime.now());
        walletTransactionMapper.insert(transaction);

        return user;
    }

    public List<WalletTransaction> getExportTransactions(Long userId, String type, String period) {
        LambdaQueryWrapper<WalletTransaction> wrapper = new LambdaQueryWrapper<WalletTransaction>()
                .eq(WalletTransaction::getUserId, userId);

        // 按类型筛选
        if (type != null && !type.isEmpty()) {
            wrapper.eq(WalletTransaction::getType, type);
        }

        // 按时间范围筛选
        if (period != null && !period.isEmpty()) {
            LocalDate today = LocalDate.now();
            LocalDateTime startDate = null;
            switch (period.toUpperCase()) {
                case "WEEK":
                    startDate = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay();
                    break;
                case "MONTH":
                    startDate = today.withDayOfMonth(1).atStartOfDay();
                    break;
                case "YEAR":
                    startDate = today.withDayOfYear(1).atStartOfDay();
                    break;
                default:
                    break;
            }
            if (startDate != null) {
                wrapper.ge(WalletTransaction::getCreateTime, startDate);
            }
        }

        wrapper.orderByAsc(WalletTransaction::getCreateTime);
        return walletTransactionMapper.selectList(wrapper);
    }
}
