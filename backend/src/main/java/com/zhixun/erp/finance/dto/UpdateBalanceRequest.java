package com.zhixun.erp.finance.dto;

import java.math.BigDecimal;

public class UpdateBalanceRequest {

    private Long userId;

    private BigDecimal balance;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
}
