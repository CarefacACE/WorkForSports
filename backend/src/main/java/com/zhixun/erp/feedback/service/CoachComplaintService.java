package com.zhixun.erp.feedback.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhixun.erp.feedback.dto.FeedbackRequest;
import com.zhixun.erp.feedback.dto.SubmitComplaintRequest;
import com.zhixun.erp.feedback.entity.CoachComplaint;
import com.zhixun.erp.feedback.mapper.CoachComplaintMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CoachComplaintService {

    private final CoachComplaintMapper complaintMapper;

    @Transactional
    public CoachComplaint submitComplaint(SubmitComplaintRequest request) {
        CoachComplaint complaint = new CoachComplaint();
        complaint.setUserId(request.getUserId());
        complaint.setUsername(request.getUsername());
        complaint.setCoachId(request.getCoachId());
        complaint.setCoachUsername(request.getCoachUsername());
        complaint.setContent(request.getContent());
        complaint.setStatus("PENDING");
        complaint.setCreateTime(LocalDateTime.now());
        complaintMapper.insert(complaint);
        return complaint;
    }

    public IPage<CoachComplaint> listComplaints(int pageNum, int pageSize, Long userId, Long coachId, String status) {
        Page<CoachComplaint> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<CoachComplaint> wrapper = new LambdaQueryWrapper<CoachComplaint>()
                .orderByDesc(CoachComplaint::getCreateTime);

        if (userId != null) {
            wrapper.eq(CoachComplaint::getUserId, userId);
        }
        if (coachId != null) {
            wrapper.eq(CoachComplaint::getCoachId, coachId);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(CoachComplaint::getStatus, status);
        }

        return complaintMapper.selectPage(page, wrapper);
    }

    public Long countPending() {
        LambdaQueryWrapper<CoachComplaint> wrapper = new LambdaQueryWrapper<CoachComplaint>()
                .eq(CoachComplaint::getStatus, "PENDING");
        return complaintMapper.selectCount(wrapper);
    }

    /**
     * 会员查询自己的投诉列表
     */
    public IPage<CoachComplaint> getMyComplaints(Long userId, int pageNum, int pageSize) {
        return listComplaints(pageNum, pageSize, userId, null, null);
    }

    /**
     * 管理员处理投诉（反馈）
     */
    @Transactional
    public CoachComplaint processComplaint(Long id, FeedbackRequest request) {
        CoachComplaint complaint = complaintMapper.selectById(id);
        if (complaint == null) {
            throw new RuntimeException("投诉记录不存在");
        }
        complaint.setStatus(request.getStatus());
        complaint.setFeedback(request.getFeedback());
        complaint.setProcessedTime(LocalDateTime.now());
        complaint.setUpdateTime(LocalDateTime.now());
        complaintMapper.updateById(complaint);
        return complaint;
    }
}
