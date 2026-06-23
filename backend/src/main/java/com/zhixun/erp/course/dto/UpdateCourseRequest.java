package com.zhixun.erp.course.dto;

public class UpdateCourseRequest {

    private Long id;
    private String name;
    private String description;
    private String coverImage;
    private String category;
    private String difficulty;
    private Integer maxStudents;
    private String location;
    private String startDate;
    private String tags;
    private Integer totalLessons;
    private String frequency;
    private String scheduleMode;
    private String defaultTimeSlot;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCoverImage() { return coverImage; }
    public void setCoverImage(String coverImage) { this.coverImage = coverImage; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
    public Integer getMaxStudents() { return maxStudents; }
    public void setMaxStudents(Integer maxStudents) { this.maxStudents = maxStudents; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    public Integer getTotalLessons() { return totalLessons; }
    public void setTotalLessons(Integer totalLessons) { this.totalLessons = totalLessons; }
    public String getFrequency() { return frequency; }
    public void setFrequency(String frequency) { this.frequency = frequency; }
    public String getScheduleMode() { return scheduleMode; }
    public void setScheduleMode(String scheduleMode) { this.scheduleMode = scheduleMode; }
    public String getDefaultTimeSlot() { return defaultTimeSlot; }
    public void setDefaultTimeSlot(String defaultTimeSlot) { this.defaultTimeSlot = defaultTimeSlot; }
}
