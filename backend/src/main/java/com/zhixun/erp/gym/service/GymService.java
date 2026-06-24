package com.zhixun.erp.gym.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhixun.erp.finance.entity.WalletTransaction;
import com.zhixun.erp.finance.mapper.WalletTransactionMapper;
import com.zhixun.erp.gym.entity.GymCard;
import com.zhixun.erp.gym.entity.GymMembership;
import com.zhixun.erp.gym.mapper.GymCardMapper;
import com.zhixun.erp.gym.mapper.GymMembershipMapper;
import com.zhixun.erp.user.entity.User;
import com.zhixun.erp.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class GymService {

    private final GymCardMapper gymCardMapper;
    private final GymMembershipMapper gymMembershipMapper;
    private final UserMapper userMapper;
    private final WalletTransactionMapper walletTransactionMapper;

    /* ─── Gym Card CRUD (Admin) ─── */

    public List<GymCard> listCards(String status) {
        LambdaQueryWrapper<GymCard> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty()) {
            wrapper.eq(GymCard::getStatus, status);
        }
        wrapper.orderByAsc(GymCard::getCreateTime);
        return gymCardMapper.selectList(wrapper);
    }

    @Transactional
    public GymCard createCard(GymCard input) {
        input.setCreateTime(LocalDateTime.now());
        if (input.getStatus() == null) input.setStatus("ACTIVE");
        gymCardMapper.insert(input);
        return input;
    }

    @Transactional
    public GymCard updateCard(Long id, GymCard input) {
        GymCard existing = gymCardMapper.selectById(id);
        if (existing == null) throw new RuntimeException("健身卡不存在");
        if (input.getName() != null) existing.setName(input.getName());
        if (input.getType() != null) existing.setType(input.getType());
        if (input.getPrice() != null) existing.setPrice(input.getPrice());
        if (input.getDuration() != null) existing.setDuration(input.getDuration());
        if (input.getDescription() != null) existing.setDescription(input.getDescription());
        if (input.getStatus() != null) existing.setStatus(input.getStatus());
        existing.setUpdateTime(LocalDateTime.now());
        gymCardMapper.updateById(existing);
        return existing;
    }

    @Transactional
    public void deleteCard(Long id) {
        GymCard existing = gymCardMapper.selectById(id);
        if (existing == null) throw new RuntimeException("健身卡不存在");
        gymCardMapper.deleteById(id);
    }

    /* ─── Member: My Membership ─── */

    public Map<String, Object> getMyMembership(Long userId) {
        GymMembership membership = gymMembershipMapper.selectOne(
                new LambdaQueryWrapper<GymMembership>()
                        .eq(GymMembership::getUserId, userId)
                        .eq(GymMembership::getStatus, "ACTIVE")
                        .orderByDesc(GymMembership::getEndDate)
                        .last("LIMIT 1"));

        if (membership == null) return null;

        GymCard card = gymCardMapper.selectById(membership.getGymCardId());
        if (card == null) return null;

        // Check if expired
        if (membership.getEndDate() != null && membership.getEndDate().isBefore(LocalDate.now())) {
            membership.setStatus("EXPIRED");
            membership.setUpdateTime(LocalDateTime.now());
            gymMembershipMapper.updateById(membership);
            return null;
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("membershipId", membership.getId());
        result.put("cardId", card.getId());
        result.put("cardName", card.getName());
        result.put("cardType", card.getType());
        result.put("startDate", membership.getStartDate());
        result.put("endDate", membership.getEndDate());
        result.put("remainingVisits", membership.getRemainingVisits());
        result.put("paidAmount", membership.getPaidAmount());

        // Calculate remaining days
        if (membership.getEndDate() != null) {
            long daysLeft = membership.getEndDate().toEpochDay() - LocalDate.now().toEpochDay();
            result.put("remainingDays", Math.max(0, daysLeft));
        }

        return result;
    }

    /* ─── Member: Purchase Card ─── */

    @Transactional
    public GymMembership purchaseCard(Long userId, Long cardId) {
        GymCard card = gymCardMapper.selectById(cardId);
        if (card == null) throw new RuntimeException("健身卡不存在");
        if (!"ACTIVE".equals(card.getStatus())) throw new RuntimeException("该健身卡已下架");

        User user = userMapper.selectById(userId);
        if (user == null) throw new RuntimeException("用户不存在");

        BigDecimal price = card.getPrice();
        if (user.getBalance() == null || user.getBalance().compareTo(price) < 0) {
            throw new RuntimeException("余额不足，需要 " + price + " 元，当前余额 " + user.getBalance());
        }

        // Deduct balance
        user.setBalance(user.getBalance().subtract(price));
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);

        // Record transaction
        WalletTransaction tx = new WalletTransaction();
        tx.setUserId(userId);
        tx.setAmount(price.negate());
        tx.setType("GYM_CONSUME");
        tx.setRemark("购买健身卡 - " + card.getName());
        tx.setCreateTime(LocalDateTime.now());
        walletTransactionMapper.insert(tx);

        // Check if user already has an active membership of the same card
        GymMembership existing = gymMembershipMapper.selectOne(
                new LambdaQueryWrapper<GymMembership>()
                        .eq(GymMembership::getUserId, userId)
                        .eq(GymMembership::getGymCardId, cardId)
                        .eq(GymMembership::getStatus, "ACTIVE"));

        LocalDate today = LocalDate.now();

        if (existing != null && existing.getEndDate() != null && !existing.getEndDate().isBefore(today)) {
            // Renew: extend end date
            existing.setEndDate(existing.getEndDate().plusDays(card.getDuration()));
            if ("VISIT".equals(card.getType()) && card.getDuration() != null) {
                existing.setRemainingVisits(
                    (existing.getRemainingVisits() != null ? existing.getRemainingVisits() : 0) + card.getDuration()
                );
            }
            existing.setPaidAmount(existing.getPaidAmount().add(price));
            existing.setUpdateTime(LocalDateTime.now());
            gymMembershipMapper.updateById(existing);
            return existing;
        } else {
            // New membership
            GymMembership membership = new GymMembership();
            membership.setUserId(userId);
            membership.setGymCardId(cardId);
            membership.setStartDate(today);
            membership.setEndDate(today.plusDays(card.getDuration()));
            membership.setStatus("ACTIVE");
            membership.setPaidAmount(price);
            membership.setCreateTime(LocalDateTime.now());

            if ("VISIT".equals(card.getType())) {
                membership.setRemainingVisits(card.getDuration());
            }

            gymMembershipMapper.insert(membership);
            return membership;
        }
    }
}
