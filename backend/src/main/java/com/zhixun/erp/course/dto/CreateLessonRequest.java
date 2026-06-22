package com.zhixun.erp.course.dto;

public class CreateLessonRequest {

    private Long courseId;
    private String title;
    private String videoUrl;
    private Integer sortOrder;
    private Integer isTrial;
    private String description;
    private Integer duration;

    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public Integer getIsTrial() { return isTrial; }
    public void setIsTrial(Integer isTrial) { this.isTrial = isTrial; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getDuration() { return duration; }
    public void setDuration(Integer duration) { this.duration = duration; }
}
