package com.zhixun.erp.course.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("private_coach_profile")
public class PrivateCoachProfile {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long coachId;

    private String description;

    private String specialties;

    private BigDecimal pricePerSession;

    private Integer sessionDuration;

    private String coverImage;

    private String status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
