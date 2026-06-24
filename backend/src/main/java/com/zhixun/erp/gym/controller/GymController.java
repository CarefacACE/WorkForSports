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

    @GetMapping("/my-membership")
    public Result<Map<String, Object>> getMyMembership(@RequestParam Long userId) {
        return Result.success(gymService.getMyMembership(userId));
    }

    @PostMapping("/purchase")
    public Result<GymMembership> purchaseCard(
            @RequestParam Long userId,
            @RequestParam Long cardId) {
        return Result.success("购买成功", gymService.purchaseCard(userId, cardId));
    }
}
