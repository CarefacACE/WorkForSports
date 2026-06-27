package com.zhixun.erp.feedback.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 提交报修请求
 */
public class SubmitRepairRequest {

    private Long userId;
    private String username;
    private String equipmentName;
    private String equipmentLocation;
    private String description;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEquipmentName() { return equipmentName; }
    public void setEquipmentName(String equipmentName) { this.equipmentName = equipmentName; }

    public String getEquipmentLocation() { return equipmentLocation; }
    public void setEquipmentLocation(String equipmentLocation) { this.equipmentLocation = equipmentLocation; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
