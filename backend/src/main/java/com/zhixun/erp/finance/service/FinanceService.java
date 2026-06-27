package com.zhixun.erp.finance.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhixun.erp.finance.config.CommissionConfig;
import com.zhixun.erp.finance.dto.CommissionDetailResponse;
import com.zhixun.erp.finance.dto.WithdrawResponse;
import com.zhixun.erp.finance.entity.WalletTransaction;
import com.zhixun.erp.finance.mapper.WalletTransactionMapper;
import com.zhixun.erp.user.entity.User;
import com.zhixun.erp.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FinanceService {

    private final UserMapper userMapper;
    private final WalletTransactionMapper walletTransactionMapper;
    private final CommissionConfig commissionConfig;

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
    public WithdrawResponse withdraw(Long userId, BigDecimal amount, String remark) {
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

        // 根据累计收入查找段位和抽成比例
        BigDecimal totalEarnings = user.getTotalEarnings() == null ? BigDecimal.ZERO : user.getTotalEarnings();
        CommissionConfig.TierConfig tier = commissionConfig.getTierByEarnings(totalEarnings);
        BigDecimal commissionRate = tier != null ? tier.getRate() : BigDecimal.valueOf(0.40);
        String tierName = tier != null ? tier.getName() : "青铜教练";

        // 计算抽成金额和净得金额
        BigDecimal commissionAmount = amount.multiply(commissionRate).setScale(2, RoundingMode.DOWN);
        BigDecimal netAmount = amount.subtract(commissionAmount);

        // 从余额扣减提现金额
        user.setBalance(currentBalance.subtract(amount));
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);

        // 创建教练提现记录
        WalletTransaction withdrawTx = new WalletTransaction();
        withdrawTx.setUserId(userId);
        withdrawTx.setAmount(amount.negate());
        withdrawTx.setType("WITHDRAW");
        withdrawTx.setRemark("提现 - 段位:" + tierName + " 抽成:" + commissionRate.multiply(BigDecimal.valueOf(100)) + "% 实际到手:" + netAmount);
        withdrawTx.setCreateTime(LocalDateTime.now());
        walletTransactionMapper.insert(withdrawTx);

        // 抽成金额归入平台账户
        if (commissionAmount.compareTo(BigDecimal.ZERO) > 0) {
            Long platformUserId = commissionConfig.getPlatformUserId();
            if (platformUserId != null) {
                User platformUser = userMapper.selectById(platformUserId);
                if (platformUser != null) {
                    BigDecimal platformBalance = platformUser.getBalance() == null ? BigDecimal.ZERO : platformUser.getBalance();
                    platformUser.setBalance(platformBalance.add(commissionAmount));
                    platformUser.setUpdateTime(LocalDateTime.now());
                    userMapper.updateById(platformUser);

                    WalletTransaction platformTx = new WalletTransaction();
                    platformTx.setUserId(platformUserId);
                    platformTx.setAmount(commissionAmount);
                    platformTx.setType("COMMISSION");
                    platformTx.setRemark("平台抽成 - 教练ID:" + userId + " 段位:" + tierName);
                    platformTx.setCreateTime(LocalDateTime.now());
                    walletTransactionMapper.insert(platformTx);
                }
            }
        }

        // 构建返回结果
        WithdrawResponse response = new WithdrawResponse();
        response.setGrossAmount(amount);
        response.setTierName(tierName);
        response.setCommissionRate(commissionRate);
        response.setCommissionAmount(commissionAmount);
        response.setNetAmount(netAmount);
        response.setBalanceAfter(user.getBalance());
        return response;
    }

    public BigDecimal getBalance(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        return user.getBalance() == null ? BigDecimal.ZERO : user.getBalance();
    }

    /**
     * 获取教练的段位信息（用于前端预览抽成）
     */
    public WithdrawResponse getCommissionTier(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        BigDecimal totalEarnings = user.getTotalEarnings() == null ? BigDecimal.ZERO : user.getTotalEarnings();
        CommissionConfig.TierConfig tier = commissionConfig.getTierByEarnings(totalEarnings);
        BigDecimal commissionRate = tier != null ? tier.getRate() : BigDecimal.valueOf(0.40);
        String tierName = tier != null ? tier.getName() : "青铜教练";

        WithdrawResponse response = new WithdrawResponse();
        response.setTierName(tierName);
        response.setCommissionRate(commissionRate);
        response.setGrossAmount(totalEarnings);  // 复用字段传累计收入
        response.setBalanceAfter(user.getBalance());
        return response;
    }

    /**
     * 获取教练的抽成详情（全部段位 + 进度信息）
     */
    public CommissionDetailResponse getCommissionDetail(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        BigDecimal totalEarnings = user.getTotalEarnings() == null ? BigDecimal.ZERO : user.getTotalEarnings();
        CommissionConfig.TierConfig currentTier = commissionConfig.getTierByEarnings(totalEarnings);

        CommissionDetailResponse response = new CommissionDetailResponse();
        response.setCurrentTierName(currentTier != null ? currentTier.getName() : "青铜教练");
        response.setCurrentRate(currentTier != null ? currentTier.getRate() : BigDecimal.valueOf(0.40));
        response.setTotalEarnings(totalEarnings);
        response.setBalance(user.getBalance());

        List<CommissionConfig.TierConfig> allTiers = commissionConfig.getTiers();
        java.util.List<CommissionDetailResponse.TierDetail> tierDetails = new java.util.ArrayList<>();

        String currentTierName = currentTier != null ? currentTier.getName() : null;
        CommissionConfig.TierConfig nextTier = null;
        boolean foundCurrent = false;

        for (int i = 0; i < allTiers.size(); i++) {
            CommissionConfig.TierConfig t = allTiers.get(i);
            CommissionDetailResponse.TierDetail detail = new CommissionDetailResponse.TierDetail();
            detail.setName(t.getName());
            detail.setRate(t.getRate());
            detail.setMinEarnings(t.getMinEarnings() != null ? t.getMinEarnings() : BigDecimal.ZERO);
            detail.setMaxEarnings(t.getMaxEarnings());

            boolean isCurrent = t.getName().equals(currentTierName);
            detail.setIsCurrent(isCurrent);

            if (isCurrent) {
                foundCurrent = true;
                // 找下一级
                if (i + 1 < allTiers.size()) {
                    nextTier = allTiers.get(i + 1);
                }
            }

            // 是否已达到该段位（累计收入 >= 该段位的 minEarnings）
            BigDecimal minEarnings = t.getMinEarnings() != null ? t.getMinEarnings() : BigDecimal.ZERO;
            boolean reached = totalEarnings.compareTo(minEarnings) >= 0;
            detail.setReached(reached);

            // 进度百分比：当前段位计算，已达到的为100%，未达到的为0%
            if (isCurrent) {
                // 距离下一段位还差多少
                if (nextTier != null) {
                    BigDecimal nextMin = nextTier.getMinEarnings() != null ? nextTier.getMinEarnings() : BigDecimal.ZERO;
                    BigDecimal diff = nextMin.subtract(totalEarnings);
                    detail.setAmountToNextTier(diff.compareTo(BigDecimal.ZERO) > 0 ? diff : BigDecimal.ZERO);

                    // 进度 = (当前收入 - 本段位起点) / (下一段位起点 - 本段位起点) * 100
                    BigDecimal range = nextMin.subtract(minEarnings);
                    if (range.compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal progress = totalEarnings.subtract(minEarnings)
                                .multiply(BigDecimal.valueOf(100))
                                .divide(range, 1, java.math.RoundingMode.DOWN);
                        detail.setProgressPercent(progress.compareTo(BigDecimal.valueOf(100)) > 0
                                ? BigDecimal.valueOf(100) : progress);
                    } else {
                        detail.setProgressPercent(BigDecimal.valueOf(100));
                    }
                } else {
                    // 最高段位，无下一级
                    detail.setAmountToNextTier(BigDecimal.ZERO);
                    detail.setProgressPercent(BigDecimal.valueOf(100));
                }
            } else if (reached) {
                detail.setAmountToNextTier(BigDecimal.ZERO);
                detail.setProgressPercent(BigDecimal.valueOf(100));
            } else {
                detail.setAmountToNextTier(BigDecimal.ZERO);
                detail.setProgressPercent(BigDecimal.ZERO);
            }

            tierDetails.add(detail);
        }

        response.setAllTiers(tierDetails);
        return response;
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
