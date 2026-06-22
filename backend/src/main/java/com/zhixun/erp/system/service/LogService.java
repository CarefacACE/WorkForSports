package com.zhixun.erp.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhixun.erp.system.entity.SysLog;
import com.zhixun.erp.system.mapper.SysLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LogService {

    private final SysLogMapper sysLogMapper;

    public IPage<SysLog> listLogs(int pageNum, int pageSize, String keyword,
                                   String role, String operationType,
                                   String startTime, String endTime) {
        Page<SysLog> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysLog> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(SysLog::getUsername, keyword)
                    .or().like(SysLog::getOperation, keyword)
                    .or().like(SysLog::getIp, keyword));
        }
        if (StringUtils.hasText(role)) {
            wrapper.eq(SysLog::getRole, role);
        }
        if (StringUtils.hasText(operationType)) {
            wrapper.like(SysLog::getOperation, operationType);
        }
        if (StringUtils.hasText(startTime)) {
            wrapper.ge(SysLog::getCreateTime, startTime);
        }
        if (StringUtils.hasText(endTime)) {
            wrapper.le(SysLog::getCreateTime, endTime);
        }

        wrapper.orderByDesc(SysLog::getCreateTime);
        return sysLogMapper.selectPage(page, wrapper);
    }

    public void deleteLog(Long id) {
        sysLogMapper.deleteById(id);
    }

    public void deleteLogs(List<Long> ids) {
        sysLogMapper.deleteBatchIds(ids);
    }

    public void saveLog(SysLog sysLog) {
        sysLogMapper.insert(sysLog);
    }
}
