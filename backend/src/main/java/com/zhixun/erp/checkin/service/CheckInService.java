package com.zhixun.erp.checkin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhixun.erp.checkin.entity.CheckInRecord;
import com.zhixun.erp.checkin.mapper.CheckInRecordMapper;
import com.zhixun.erp.course.entity.Enrollment;
import com.zhixun.erp.course.entity.PrivateCoachProfile;
import com.zhixun.erp.course.mapper.EnrollmentMapper;
import com.zhixun.erp.course.mapper.PrivateCoachProfileMapper;
import com.zhixun.erp.finance.entity.WalletTransaction;
import com.zhixun.erp.finance.mapper.WalletTransactionMapper;
import com.zhixun.erp.schedule.entity.CourseSchedule;
import com.zhixun.erp.schedule.mapper.CourseScheduleMapper;
import com.zhixun.erp.user.entity.User;
import com.zhixun.erp.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CheckInService {

    private final CheckInRecordMapper checkInRecordMapper;
    private final CourseScheduleMapper scheduleMapper;
    private final EnrollmentMapper enrollmentMapper;
    private final UserMapper userMapper;
    private final WalletTransactionMapper walletTransactionMapper;
    private final PrivateCoachProfileMapper profileMapper;

    private static final int CHECK_IN_WINDOW_MINUTES = 30;

    @Transactional
    public CheckInRecord checkIn(Long scheduleId, Long userId, String role) {
        CourseSchedule schedule = scheduleMapper.selectById(scheduleId);
        if (schedule == null) {
            throw new RuntimeException("排课记录不存在");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime windowStart = schedule.getStartTime().minusMinutes(CHECK_IN_WINDOW_MINUTES);
        LocalDateTime windowEnd = schedule.getEndTime();

        if (now.isBefore(windowStart)) {
            throw new RuntimeException("签到未开始，签到时间为课程开始前" + CHECK_IN_WINDOW_MINUTES + "分钟");
        }
        if (now.isAfter(windowEnd)) {
            throw new RuntimeException("课程已结束，无法签到");
        }

        CheckInRecord existing = checkInRecordMapper.selectOne(
                new LambdaQueryWrapper<CheckInRecord>()
                        .eq(CheckInRecord::getScheduleId, scheduleId)
                        .eq(CheckInRecord::getUserId, userId)
                        .eq(CheckInRecord::getRole, role));

        if (existing != null && "SIGNED".equals(existing.getStatus())) {
            throw new RuntimeException("已签到，请勿重复签到");
        }

        if (existing != null) {
            existing.setStatus("SIGNED");
            existing.setCheckInTime(now);
            existing.setUpdateTime(now);
            checkInRecordMapper.updateById(existing);
        } else {
            existing = new CheckInRecord();
            existing.setScheduleId(scheduleId);
            existing.setUserId(userId);
            existing.setRole(role);
            existing.setCheckInTime(now);
            existing.setStatus("SIGNED");
            existing.setCreateTime(now);
            checkInRecordMapper.insert(existing);
        }

        // 签到成功后检查是否需要自动扣费（私教双签）
        checkAndAutoDeduct(schedule);

        return existing;
    }

    @Transactional
    public CheckInRecord updateCheckInStatus(Long coachId, Long scheduleId, Long userId, String status) {
        if (!"SIGNED".equals(status) && !"ABSENT".equals(status) && !"PENDING".equals(status)) {
            throw new RuntimeException("无效的考勤状态");
        }

        CourseSchedule schedule = scheduleMapper.selectById(scheduleId);
        if (schedule == null || !schedule.getCoachId().equals(coachId)) {
            throw new RuntimeException("只能修改自己排课的考勤");
        }

        CheckInRecord existing = checkInRecordMapper.selectOne(
                new LambdaQueryWrapper<CheckInRecord>()
                        .eq(CheckInRecord::getScheduleId, scheduleId)
                        .eq(CheckInRecord::getUserId, userId)
                        .eq(CheckInRecord::getRole, "MEMBER"));

        if (existing != null) {
            existing.setStatus(status);
            existing.setCheckInTime("SIGNED".equals(status) ? LocalDateTime.now() : null);
            existing.setUpdateTime(LocalDateTime.now());
            checkInRecordMapper.updateById(existing);
        } else {
            existing = new CheckInRecord();
            existing.setScheduleId(scheduleId);
            existing.setUserId(userId);
            existing.setRole("MEMBER");
            existing.setStatus(status);
            existing.setCheckInTime("SIGNED".equals(status) ? LocalDateTime.now() : null);
            existing.setCreateTime(LocalDateTime.now());
            checkInRecordMapper.insert(existing);
        }

        // 更新后检查自动扣费
        if ("SIGNED".equals(status)) {
            checkAndAutoDeduct(schedule);
        }

        return existing;
    }

    /* ─── 自动扣费: 双方签到后按小时扣款 ─── */

    private void checkAndAutoDeduct(CourseSchedule schedule) {
        // Only for private coaching schedules (member_id is set)
        if (schedule.getMemberId() == null) return;

        Long enrollmentId = schedule.getEnrollmentId();
        if (enrollmentId == null) return;

        Enrollment enrollment = enrollmentMapper.selectById(enrollmentId);
        if (enrollment == null) return;

        // Must be private coaching enrollment (has coachId)
        if (enrollment.getCoachId() == null) return;

        // Must have auto-deduct agreed
        if (enrollment.getAutoDeductAgreed() == null || enrollment.getAutoDeductAgreed() != 1) return;

        // Check if both coach and member have signed in
        CheckInRecord memberCheckIn = checkInRecordMapper.selectOne(
                new LambdaQueryWrapper<CheckInRecord>()
                        .eq(CheckInRecord::getScheduleId, schedule.getId())
                        .eq(CheckInRecord::getUserId, schedule.getMemberId())
                        .eq(CheckInRecord::getRole, "MEMBER")
                        .eq(CheckInRecord::getStatus, "SIGNED"));
        if (memberCheckIn == null) return;

        CheckInRecord coachCheckIn = checkInRecordMapper.selectOne(
                new LambdaQueryWrapper<CheckInRecord>()
                        .eq(CheckInRecord::getScheduleId, schedule.getId())
                        .eq(CheckInRecord::getUserId, schedule.getCoachId())
                        .eq(CheckInRecord::getRole, "COACH")
                        .eq(CheckInRecord::getStatus, "SIGNED"));
        if (coachCheckIn == null) return;

        // Both signed in — check if already deducted (prevent double charge)
        WalletTransaction existingDeduct = walletTransactionMapper.selectOne(
                new LambdaQueryWrapper<WalletTransaction>()
                        .eq(WalletTransaction::getUserId, schedule.getMemberId())
                        .eq(WalletTransaction::getType, "AUTO_DEDUCT")
                        .like(WalletTransaction::getRemark, "schedule:" + schedule.getId()));
        if (existingDeduct != null) return;

        // Get price per session from coach profile
        PrivateCoachProfile profile = profileMapper.selectOne(
                new LambdaQueryWrapper<PrivateCoachProfile>()
                        .eq(PrivateCoachProfile::getCoachId, schedule.getCoachId()));
        BigDecimal pricePerSession = (profile != null && profile.getPricePerSession() != null)
                ? profile.getPricePerSession() : BigDecimal.ZERO;

        if (pricePerSession.compareTo(BigDecimal.ZERO) <= 0) return;

        // Check member balance
        User member = userMapper.selectById(schedule.getMemberId());
        if (member == null) return;
        BigDecimal memberBalance = member.getBalance() != null ? member.getBalance() : BigDecimal.ZERO;
        if (memberBalance.compareTo(pricePerSession) < 0) {
            // Insufficient balance - still record as not deducted
            return;
        }

        // Deduct member balance
        member.setBalance(memberBalance.subtract(pricePerSession));
        member.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(member);

        // Record member wallet transaction
        WalletTransaction memberTx = new WalletTransaction();
        memberTx.setUserId(schedule.getMemberId());
        memberTx.setAmount(pricePerSession.negate());
        memberTx.setType("AUTO_DEDUCT");
        memberTx.setRemark("私教课自动扣费 - schedule:" + schedule.getId());
        memberTx.setCreateTime(LocalDateTime.now());
        walletTransactionMapper.insert(memberTx);

        // Credit coach balance
        User coach = userMapper.selectById(schedule.getCoachId());
        if (coach != null) {
            BigDecimal coachBalance = coach.getBalance() != null ? coach.getBalance() : BigDecimal.ZERO;
            coach.setBalance(coachBalance.add(pricePerSession));
            coach.setUpdateTime(LocalDateTime.now());

            // 累计收入（只增不减，用于段位判定）
            BigDecimal coachEarnings = coach.getTotalEarnings() == null ? BigDecimal.ZERO : coach.getTotalEarnings();
            coach.setTotalEarnings(coachEarnings.add(pricePerSession));

            userMapper.updateById(coach);

            // Record coach wallet transaction
            WalletTransaction coachTx = new WalletTransaction();
            coachTx.setUserId(schedule.getCoachId());
            coachTx.setAmount(pricePerSession);
            coachTx.setType("COACH_INCOME");
            coachTx.setRemark("私教课收入 - schedule:" + schedule.getId());
            coachTx.setCreateTime(LocalDateTime.now());
            walletTransactionMapper.insert(coachTx);
        }
    }

    public CheckInRecord getCheckInStatus(Long scheduleId, Long userId, String role) {
        return checkInRecordMapper.selectOne(
                new LambdaQueryWrapper<CheckInRecord>()
                        .eq(CheckInRecord::getScheduleId, scheduleId)
                        .eq(CheckInRecord::getUserId, userId)
                        .eq(CheckInRecord::getRole, role));
    }

    public List<CheckInRecord> getScheduleCheckIns(Long scheduleId) {
        return checkInRecordMapper.selectList(
                new LambdaQueryWrapper<CheckInRecord>()
                        .eq(CheckInRecord::getScheduleId, scheduleId)
                        .orderByAsc(CheckInRecord::getRole)
                        .orderByAsc(CheckInRecord::getUserId));
    }

    public List<CheckInRecord> getUserCheckInHistory(Long userId, String role, LocalDateTime from, LocalDateTime to) {
        LambdaQueryWrapper<CheckInRecord> wrapper = new LambdaQueryWrapper<CheckInRecord>()
                .eq(CheckInRecord::getUserId, userId)
                .eq(CheckInRecord::getRole, role);
        if (from != null) wrapper.ge(CheckInRecord::getCreateTime, from);
        if (to != null) wrapper.le(CheckInRecord::getCreateTime, to);
        wrapper.orderByDesc(CheckInRecord::getCreateTime);
        return checkInRecordMapper.selectList(wrapper);
    }

    public Map<String, Object> getCheckInStats(Long userId, String role) {
        List<CheckInRecord> records = checkInRecordMapper.selectList(
                new LambdaQueryWrapper<CheckInRecord>()
                        .eq(CheckInRecord::getUserId, userId)
                        .eq(CheckInRecord::getRole, role));

        long totalRecords = records.size();
        long signedCount = records.stream().filter(r -> "SIGNED".equals(r.getStatus())).count();
        long absentCount = records.stream().filter(r -> "ABSENT".equals(r.getStatus())).count();
        long pendingCount = records.stream().filter(r -> "PENDING".equals(r.getStatus())).count();
        double checkInRate = totalRecords > 0 ? (double) signedCount / totalRecords * 100 : 0;

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalRecords", totalRecords);
        stats.put("signedCount", signedCount);
        stats.put("absentCount", absentCount);
        stats.put("pendingCount", pendingCount);
        stats.put("checkInRate", Math.round(checkInRate * 10.0) / 10.0);
        return stats;
    }

    @Transactional
    public void generatePendingRecords(Long scheduleId) {
        CourseSchedule schedule = scheduleMapper.selectById(scheduleId);
        if (schedule == null) return;

        // Generate coach pending record
        CheckInRecord coachRecord = checkInRecordMapper.selectOne(
                new LambdaQueryWrapper<CheckInRecord>()
                        .eq(CheckInRecord::getScheduleId, scheduleId)
                        .eq(CheckInRecord::getUserId, schedule.getCoachId())
                        .eq(CheckInRecord::getRole, "COACH"));
        if (coachRecord == null) {
            CheckInRecord record = new CheckInRecord();
            record.setScheduleId(scheduleId);
            record.setUserId(schedule.getCoachId());
            record.setRole("COACH");
            record.setStatus("PENDING");
            record.setCreateTime(LocalDateTime.now());
            checkInRecordMapper.insert(record);
        }

        // For private coaching (memberId set), generate just for that member
        if (schedule.getMemberId() != null) {
            CheckInRecord existing = checkInRecordMapper.selectOne(
                    new LambdaQueryWrapper<CheckInRecord>()
                            .eq(CheckInRecord::getScheduleId, scheduleId)
                            .eq(CheckInRecord::getUserId, schedule.getMemberId())
                            .eq(CheckInRecord::getRole, "MEMBER"));
            if (existing == null) {
                CheckInRecord record = new CheckInRecord();
                record.setScheduleId(scheduleId);
                record.setUserId(schedule.getMemberId());
                record.setRole("MEMBER");
                record.setStatus("PENDING");
                record.setCreateTime(LocalDateTime.now());
                checkInRecordMapper.insert(record);
            }
            return;
        }

        // For course-based schedules, generate for all enrolled students
        if (schedule.getCourseId() != null) {
            List<Enrollment> enrollments = enrollmentMapper.selectList(
                    new LambdaQueryWrapper<Enrollment>()
                            .eq(Enrollment::getCourseId, schedule.getCourseId())
                            .in(Enrollment::getStatus, "PAID", "CONFIRMED"));

            for (Enrollment enrollment : enrollments) {
                CheckInRecord existing = checkInRecordMapper.selectOne(
                        new LambdaQueryWrapper<CheckInRecord>()
                                .eq(CheckInRecord::getScheduleId, scheduleId)
                                .eq(CheckInRecord::getUserId, enrollment.getUserId())
                                .eq(CheckInRecord::getRole, "MEMBER"));
                if (existing == null) {
                    CheckInRecord record = new CheckInRecord();
                    record.setScheduleId(scheduleId);
                    record.setUserId(enrollment.getUserId());
                    record.setRole("MEMBER");
                    record.setStatus("PENDING");
                    record.setCreateTime(LocalDateTime.now());
                    checkInRecordMapper.insert(record);
                }
            }
        }
    }

    @Scheduled(fixedRate = 300000)
    @Transactional
    public void markAbsent() {
        List<CourseSchedule> endedSchedules = scheduleMapper.selectList(
                new LambdaQueryWrapper<CourseSchedule>()
                        .lt(CourseSchedule::getEndTime, LocalDateTime.now()));

        for (CourseSchedule schedule : endedSchedules) {
            List<CheckInRecord> pendingRecords = checkInRecordMapper.selectList(
                    new LambdaQueryWrapper<CheckInRecord>()
                            .eq(CheckInRecord::getScheduleId, schedule.getId())
                            .eq(CheckInRecord::getStatus, "PENDING"));

            for (CheckInRecord record : pendingRecords) {
                record.setStatus("ABSENT");
                record.setUpdateTime(LocalDateTime.now());
                checkInRecordMapper.updateById(record);
            }
        }
    }
}
