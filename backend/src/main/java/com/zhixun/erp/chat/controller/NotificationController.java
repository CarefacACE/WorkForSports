package com.zhixun.erp.chat.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zhixun.erp.chat.entity.Notification;
import com.zhixun.erp.chat.service.NotificationService;
import com.zhixun.erp.common.response.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/notification")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/list")
    public Result<IPage<Notification>> getNotifications(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.success(notificationService.getNotifications(userId, pageNum, pageSize));
    }

    @PutMapping("/{id}/read")
    public Result<Void> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return Result.success("已标记为已读", null);
    }

    @GetMapping("/unread")
    public Result<Long> getUnreadCount(@RequestParam Long userId) {
        return Result.success(notificationService.getUnreadCount(userId));
    }
}
