package com.zhixun.erp.finance.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "commission")
public class CommissionConfig {

    private Long platformUserId;
    private List<TierConfig> tiers;

    public Long getPlatformUserId() { return platformUserId; }
    public void setPlatformUserId(Long platformUserId) { this.platformUserId = platformUserId; }

    public List<TierConfig> getTiers() { return tiers; }
    public void setTiers(List<TierConfig> tiers) { this.tiers = tiers; }

    /**
     * 根据累计收入匹配对应的段位
     */
    public TierConfig getTierByEarnings(BigDecimal totalEarnings) {
        if (tiers == null || tiers.isEmpty()) {
            return null;
        }
        BigDecimal earnings = totalEarnings == null ? BigDecimal.ZERO : totalEarnings;
        for (TierConfig tier : tiers) {
            BigDecimal min = tier.getMinEarnings() != null ? tier.getMinEarnings() : BigDecimal.ZERO;
            BigDecimal max = tier.getMaxEarnings();
            if (earnings.compareTo(min) >= 0) {
                // max == -1 表示无上限
                if (max != null && max.compareTo(BigDecimal.ZERO) < 0) {
                    return tier;
                }
                if (max != null && earnings.compareTo(max) < 0) {
                    return tier;
                }
            }
        }
        // fallback: return last tier (highest)
        return tiers.get(tiers.size() - 1);
    }

    public static class TierConfig {
        private String name;
        private BigDecimal minEarnings;
        private BigDecimal maxEarnings;
        private BigDecimal rate;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public BigDecimal getMinEarnings() { return minEarnings; }
        public void setMinEarnings(BigDecimal minEarnings) { this.minEarnings = minEarnings; }

        public BigDecimal getMaxEarnings() { return maxEarnings; }
        public void setMaxEarnings(BigDecimal maxEarnings) { this.maxEarnings = maxEarnings; }

        public BigDecimal getRate() { return rate; }
        public void setRate(BigDecimal rate) { this.rate = rate; }
    }
}
