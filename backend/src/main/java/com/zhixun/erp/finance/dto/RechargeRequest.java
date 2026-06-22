package com.zhixun.erp.finance.dto;

import java.math.BigDecimal;

public class RechargeRequest {

    private Long userId;

    private BigDecimal amount;

    private String remark;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
