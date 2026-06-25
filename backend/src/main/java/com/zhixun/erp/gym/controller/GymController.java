package com.zhixun.erp.gym.controller;

import com.zhixun.erp.common.response.Result;
import com.zhixun.erp.gym.entity.GymCard;
import com.zhixun.erp.gym.entity.GymMembership;
import com.zhixun.erp.gym.service.GymService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@CrossOrigin
@RestController
@RequiredArgsConstructor
@RequestMapping("/gym")
public class GymController {

    private final GymService gymService;

    /* ─── 健身卡 CRUD（管理员） ─── */

    @GetMapping("/cards")
    public Result<List<GymCard>> listCards(
            @RequestParam(required = false) String status) {
        return Result.success(gymService.listCards(status));
    }

    @PostMapping("/card")
    public Result<GymCard> createCard(@RequestBody GymCard card) {
        return Result.success("创建成功", gymService.createCard(card));
    }

    @PutMapping("/card")
    public Result<GymCard> updateCard(@RequestBody GymCard card) {
        return Result.success("更新成功", gymService.updateCard(card.getId(), card));
    }

    @DeleteMapping("/card/{id}")
    public Result<Void> deleteCard(@PathVariable Long id) {
        gymService.deleteCard(id);
        return Result.success("删除成功", null);
    }

    /* ─── 会员：我的健身卡信息 ─── */

    @GetMapping("/my-membership")
    public Result<Map<String, Object>> getMyMembership(@RequestParam Long userId) {
        return Result.success(gymService.getMyMembership(userId));
    }

    /* ─── 会员：购买健身卡 ─── */

    @PostMapping("/purchase")
    public Result<GymMembership> purchaseCard(
            @RequestParam Long userId,
            @RequestParam Long cardId,
            @RequestParam(required = false) Integer trialDays) {
        return Result.success("购买成功", gymService.purchaseCard(userId, cardId, trialDays));
    }

    /* ─── 次卡副卡管理 ─── */

    @PostMapping("/sub-card")
    public Result<GymMembership> createSubCard(
            @RequestParam Long userId,
            @RequestParam Long primaryMembershipId,
            @RequestParam Long targetUserId,
            @RequestParam(required = false) String holderName) {
        return Result.success("副卡创建成功",
                gymService.createSubCard(userId, primaryMembershipId, targetUserId, holderName));
    }

    @GetMapping("/sub-cards")
    public Result<List<Map<String, Object>>> listSubCards(
            @RequestParam Long userId,
            @RequestParam Long primaryMembershipId) {
        return Result.success(gymService.listSubCards(userId, primaryMembershipId));
    }

    @DeleteMapping("/sub-card/{subMembershipId}")
    public Result<Void> revokeSubCard(
            @RequestParam Long userId,
            @PathVariable Long subMembershipId) {
        gymService.revokeSubCard(userId, subMembershipId);
        return Result.success("副卡已撤销", null);
    }
}
