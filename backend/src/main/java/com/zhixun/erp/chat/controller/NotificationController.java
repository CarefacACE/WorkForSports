package com.zhixun.erp.chat.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zhixun.erp.chat.entity.Notification;
import com.zhixun.erp.chat.service.NotificationService;
import com.zhixun.erp.common.response.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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

    /* ─── 管理员：通知发布 ─── */

    /** 广播通知给所有用户 */
    @PostMapping("/admin/broadcast")
    public Result<Void> broadcastNotification(@RequestBody Map<String, String> body) {
        String title = body.get("title");
        String content = body.get("content");
        if (title == null || title.isEmpty()) return Result.fail(400, "标题不能为空");
        if (content == null || content.isEmpty()) return Result.fail(400, "内容不能为空");
        notificationService.broadcastNotification(title, content);
        return Result.success("广播通知已发送", null);
    }

    /** 向指定用户发送通知 */
    @PostMapping("/admin/send-to-user")
    public Result<Void> sendToUser(@RequestBody Map<String, Object> body) {
        Long userId = Long.valueOf(body.get("userId").toString());
        String title = (String) body.get("title");
        String content = (String) body.get("content");
        if (title == null || title.isEmpty()) return Result.fail(400, "标题不能为空");
        if (content == null || content.isEmpty()) return Result.fail(400, "内容不能为空");
        notificationService.sendToUser(userId, title, content);
        return Result.success("通知已发送", null);
    }
}
