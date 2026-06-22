package com.zhixun.erp.course.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhixun.erp.course.entity.Course;
import com.zhixun.erp.course.entity.Enrollment;
import com.zhixun.erp.course.mapper.CourseMapper;
import com.zhixun.erp.course.mapper.EnrollmentMapper;
import com.zhixun.erp.finance.entity.WalletTransaction;
import com.zhixun.erp.finance.mapper.WalletTransactionMapper;
import com.zhixun.erp.chat.entity.ChatConversation;
import com.zhixun.erp.chat.service.ChatService;
import com.zhixun.erp.chat.service.NotificationService;
import com.zhixun.erp.user.entity.User;
import com.zhixun.erp.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EnrollmentService {

    private final EnrollmentMapper enrollmentMapper;
    private final CourseMapper courseMapper;
    private final UserMapper userMapper;
    private final WalletTransactionMapper walletTransactionMapper;
    private final ChatService chatService;
    private final NotificationService notificationService;

    @Transactional
    public Enrollment enroll(Long userId, Long courseId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new RuntimeException("课程不存在");
        }

        Enrollment exists = enrollmentMapper.selectOne(new LambdaQueryWrapper<Enrollment>()
                .eq(Enrollment::getUserId, userId)
                .eq(Enrollment::getCourseId, courseId)
                .eq(Enrollment::getStatus, "PAID"));
        if (exists != null) {
            throw new RuntimeException("已报名该课程");
        }

        Enrollment trialExists = enrollmentMapper.selectOne(new LambdaQueryWrapper<Enrollment>()
                .eq(Enrollment::getUserId, userId)
                .eq(Enrollment::getCourseId, courseId)
                .eq(Enrollment::getStatus, "TRIAL"));
        if (trialExists != null) {
            return trialExists;
        }

        Enrollment enrollment = new Enrollment();
        enrollment.setUserId(userId);
        enrollment.setCourseId(courseId);

        if (course.getPrice().compareTo(BigDecimal.ZERO) == 0) {
            enrollment.setStatus("PAID");
            enrollment.setPaidAmount(BigDecimal.ZERO);
        } else {
            enrollment.setStatus("TRIAL");
            enrollment.setPaidAmount(BigDecimal.ZERO);
        }

        enrollment.setCreateTime(LocalDateTime.now());
        enrollmentMapper.insert(enrollment);

        if ("PAID".equals(enrollment.getStatus())) {
            afterEnrollSuccess(userId, course);
        }

        return enrollment;
    }

    @Transactional
    public Enrollment payCourse(Long userId, Long courseId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new RuntimeException("课程不存在");
        }

        Enrollment enrollment = enrollmentMapper.selectOne(new LambdaQueryWrapper<Enrollment>()
                .eq(Enrollment::getUserId, userId)
                .eq(Enrollment::getCourseId, courseId));

        if (enrollment != null && "PAID".equals(enrollment.getStatus())) {
            throw new RuntimeException("已购买该课程");
        }

        BigDecimal price = course.getPrice();
        BigDecimal balance = user.getBalance() == null ? BigDecimal.ZERO : user.getBalance();
        if (balance.compareTo(price) < 0) {
            throw new RuntimeException("余额不足，课程价格: " + price + "，当前余额: " + balance);
        }

        user.setBalance(balance.subtract(price));
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);

        if (enrollment == null) {
            enrollment = new Enrollment();
            enrollment.setUserId(userId);
            enrollment.setCourseId(courseId);
            enrollment.setStatus("PAID");
            enrollment.setPaidAmount(price);
            enrollment.setCreateTime(LocalDateTime.now());
            enrollmentMapper.insert(enrollment);
        } else {
            enrollment.setStatus("PAID");
            enrollment.setPaidAmount(price);
            enrollment.setUpdateTime(LocalDateTime.now());
            enrollmentMapper.updateById(enrollment);
        }

        // 记录会员消费流水
        WalletTransaction consumeTransaction = new WalletTransaction();
        consumeTransaction.setUserId(userId);
        consumeTransaction.setAmount(price.negate());
        consumeTransaction.setType("CONSUME");
        consumeTransaction.setRemark("购买课程: " + course.getName());
        consumeTransaction.setCreateTime(LocalDateTime.now());
        walletTransactionMapper.insert(consumeTransaction);

        // 增加教练余额并记录收入流水
        User coach = userMapper.selectById(course.getCoachId());
        if (coach != null) {
            BigDecimal coachBalance = coach.getBalance() == null ? BigDecimal.ZERO : coach.getBalance();
            coach.setBalance(coachBalance.add(price));
            coach.setUpdateTime(LocalDateTime.now());
            userMapper.updateById(coach);

            WalletTransaction incomeTransaction = new WalletTransaction();
            incomeTransaction.setUserId(coach.getId());
            incomeTransaction.setAmount(price);
            incomeTransaction.setType("COURSE_INCOME");
            incomeTransaction.setRemark("卖课收入: " + course.getName());
            incomeTransaction.setCreateTime(LocalDateTime.now());
            walletTransactionMapper.insert(incomeTransaction);
        }

        afterEnrollSuccess(userId, course);

        return enrollment;
    }

    @Transactional
    public Enrollment confirmEnrollment(Long coachId, Long enrollmentId) {
        Enrollment enrollment = enrollmentMapper.selectById(enrollmentId);
        if (enrollment == null) {
            throw new RuntimeException("选课记录不存在");
        }

        Course course = courseMapper.selectById(enrollment.getCourseId());
        if (course == null || !course.getCoachId().equals(coachId)) {
            throw new RuntimeException("只能确认自己课程的选课");
        }
        if (!"TRIAL".equals(enrollment.getStatus())) {
            throw new RuntimeException("该选课不在待确认状态");
        }

        enrollment.setStatus("CONFIRMED");
        enrollment.setUpdateTime(LocalDateTime.now());
        enrollmentMapper.updateById(enrollment);
        return enrollment;
    }

    public IPage<Enrollment> getMyEnrollments(Long userId, String type, int pageNum, int pageSize) {
        Page<Enrollment> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Enrollment> wrapper = new LambdaQueryWrapper<Enrollment>()
                .eq(Enrollment::getUserId, userId);

        if (type != null && !type.isEmpty()) {
            wrapper.inSql(Enrollment::getCourseId,
                    "SELECT id FROM course WHERE type = '" + type + "' AND deleted = 0");
        }

        wrapper.orderByDesc(Enrollment::getCreateTime);
        return enrollmentMapper.selectPage(page, wrapper);
    }

    public IPage<User> getCourseStudents(Long coachId, Long courseId, String keyword, int pageNum, int pageSize) {
        Course course = courseMapper.selectById(courseId);
        if (course == null || !course.getCoachId().equals(coachId)) {
            throw new RuntimeException("只能查看自己课程的学员");
        }

        Page<User> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .inSql(User::getId,
                        "SELECT user_id FROM enrollment WHERE course_id = " + courseId + " AND deleted = 0");

        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(User::getUsername, kw)
                    .or().like(User::getRealName, kw)
                    .or().like(User::getPhone, kw));
        }

        return userMapper.selectPage(page, wrapper);
    }

    public IPage<Enrollment> getCoachEnrollments(Long coachId, String keyword, int pageNum, int pageSize) {
        Page<Enrollment> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Enrollment> wrapper = new LambdaQueryWrapper<Enrollment>()
                .inSql(Enrollment::getCourseId,
                        "SELECT id FROM course WHERE coach_id = " + coachId + " AND deleted = 0");

        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.inSql(Enrollment::getUserId,
                    "SELECT id FROM sys_user WHERE (username LIKE '%" + keyword.trim() + "%' OR real_name LIKE '%" + keyword.trim() + "%' OR phone LIKE '%" + keyword.trim() + "%') AND deleted = 0");
        }

        wrapper.orderByDesc(Enrollment::getCreateTime);
        return enrollmentMapper.selectPage(page, wrapper);
    }

    private void afterEnrollSuccess(Long userId, Course course) {
        User member = userMapper.selectById(userId);
        if (member == null) return;

        if ("PUBLIC".equals(course.getType())) {
            ChatConversation existing = chatService.getConversationsByType(userId, "GROUP")
                    .stream().filter(c -> course.getId().equals(c.getCourseId()))
                    .findFirst().orElse(null);

            if (existing == null) {
                ChatConversation group = chatService.createConversation(
                        "GROUP", course.getName() + " 群聊",
                        course.getId(), course.getCoachId(), List.of(userId));
                notificationService.sendNotification(userId,
                        "已加入群聊",
                        "你已加入课程「" + course.getName() + "」的群聊",
                        "GROUP", group.getId());
            }
        } else {
            ChatConversation existing = chatService.getConversationsByType(userId, "PRIVATE")
                    .stream().filter(c -> course.getCoachId().equals(c.getOwnerId()))
                    .findFirst().orElse(null);

            if (existing == null) {
                ChatConversation priv = chatService.createConversation(
                        "PRIVATE", null, course.getId(), course.getCoachId(), List.of(userId));
                notificationService.sendNotification(userId,
                        "已创建私信",
                        "你已与教练「" + (userMapper.selectById(course.getCoachId()) != null ?
                                userMapper.selectById(course.getCoachId()).getRealName() : "") + "」建立私信",
                        "PRIVATE", priv.getId());
            }
        }
    }
}
