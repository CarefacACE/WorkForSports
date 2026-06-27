package com.zhixun.erp.finance.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 抽成详情响应 DTO，包含全部段位信息及当前进度
 */
public class CommissionDetailResponse {

    /** 当前段位名称 */
    private String currentTierName;

    /** 当前抽成比例 */
    private BigDecimal currentRate;

    /** 累计收入（用于段位判定） */
    private BigDecimal totalEarnings;

    /** 当前余额 */
    private BigDecimal balance;

    /** 全部段位列表（含进度信息） */
    private List<TierDetail> allTiers;

    public String getCurrentTierName() { return currentTierName; }
    public void setCurrentTierName(String currentTierName) { this.currentTierName = currentTierName; }

    public BigDecimal getCurrentRate() { return currentRate; }
    public void setCurrentRate(BigDecimal currentRate) { this.currentRate = currentRate; }

    public BigDecimal getTotalEarnings() { return totalEarnings; }
    public void setTotalEarnings(BigDecimal totalEarnings) { this.totalEarnings = totalEarnings; }

    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }

    public List<TierDetail> getAllTiers() { return allTiers; }
    public void setAllTiers(List<TierDetail> allTiers) { this.allTiers = allTiers; }

    /**
     * 单个段位详情
     */
    public static class TierDetail {
        /** 段位名称 */
        private String name;

        /** 抽成比例 */
        private BigDecimal rate;

        /** 该段位的起始累计收入门槛 */
        private BigDecimal minEarnings;

        /** 该段位的上限（-1 表示无上限） */
        private BigDecimal maxEarnings;

        /** 是否为当前段位 */
        private boolean isCurrent;

        /** 是否已达到该段位 */
        private boolean reached;

        /** 距离下一个段位还差多少收入（当前段位才有意义，最高段位时为0） */
        private BigDecimal amountToNextTier;

        /** 从本段位起点算起的进度百分比（0-100，当前段位时计算，达到的为100） */
        private BigDecimal progressPercent;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public BigDecimal getRate() { return rate; }
        public void setRate(BigDecimal rate) { this.rate = rate; }

        public BigDecimal getMinEarnings() { return minEarnings; }
        public void setMinEarnings(BigDecimal minEarnings) { this.minEarnings = minEarnings; }

        public BigDecimal getMaxEarnings() { return maxEarnings; }
        public void setMaxEarnings(BigDecimal maxEarnings) { this.maxEarnings = maxEarnings; }

        public boolean getIsCurrent() { return isCurrent; }
        public void setIsCurrent(boolean isCurrent) { this.isCurrent = isCurrent; }

        public boolean getReached() { return reached; }
        public void setReached(boolean reached) { this.reached = reached; }

        public BigDecimal getAmountToNextTier() { return amountToNextTier; }
        public void setAmountToNextTier(BigDecimal amountToNextTier) { this.amountToNextTier = amountToNextTier; }

        public BigDecimal getProgressPercent() { return progressPercent; }
        public void setProgressPercent(BigDecimal progressPercent) { this.progressPercent = progressPercent; }
    }
}
