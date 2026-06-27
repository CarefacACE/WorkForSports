package com.zhixun.erp.agent.dto;

import lombok.Data;

@Data
public class PlanGenerateRequest {
    /** 训练计划名称/目标 */
    private String goal;
    /** 持续天数，默认28 */
    private Integer durationDays;
    /** 开始日期 yyyy-MM-dd，默认明天 */
    private String startDate;
    /** 计划描述 */
    private String description;
    /** 是否推荐课程，默认true */
    private Boolean includeCourseRecommendation;
}
