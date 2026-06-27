package com.zhixun.erp.plan.dto;

import java.util.List;

public class PlanSubmitRequest {

    private String goal;
    private Integer durationDays;
    private String startDate;
    private String endDate;
    private String description;
    private List<DetailItem> details;

    public String getGoal() { return goal; }
    public void setGoal(String goal) { this.goal = goal; }
    public Integer getDurationDays() { return durationDays; }
    public void setDurationDays(Integer durationDays) { this.durationDays = durationDays; }
    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }
    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public List<DetailItem> getDetails() { return details; }
    public void setDetails(List<DetailItem> details) { this.details = details; }

    public static class DetailItem {
        private Integer dayNumber;
        private String trainingType;
        private String content;
        private Integer durationMinutes;
        private String intensity;

        public Integer getDayNumber() { return dayNumber; }
        public void setDayNumber(Integer dayNumber) { this.dayNumber = dayNumber; }
        public String getTrainingType() { return trainingType; }
        public void setTrainingType(String trainingType) { this.trainingType = trainingType; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public Integer getDurationMinutes() { return durationMinutes; }
        public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }
        public String getIntensity() { return intensity; }
        public void setIntensity(String intensity) { this.intensity = intensity; }
    }
}
