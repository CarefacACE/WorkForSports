package com.zhixun.erp.feedback.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 器材报修实体
 */
@TableName("equipment_repair")
public class EquipmentRepair {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 提交报修的教练ID */
    private Long userId;

    /** 教练用户名 */
    private String username;

    /** 器材名称 */
    private String equipmentName;

    /** 器材位置/编号 */
    private String equipmentLocation;

    /** 故障描述 */
    private String description;

    /** 状态：PENDING（待处理）、PROCESSING（处理中）、RESOLVED（已解决）、REJECTED（已驳回） */
    private String status;

    /** 管理员反馈/备注 */
    private String feedback;

    /** 处理时间 */
    private LocalDateTime processedTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

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

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }

    public LocalDateTime getProcessedTime() { return processedTime; }
    public void setProcessedTime(LocalDateTime processedTime) { this.processedTime = processedTime; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }

    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
