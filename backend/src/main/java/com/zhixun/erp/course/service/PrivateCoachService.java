package com.zhixun.erp.course.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhixun.erp.course.entity.Enrollment;
import com.zhixun.erp.course.entity.PrivateCoachProfile;
import com.zhixun.erp.course.mapper.EnrollmentMapper;
import com.zhixun.erp.course.mapper.PrivateCoachProfileMapper;
import com.zhixun.erp.schedule.entity.CourseSchedule;
import com.zhixun.erp.schedule.mapper.CourseScheduleMapper;
import com.zhixun.erp.user.entity.User;
import com.zhixun.erp.user.mapper.UserMapper;
import com.zhixun.erp.finance.entity.WalletTransaction;
import com.zhixun.erp.finance.mapper.WalletTransactionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
    private final WalletTransactionMapper walletTransactionMapper;

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
        // Find all active profiles
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

        // Wrap in page
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

        // Include available schedules
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

    /* ─── Purchase ─── */

    public Enrollment purchaseSessions(Long memberId, Long coachId, Integer sessions) {
        if (sessions == null || sessions <= 0) throw new RuntimeException("购买节数必须大于0");

        PrivateCoachProfile profile = getCoachProfile(coachId);
        if (profile == null || !"ACTIVE".equals(profile.getStatus())) {
            throw new RuntimeException("该教练暂未开放私教主页");
        }

        User member = userMapper.selectById(memberId);
        if (member == null) throw new RuntimeException("会员不存在");

        BigDecimal totalCost = profile.getPricePerSession().multiply(new BigDecimal(sessions));

        // Check balance
        if (member.getBalance() == null || member.getBalance().compareTo(totalCost) < 0) {
            throw new RuntimeException("余额不足，需要 " + totalCost + " 元，当前余额 " + member.getBalance());
        }

        // Deduct member balance
        member.setBalance(member.getBalance().subtract(totalCost));
        userMapper.updateById(member);

        // Record member transaction
        WalletTransaction memberTx = new WalletTransaction();
        memberTx.setUserId(memberId);
        memberTx.setAmount(totalCost.negate());
        memberTx.setType("CONSUME");
        memberTx.setRemark("购买私教课 - " + sessions + "节");
        memberTx.setCreateTime(LocalDateTime.now());
        walletTransactionMapper.insert(memberTx);

        // Credit coach
        User coach = userMapper.selectById(coachId);
        BigDecimal coachBalance = coach.getBalance() != null ? coach.getBalance() : BigDecimal.ZERO;
        coach.setBalance(coachBalance.add(totalCost));
        userMapper.updateById(coach);

        // Record coach transaction
        WalletTransaction coachTx = new WalletTransaction();
        coachTx.setUserId(coachId);
        coachTx.setAmount(totalCost);
        coachTx.setType("COURSE_INCOME");
        coachTx.setRemark("私教课收入 - " + sessions + "节");
        coachTx.setCreateTime(LocalDateTime.now());
        walletTransactionMapper.insert(coachTx);

        // Create or update enrollment (use courseId = coachId as a convention for private coaching)
        Enrollment enrollment = enrollmentMapper.selectOne(new LambdaQueryWrapper<Enrollment>()
                .eq(Enrollment::getUserId, memberId)
                .eq(Enrollment::getCourseId, coachId)
                .eq(Enrollment::getStatus, "PAID"));

        if (enrollment != null) {
            enrollment.setTotalSessions(enrollment.getTotalSessions() + sessions);
            enrollment.setRemainingSessions(enrollment.getRemainingSessions() + sessions);
            enrollment.setPaidAmount(enrollment.getPaidAmount().add(totalCost));
            enrollment.setUpdateTime(LocalDateTime.now());
            enrollmentMapper.updateById(enrollment);
        } else {
            enrollment = new Enrollment();
            enrollment.setUserId(memberId);
            enrollment.setCourseId(coachId); // coachId as courseId for private coaching
            enrollment.setStatus("PAID");
            enrollment.setPaidAmount(totalCost);
            enrollment.setTotalSessions(sessions);
            enrollment.setRemainingSessions(sessions);
            enrollment.setCreateTime(LocalDateTime.now());
            enrollmentMapper.insert(enrollment);
        }

        return enrollment;
    }

    /* ─── My Coaches ─── */

    public List<Map<String, Object>> getMyCoaches(Long memberId) {
        List<Enrollment> enrollments = enrollmentMapper.selectList(
                new LambdaQueryWrapper<Enrollment>()
                        .eq(Enrollment::getUserId, memberId)
                        .eq(Enrollment::getStatus, "PAID")
                        .isNotNull(Enrollment::getTotalSessions));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Enrollment e : enrollments) {
            PrivateCoachProfile profile = getCoachProfile(e.getCourseId());
            if (profile == null) continue;
            User coach = userMapper.selectById(e.getCourseId());
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
            result.add(item);
        }
        return result;
    }

    /* ─── Book Session ─── */

    public void bookSession(Long memberId, Long scheduleId) {
        CourseSchedule schedule = scheduleMapper.selectById(scheduleId);
        if (schedule == null) throw new RuntimeException("课表不存在");

        // Check if AVAILABLE
        if (schedule.getBookingStatus() != null && !"AVAILABLE".equals(schedule.getBookingStatus())) {
            throw new RuntimeException("该时段已被预约");
        }

        // Find enrollment for this coach
        Enrollment enrollment = enrollmentMapper.selectOne(new LambdaQueryWrapper<Enrollment>()
                .eq(Enrollment::getUserId, memberId)
                .eq(Enrollment::getCourseId, schedule.getCoachId())
                .eq(Enrollment::getStatus, "PAID")
                .gt(Enrollment::getRemainingSessions, 0));

        if (enrollment == null) {
            throw new RuntimeException("您尚未购买该教练的私教课或课时已用完");
        }

        // Book
        schedule.setMemberId(memberId);
        schedule.setEnrollmentId(enrollment.getId());
        schedule.setBookingStatus("BOOKED");
        schedule.setUpdateTime(LocalDateTime.now());
        scheduleMapper.updateById(schedule);

        // Deduct remaining session
        enrollment.setRemainingSessions(enrollment.getRemainingSessions() - 1);
        enrollment.setUpdateTime(LocalDateTime.now());
        enrollmentMapper.updateById(enrollment);
    }

    /* ─── Book Direct (empty slot) ─── */

    public CourseSchedule bookDirect(Long memberId, Long coachId, LocalDateTime startTime, LocalDateTime endTime) {
        // Find enrollment
        Enrollment enrollment = enrollmentMapper.selectOne(new LambdaQueryWrapper<Enrollment>()
                .eq(Enrollment::getUserId, memberId)
                .eq(Enrollment::getCourseId, coachId)
                .eq(Enrollment::getStatus, "PAID")
                .gt(Enrollment::getRemainingSessions, 0));
        if (enrollment == null) {
            throw new RuntimeException("您尚未购买该教练的私教课或课时已用完");
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

        // Get coach profile for location
        PrivateCoachProfile profile = getCoachProfile(coachId);

        // Create BOOKED entry directly
        CourseSchedule schedule = new CourseSchedule();
        schedule.setCoachId(coachId);
        schedule.setMemberId(memberId);
        schedule.setEnrollmentId(enrollment.getId());
        schedule.setTitle("私教课");
        schedule.setStartTime(startTime);
        schedule.setEndTime(endTime);
        schedule.setLocation(profile != null ? "私教课" : null);
        schedule.setColor("#3056d3");
        schedule.setBookingStatus("BOOKED");
        schedule.setCreateTime(LocalDateTime.now());
        scheduleMapper.insert(schedule);

        // Deduct
        enrollment.setRemainingSessions(enrollment.getRemainingSessions() - 1);
        enrollment.setUpdateTime(LocalDateTime.now());
        enrollmentMapper.updateById(enrollment);

        return schedule;
    }

    /* ─── Cancel Booking ─── */

    public void cancelBooking(Long memberId, Long scheduleId) {
        CourseSchedule schedule = scheduleMapper.selectById(scheduleId);
        if (schedule == null) throw new RuntimeException("课表不存在");

        if (!memberId.equals(schedule.getMemberId())) {
            throw new RuntimeException("只能取消自己的预约");
        }

        if (!"BOOKED".equals(schedule.getBookingStatus())) {
            throw new RuntimeException("该时段未处于已预约状态");
        }

        Long savedEnrollmentId = schedule.getEnrollmentId();

        // Delete the entry entirely — empty slot is implicitly available
        scheduleMapper.deleteById(scheduleId);

        // Return session
        Enrollment enrollment = enrollmentMapper.selectById(savedEnrollmentId);
        if (enrollment != null) {
            enrollment.setRemainingSessions(enrollment.getRemainingSessions() + 1);
            enrollment.setUpdateTime(LocalDateTime.now());
            enrollmentMapper.updateById(enrollment);
        }
    }

    /* ─── Reschedule Booking (to any empty time) ─── */

    public void rescheduleBooking(Long memberId, Long currentScheduleId, LocalDateTime targetStart, LocalDateTime targetEnd) {
        CourseSchedule current = scheduleMapper.selectById(currentScheduleId);
        if (current == null) throw new RuntimeException("当前预约不存在");

        if (!memberId.equals(current.getMemberId())) {
            throw new RuntimeException("只能改期自己的预约");
        }
        if (!"BOOKED".equals(current.getBookingStatus())) {
            throw new RuntimeException("该时段未处于已预约状态");
        }

        // Same time → no-op
        if (current.getStartTime().equals(targetStart) && current.getEndTime().equals(targetEnd)) {
            return;
        }

        // Check conflicts at target time (excluding the current entry itself)
        List<CourseSchedule> conflicts = scheduleMapper.selectList(
                new LambdaQueryWrapper<CourseSchedule>()
                        .eq(CourseSchedule::getCoachId, current.getCoachId())
                        .lt(CourseSchedule::getStartTime, targetEnd)
                        .gt(CourseSchedule::getEndTime, targetStart)
                        .ne(CourseSchedule::getId, currentScheduleId));
        if (!conflicts.isEmpty()) {
            throw new RuntimeException("目标时段教练已有安排");
        }

        Long savedEnrollmentId = current.getEnrollmentId();

        // Delete old entry — empty slot is implicitly available
        scheduleMapper.deleteById(currentScheduleId);

        // Create new BOOKED entry at target time
        CourseSchedule newSchedule = new CourseSchedule();
        newSchedule.setCoachId(current.getCoachId());
        newSchedule.setMemberId(memberId);
        newSchedule.setEnrollmentId(savedEnrollmentId);
        newSchedule.setTitle(current.getTitle());
        newSchedule.setStartTime(targetStart);
        newSchedule.setEndTime(targetEnd);
        newSchedule.setLocation(current.getLocation());
        newSchedule.setColor(current.getColor());
        newSchedule.setBookingStatus("BOOKED");
        newSchedule.setCreateTime(LocalDateTime.now());
        scheduleMapper.insert(newSchedule);

        // remainingSessions unchanged (swap)
    }
}
