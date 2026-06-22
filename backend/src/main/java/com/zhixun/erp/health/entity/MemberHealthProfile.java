package com.zhixun.erp.health.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("member_health_profile")
public class MemberHealthProfile {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    // 健康信息
    private BigDecimal height;
    private BigDecimal weight;
    private BigDecimal bodyFat;
    private BigDecimal muscleMass;
    private Integer bpSystolic;
    private Integer bpDiastolic;
    private Integer restingHeartRate;
    private String bloodType;
    private String allergies;
    private String medicalHistory;
    private String currentMedications;
    private String emergencyContactName;
    private String emergencyContactPhone;

    // 达成目标
    private BigDecimal targetWeight;
    private BigDecimal targetBodyFat;
    private BigDecimal targetMuscleMass;
    private String fitnessGoal;
    private Integer weeklyWorkoutFreq;
    private LocalDate targetDate;
    private String goalNotes;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
