package com.zhixun.erp.finance.dto;

import java.math.BigDecimal;

/**
 * 提现响应 DTO，包含抽成明细
 */
public class WithdrawResponse {

    /** 毛提现金额 */
    private BigDecimal grossAmount;

    /** 段位名称 */
    private String tierName;

    /** 抽成比例 */
    private BigDecimal commissionRate;

    /** 抽成金额 */
    private BigDecimal commissionAmount;

    /** 实际到手 */
    private BigDecimal netAmount;

    /** 提现后余额 */
    private BigDecimal balanceAfter;

    public BigDecimal getGrossAmount() { return grossAmount; }
    public void setGrossAmount(BigDecimal grossAmount) { this.grossAmount = grossAmount; }

    public String getTierName() { return tierName; }
    public void setTierName(String tierName) { this.tierName = tierName; }

    public BigDecimal getCommissionRate() { return commissionRate; }
    public void setCommissionRate(BigDecimal commissionRate) { this.commissionRate = commissionRate; }

    public BigDecimal getCommissionAmount() { return commissionAmount; }
    public void setCommissionAmount(BigDecimal commissionAmount) { this.commissionAmount = commissionAmount; }

    public BigDecimal getNetAmount() { return netAmount; }
    public void setNetAmount(BigDecimal netAmount) { this.netAmount = netAmount; }

    public BigDecimal getBalanceAfter() { return balanceAfter; }
    public void setBalanceAfter(BigDecimal balanceAfter) { this.balanceAfter = balanceAfter; }
}
