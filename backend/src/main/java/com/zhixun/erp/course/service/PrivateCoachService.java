package com.zhixun.erp.course.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhixun.erp.chat.service.NotificationService;
import com.zhixun.erp.course.entity.Enrollment;
import com.zhixun.erp.course.entity.PrivateCoachProfile;
import com.zhixun.erp.course.mapper.EnrollmentMapper;
import com.zhixun.erp.course.mapper.PrivateCoachProfileMapper;
import com.zhixun.erp.schedule.entity.CourseSchedule;
import com.zhixun.erp.schedule.mapper.CourseScheduleMapper;
import com.zhixun.erp.user.entity.User;
import com.zhixun.erp.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PrivateCoachService {

    private final PrivateCoachProfileMapper profileMapper;
    private final UserMapper userMapper;
    private final EnrollmentMapper enrollmentMapper;
    private final CourseScheduleMapper scheduleMapper;
    private final NotificationService notificationService;

    /* ─── Coach Profile ─── */

    public PrivateCoachProfile getCoachProfile(Long coachId) {
        return profileMapper.selectOne(new LambdaQueryWrapper<PrivateCoachProfile>()
                .eq(PrivateCoachProfile::getCoachId, coachId));
    }

    public PrivateCoachProfile saveCoachProfile(Long coachId, PrivateCoachProfile input) {
        User coach = userMapper.selectById(coachId);
        if (coach == null || !"COACH".equals(coach.getRole())) {
            throw new RuntimeException("仅教练可编辑私教主页");
        }

        PrivateCoachProfile existing = getCoachProfile(coachId);
        if (existing != null) {
            if (input.getDescription() != null) existing.setDescription(input.getDescription());
            if (input.getSpecialties() != null) existing.setSpecialties(input.getSpecialties());
            if (input.getPricePerSession() != null) existing.setPricePerSession(input.getPricePerSession());
            if (input.getSessionDuration() != null) existing.setSessionDuration(input.getSessionDuration());
            if (input.getCoverImage() != null) existing.setCoverImage(input.getCoverImage());
            if (input.getStatus() != null) existing.setStatus(input.getStatus());
            existing.setUpdateTime(LocalDateTime.now());
            profileMapper.updateById(existing);
            return existing;
        } else {
            input.setCoachId(coachId);
            input.setStatus("ACTIVE");
            input.setCreateTime(LocalDateTime.now());
            if (input.getSessionDuration() == null) input.setSessionDuration(60);
            if (input.getPricePerSession() == null) input.setPricePerSession(BigDecimal.ZERO);
            profileMapper.insert(input);
            return input;
        }
    }

    /* ─── List Coaches (for members) ─── */

    public IPage<Map<String, Object>> listCoaches(String keyword, int pageNum, int pageSize) {
        LambdaQueryWrapper<PrivateCoachProfile> wrapper = new LambdaQueryWrapper<PrivateCoachProfile>()
                .eq(PrivateCoachProfile::getStatus, "ACTIVE");

        Page<PrivateCoachProfile> page = new Page<>(pageNum, pageSize);
        IPage<PrivateCoachProfile> profilePage = profileMapper.selectPage(page, wrapper);

        List<Map<String, Object>> result = new ArrayList<>();
        for (PrivateCoachProfile profile : profilePage.getRecords()) {
            User coach = userMapper.selectById(profile.getCoachId());
            if (coach == null) continue;
            if (keyword != null && !keyword.trim().isEmpty()) {
                String kw = keyword.trim();
                boolean match = (coach.getRealName() != null && coach.getRealName().contains(kw))
                        || (coach.getUsername() != null && coach.getUsername().contains(kw));
                if (!match) continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("coachId", coach.getId());
            item.put("username", coach.getUsername());
            item.put("realName", coach.getRealName());
            item.put("avatar", coach.getAvatar());
            item.put("phone", coach.getPhone());
            item.put("description", profile.getDescription());
            item.put("specialties", profile.getSpecialties());
            item.put("pricePerSession", profile.getPricePerSession());
            item.put("sessionDuration", profile.getSessionDuration());
            item.put("coverImage", profile.getCoverImage());
            result.add(item);
        }

        Page<Map<String, Object>> resultPage = new Page<>(pageNum, pageSize);
        resultPage.setRecords(result);
        resultPage.setTotal(profilePage.getTotal());
        return resultPage;
    }

    /* ─── Coach Detail ─── */

    public Map<String, Object> getCoachDetail(Long coachId) {
        PrivateCoachProfile profile = getCoachProfile(coachId);
        if (profile == null) throw new RuntimeException("该教练暂未开放私教主页");

        User coach = userMapper.selectById(coachId);
        if (coach == null) throw new RuntimeException("教练不存在");

        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("coachId", coach.getId());
        detail.put("username", coach.getUsername());
        detail.put("realName", coach.getRealName());
        detail.put("avatar", coach.getAvatar());
        detail.put("description", profile.getDescription());
        detail.put("specialties", profile.getSpecialties());
        detail.put("pricePerSession", profile.getPricePerSession());
        detail.put("sessionDuration", profile.getSessionDuration());
        detail.put("coverImage", profile.getCoverImage());

        List<CourseSchedule> schedules = scheduleMapper.selectList(
                new LambdaQueryWrapper<CourseSchedule>()
                        .eq(CourseSchedule::getCoachId, coachId)
                        .and(w -> w.isNull(CourseSchedule::getBookingStatus)
                                .or().eq(CourseSchedule::getBookingStatus, "AVAILABLE"))
                        .ge(CourseSchedule::getStartTime, LocalDateTime.now())
                        .orderByAsc(CourseSchedule::getStartTime));
        detail.put("availableSchedules", schedules);

        return detail;
    }

    /* ─── Enroll (join coach, no upfront payment) ─── */

    @Transactional
    public Enrollment enrollCoach(Long memberId, Long coachId, Integer autoDeductAgreed) {
        PrivateCoachProfile profile = getCoachProfile(coachId);
        if (profile == null || !"ACTIVE".equals(profile.getStatus())) {
            throw new RuntimeException("该教练暂未开放私教主页");
        }

        User member = userMapper.selectById(memberId);
        if (member == null) throw new RuntimeException("会员不存在");

        // Check if already enrolled
        Enrollment existing = enrollmentMapper.selectOne(new LambdaQueryWrapper<Enrollment>()
                .eq(Enrollment::getUserId, memberId)
                .eq(Enrollment::getCoachId, coachId)
                .ne(Enrollment::getStatus, "CANCELLED"));
        if (existing != null) {
            throw new RuntimeException("您已加入该教练的私教课程");
        }

        Enrollment enrollment = new Enrollment();
        enrollment.setUserId(memberId);
        enrollment.setCoachId(coachId);
        enrollment.setCourseId(null); // No course association
        enrollment.setStatus("PAID"); // Active enrollment
        enrollment.setPaidAmount(BigDecimal.ZERO);
        enrollment.setTotalSessions(null);
        enrollment.setRemainingSessions(null);
        enrollment.setAutoDeductAgreed(autoDeductAgreed != null ? autoDeductAgreed : 0);
        enrollment.setCreateTime(LocalDateTime.now());
        enrollmentMapper.insert(enrollment);

        return enrollment;
    }

    /* ─── Request Session (member → coach approval) ─── */

    @Transactional
    public CourseSchedule requestSession(Long memberId, Long coachId, LocalDateTime startTime, LocalDateTime endTime) {
        // Check enrollment
        Enrollment enrollment = enrollmentMapper.selectOne(new LambdaQueryWrapper<Enrollment>()
                .eq(Enrollment::getUserId, memberId)
                .eq(Enrollment::getCoachId, coachId)
                .eq(Enrollment::getStatus, "PAID"));
        if (enrollment == null) {
            throw new RuntimeException("您尚未加入该教练的私教课程");
        }

        // Check for conflicts
        List<CourseSchedule> conflicts = scheduleMapper.selectList(
                new LambdaQueryWrapper<CourseSchedule>()
                        .eq(CourseSchedule::getCoachId, coachId)
                        .lt(CourseSchedule::getStartTime, endTime)
                        .gt(CourseSchedule::getEndTime, startTime));
        if (!conflicts.isEmpty()) {
            throw new RuntimeException("该时段教练已有安排");
        }

        User member = userMapper.selectById(memberId);
        String memberName = member != null && member.getRealName() != null ? member.getRealName() : member.getUsername();

        CourseSchedule schedule = new CourseSchedule();
        schedule.setCoachId(coachId);
        schedule.setMemberId(memberId);
        schedule.setEnrollmentId(enrollment.getId());
        schedule.setTitle(memberName + "的私教课(待确认)");
        schedule.setStartTime(startTime);
        schedule.setEndTime(endTime);
        schedule.setLocation("私教课");
        schedule.setColor("#f59e0b"); // Orange for pending
        schedule.setBookingStatus("REQUESTED");
        schedule.setCreateTime(LocalDateTime.now());
        scheduleMapper.insert(schedule);

        // 通知教练有新的预约请求
        User coach = userMapper.selectById(coachId);
        String coachName = coach != null && coach.getRealName() != null ? coach.getRealName() : "教练";
        String timeStr = startTime.format(java.time.format.DateTimeFormatter.ofPattern("MM-dd HH:mm"));
        notificationService.sendNotification(coachId,
                "新的私教预约请求",
                memberName + " 请求预约 " + timeStr + " 的私教课",
                "SESSION_REQUEST", schedule.getId());

        return schedule;
    }

    /* ─── Approve Session (coach approves member's request) ─── */

    @Transactional
    public void approveSession(Long coachId, Long scheduleId) {
        CourseSchedule schedule = scheduleMapper.selectById(scheduleId);
        if (schedule == null) throw new RuntimeException("预约记录不存在");
        if (!coachId.equals(schedule.getCoachId())) {
            throw new RuntimeException("只能审批自己的预约请求");
        }
        if (!"REQUESTED".equals(schedule.getBookingStatus())) {
            throw new RuntimeException("该预约已处理过");
        }

        schedule.setBookingStatus("BOOKED");
        schedule.setColor("#3056d3"); // Blue for approved
        schedule.setUpdateTime(LocalDateTime.now());

        // Update title to remove pending marker
        if (schedule.getTitle() != null && schedule.getTitle().contains("待确认")) {
            schedule.setTitle(schedule.getTitle().replace("(待确认)", ""));
        }
        scheduleMapper.updateById(schedule);

        // 通知会员预约已通过
        User coach = userMapper.selectById(coachId);
        String coachName = coach != null && coach.getRealName() != null ? coach.getRealName() : "教练";
        String timeStr = schedule.getStartTime().format(java.time.format.DateTimeFormatter.ofPattern("MM-dd HH:mm"));
        notificationService.sendNotification(schedule.getMemberId(),
                "私教预约已通过",
                "教练 " + coachName + " 已通过您在 " + timeStr + " 的私教预约",
                "SESSION_APPROVED", scheduleId);
    }

    /* ─── Reject Session (coach rejects member's request) ─── */

    @Transactional
    public void rejectSession(Long coachId, Long scheduleId, String reason) {
        CourseSchedule schedule = scheduleMapper.selectById(scheduleId);
        if (schedule == null) throw new RuntimeException("预约记录不存在");
        if (!coachId.equals(schedule.getCoachId())) {
            throw new RuntimeException("只能处理自己的预约请求");
        }
        if (!"REQUESTED".equals(schedule.getBookingStatus())) {
            throw new RuntimeException("该预约已处理过");
        }

        schedule.setBookingStatus("REJECTED");
        schedule.setRejectReason(reason);
        schedule.setUpdateTime(LocalDateTime.now());
        schedule.setColor("#ef4444"); // Red for rejected
        scheduleMapper.updateById(schedule);

        // 通知会员预约被拒绝
        User coach = userMapper.selectById(coachId);
        String coachName = coach != null && coach.getRealName() != null ? coach.getRealName() : "教练";
        String timeStr = schedule.getStartTime().format(java.time.format.DateTimeFormatter.ofPattern("MM-dd HH:mm"));
        String content = "教练 " + coachName + " 拒绝了您在 " + timeStr + " 的私教预约";
        if (reason != null && !reason.trim().isEmpty()) {
            content += "，原因：" + reason;
        }
        notificationService.sendNotification(schedule.getMemberId(),
                "私教预约已拒绝",
                content,
                "SESSION_REJECTED", scheduleId);
    }

    /* ─── Quit Coach (member leaves) ─── */

    @Transactional
    public void quitCoach(Long memberId, Long coachId) {
        Enrollment enrollment = enrollmentMapper.selectOne(new LambdaQueryWrapper<Enrollment>()
                .eq(Enrollment::getUserId, memberId)
                .eq(Enrollment::getCoachId, coachId)
                .eq(Enrollment::getStatus, "PAID"));
        if (enrollment == null) {
            throw new RuntimeException("未找到该私教报名记录");
        }

        // Cancel all REQUESTED schedules for this member + coach
        List<CourseSchedule> pendingSchedules = scheduleMapper.selectList(
                new LambdaQueryWrapper<CourseSchedule>()
                        .eq(CourseSchedule::getCoachId, coachId)
                        .eq(CourseSchedule::getMemberId, memberId)
                        .eq(CourseSchedule::getBookingStatus, "REQUESTED"));
        for (CourseSchedule s : pendingSchedules) {
            scheduleMapper.deleteById(s.getId());
        }

        // Mark enrollment as CANCELLED
        enrollment.setStatus("CANCELLED");
        enrollment.setUpdateTime(LocalDateTime.now());
        enrollmentMapper.updateById(enrollment);
    }

    /* ─── My Coaches (via coachId) ─── */

    public List<Map<String, Object>> getMyCoaches(Long memberId) {
        List<Enrollment> enrollments = enrollmentMapper.selectList(
                new LambdaQueryWrapper<Enrollment>()
                        .eq(Enrollment::getUserId, memberId)
                        .eq(Enrollment::getStatus, "PAID")
                        .isNotNull(Enrollment::getCoachId));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Enrollment e : enrollments) {
            PrivateCoachProfile profile = getCoachProfile(e.getCoachId());
            if (profile == null) continue;
            User coach = userMapper.selectById(e.getCoachId());
            if (coach == null) continue;

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("enrollmentId", e.getId());
            item.put("coachId", coach.getId());
            item.put("realName", coach.getRealName());
            item.put("avatar", coach.getAvatar());
            item.put("specialties", profile.getSpecialties());
            item.put("pricePerSession", profile.getPricePerSession());
            item.put("totalSessions", e.getTotalSessions());
            item.put("remainingSessions", e.getRemainingSessions());
            item.put("paidAmount", e.getPaidAmount());
            item.put("autoDeductAgreed", e.getAutoDeductAgreed());
            result.add(item);
        }
        return result;
    }

    /* ─── Book Direct (member clicks empty slot → directly book) ─── */

    @Transactional
    public CourseSchedule bookDirect(Long memberId, Long coachId, LocalDateTime startTime, LocalDateTime endTime) {
        // Verify enrollment
        Enrollment enrollment = enrollmentMapper.selectOne(new LambdaQueryWrapper<Enrollment>()
                .eq(Enrollment::getUserId, memberId)
                .eq(Enrollment::getCoachId, coachId)
                .eq(Enrollment::getStatus, "PAID"));
        if (enrollment == null) {
            throw new RuntimeException("您尚未加入该教练的私教课程");
        }

        // Check remaining sessions (only when explicitly set, null = unlimited)
        if (enrollment.getRemainingSessions() != null && enrollment.getRemainingSessions() <= 0) {
            throw new RuntimeException("该教练课时不足");
        }

        // Check for conflicts
        List<CourseSchedule> conflicts = scheduleMapper.selectList(
                new LambdaQueryWrapper<CourseSchedule>()
                        .eq(CourseSchedule::getCoachId, coachId)
                        .lt(CourseSchedule::getStartTime, endTime)
                        .gt(CourseSchedule::getEndTime, startTime));
        if (!conflicts.isEmpty()) {
            throw new RuntimeException("该时段教练已有安排");
        }

        User member = userMapper.selectById(memberId);
        String memberName = member != null && member.getRealName() != null ? member.getRealName() : member.getUsername();

        CourseSchedule schedule = new CourseSchedule();
        schedule.setCoachId(coachId);
        schedule.setMemberId(memberId);
        schedule.setEnrollmentId(enrollment.getId());
        schedule.setTitle(memberName + "的私教课");
        schedule.setStartTime(startTime);
        schedule.setEndTime(endTime);
        schedule.setLocation("私教课");
        schedule.setColor("#3056d3");
        schedule.setBookingStatus("BOOKED");
        schedule.setCreateTime(LocalDateTime.now());
        scheduleMapper.insert(schedule);

        // Deduct remaining sessions if applicable
        if (enrollment.getRemainingSessions() != null && enrollment.getRemainingSessions() > 0) {
            enrollment.setRemainingSessions(enrollment.getRemainingSessions() - 1);
            enrollment.setUpdateTime(LocalDateTime.now());
            enrollmentMapper.updateById(enrollment);
        }

        return schedule;
    }

    /* ─── Cancel Booking ─── */

    @Transactional
    public void cancelBooking(Long memberId, Long scheduleId) {
        CourseSchedule schedule = scheduleMapper.selectById(scheduleId);
        if (schedule == null) throw new RuntimeException("课表不存在");
        if (!memberId.equals(schedule.getMemberId())) {
            throw new RuntimeException("只能取消自己的预约");
        }
        if (!"BOOKED".equals(schedule.getBookingStatus()) && !"REQUESTED".equals(schedule.getBookingStatus())) {
            throw new RuntimeException("该时段不允许取消");
        }
        scheduleMapper.deleteById(scheduleId);
    }
}
