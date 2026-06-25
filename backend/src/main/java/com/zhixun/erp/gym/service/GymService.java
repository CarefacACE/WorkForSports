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
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GymService {

    private final GymCardMapper gymCardMapper;
    private final GymMembershipMapper gymMembershipMapper;
    private final UserMapper userMapper;
    private final WalletTransactionMapper walletTransactionMapper;

    /* ═══════════════════════════════════════════════════════════
       Gym Card CRUD (Admin)
       ═══════════════════════════════════════════════════════════ */

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
        if (input.getCardCategory() == null) input.setCardCategory("SESSION");
        if (input.getSubCardLimit() == null) input.setSubCardLimit(2);
        gymCardMapper.insert(input);
        return input;
    }

    @Transactional
    public GymCard updateCard(Long id, GymCard input) {
        GymCard existing = gymCardMapper.selectById(id);
        if (existing == null) throw new RuntimeException("健身卡不存在");
        if (input.getName() != null) existing.setName(input.getName());
        if (input.getCardCategory() != null) existing.setCardCategory(input.getCardCategory());
        if (input.getType() != null) existing.setType(input.getType());
        if (input.getPrice() != null) existing.setPrice(input.getPrice());
        if (input.getDuration() != null) existing.setDuration(input.getDuration());
        if (input.getSubCardLimit() != null) existing.setSubCardLimit(input.getSubCardLimit());
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

    /* ═══════════════════════════════════════════════════════════
       Member: My Membership (主卡 + 副卡展示)
       ═══════════════════════════════════════════════════════════ */

    public Map<String, Object> getMyMembership(Long userId) {
        // 先查该用户的主卡
        GymMembership membership = gymMembershipMapper.selectOne(
                new LambdaQueryWrapper<GymMembership>()
                        .eq(GymMembership::getUserId, userId)
                        .eq(GymMembership::getCardHolderType, "PRIMARY")
                        .eq(GymMembership::getStatus, "ACTIVE")
                        .eq(GymMembership::getDeleted, 0)
                        .orderByDesc(GymMembership::getEndDate)
                        .last("LIMIT 1"));

        // 如果没有主卡，查副卡
        boolean isSub = false;
        if (membership == null) {
            membership = gymMembershipMapper.selectOne(
                    new LambdaQueryWrapper<GymMembership>()
                            .eq(GymMembership::getUserId, userId)
                            .eq(GymMembership::getCardHolderType, "SUB")
                            .eq(GymMembership::getStatus, "ACTIVE")
                            .eq(GymMembership::getDeleted, 0)
                            .orderByDesc(GymMembership::getEndDate)
                            .last("LIMIT 1"));
            isSub = true;
        }

        if (membership == null) return null;

        GymCard card = gymCardMapper.selectById(membership.getGymCardId());
        if (card == null) return null;

        // 检查是否过期
        boolean expired = membership.getEndDate() != null
                && membership.getEndDate().isBefore(LocalDate.now());
        if (expired) {
            membership.setStatus("EXPIRED");
            membership.setUpdateTime(LocalDateTime.now());
            gymMembershipMapper.updateById(membership);
            return null;
        }

        // 次卡次数用完
        if ("SESSION".equals(card.getCardCategory())
                && membership.getRemainingVisits() != null
                && membership.getRemainingVisits() <= 0) {
            membership.setStatus("USED_UP");
            membership.setUpdateTime(LocalDateTime.now());
            gymMembershipMapper.updateById(membership);
            return null;
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("membershipId", membership.getId());
        result.put("cardId", card.getId());
        result.put("cardName", card.getName());
        result.put("cardCategory", card.getCardCategory());
        result.put("cardType", card.getType());
        result.put("cardHolderType", membership.getCardHolderType());
        result.put("holderName", membership.getHolderName());
        result.put("startDate", membership.getStartDate());
        result.put("endDate", membership.getEndDate());
        result.put("remainingVisits", membership.getRemainingVisits());
        result.put("paidAmount", membership.getPaidAmount());

        // 剩余天数
        if (membership.getEndDate() != null) {
            long daysLeft = LocalDate.now().until(membership.getEndDate(), ChronoUnit.DAYS);
            result.put("remainingDays", Math.max(0, daysLeft));
        }

        // 如果是主卡且是次卡，展示副卡列表
        if ("PRIMARY".equals(membership.getCardHolderType()) && "SESSION".equals(card.getCardCategory())) {
            result.put("subCards", listSubCardsInternal(membership.getId()));
        }

        // 如果是副卡，展示主卡持有人信息
        if ("SUB".equals(membership.getCardHolderType()) && membership.getPrimaryMembershipId() != null) {
            GymMembership primary = gymMembershipMapper.selectById(membership.getPrimaryMembershipId());
            if (primary != null) {
                User primaryUser = userMapper.selectById(primary.getUserId());
                result.put("primaryOwnerName", primaryUser != null ? primaryUser.getRealName() : null);
                result.put("primaryMembershipId", primary.getId());
                // 副卡共享主卡的剩余次数
                result.put("remainingVisits", primary.getRemainingVisits());
            }
        }

        return result;
    }

    /* ═══════════════════════════════════════════════════════════
       Member: Purchase Card（购买健身卡）
       ═══════════════════════════════════════════════════════════ */

    @Transactional
    public GymMembership purchaseCard(Long userId, Long cardId, Integer trialDays) {
        GymCard card = gymCardMapper.selectById(cardId);
        if (card == null) throw new RuntimeException("健身卡不存在");
        if (!"ACTIVE".equals(card.getStatus())) throw new RuntimeException("该健身卡已下架");

        User user = userMapper.selectById(userId);
        if (user == null) throw new RuntimeException("用户不存在");

        BigDecimal price = card.getPrice();
        if (user.getBalance() == null || user.getBalance().compareTo(price) < 0) {
            throw new RuntimeException("余额不足，需要 " + price + " 元，当前余额 " + user.getBalance());
        }

        // 扣款
        user.setBalance(user.getBalance().subtract(price));
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);

        // 记录交易
        WalletTransaction tx = new WalletTransaction();
        tx.setUserId(userId);
        tx.setAmount(price.negate());
        tx.setType("GYM_CONSUME");
        tx.setRemark("购买健身卡 - " + card.getName());
        tx.setCreateTime(LocalDateTime.now());
        walletTransactionMapper.insert(tx);

        // 按卡类别走不同逻辑
        if ("SESSION".equals(card.getCardCategory())) {
            return purchaseSessionCard(user, card, price);
        } else {
            return purchaseTimeCard(user, card, price, trialDays);
        }
    }

    /**
     * 购买次卡：次数 = duration，有效期统一一年
     */
    private GymMembership purchaseSessionCard(User user, GymCard card, BigDecimal price) {
        // 查询是否已有同类型活跃主卡，如果是则续卡（增加次数）
        GymMembership existing = gymMembershipMapper.selectOne(
                new LambdaQueryWrapper<GymMembership>()
                        .eq(GymMembership::getUserId, user.getId())
                        .eq(GymMembership::getGymCardId, card.getId())
                        .eq(GymMembership::getCardHolderType, "PRIMARY")
                        .eq(GymMembership::getStatus, "ACTIVE")
                        .eq(GymMembership::getDeleted, 0));

        LocalDate today = LocalDate.now();

        if (existing != null) {
            // 续卡：增加次数，延长有效期
            existing.setEndDate(existing.getEndDate().plusDays(365));
            existing.setRemainingVisits(
                    (existing.getRemainingVisits() != null ? existing.getRemainingVisits() : 0) + card.getDuration()
            );
            existing.setPaidAmount(existing.getPaidAmount().add(price));
            existing.setUpdateTime(LocalDateTime.now());
            gymMembershipMapper.updateById(existing);
            return existing;
        } else {
            GymMembership membership = new GymMembership();
            membership.setUserId(user.getId());
            membership.setGymCardId(card.getId());
            membership.setCardHolderType("PRIMARY");
            membership.setStartDate(today);
            membership.setEndDate(today.plusYears(1));
            membership.setRemainingVisits(card.getDuration());
            membership.setStatus("ACTIVE");
            membership.setPaidAmount(price);
            membership.setCreateTime(LocalDateTime.now());
            gymMembershipMapper.insert(membership);
            return membership;
        }
    }

    /**
     * 购买时间卡：按类型计算到期时间
     * MONTHLY → 30天, QUARTERLY → 90天, YEARLY → 365天, TRIAL → 用户自选1-7天
     */
    private GymMembership purchaseTimeCard(User user, GymCard card, BigDecimal price, Integer trialDays) {
        LocalDate today = LocalDate.now();
        int days;

        switch (card.getType()) {
            case "MONTHLY":
                days = 30;
                break;
            case "QUARTERLY":
                days = 90;
                break;
            case "YEARLY":
                days = 365;
                break;
            case "TRIAL":
                // 体验卡：前端传入 trialDays（1-7），否则取配置的 duration
                days = trialDays != null ? Math.min(Math.max(trialDays, 1), 7) : Math.min(Math.max(card.getDuration(), 1), 7);
                break;
            default:
                days = card.getDuration() != null ? card.getDuration() : 30;
        }

        // 时间卡也可以续卡
        GymMembership existing = gymMembershipMapper.selectOne(
                new LambdaQueryWrapper<GymMembership>()
                        .eq(GymMembership::getUserId, user.getId())
                        .eq(GymMembership::getGymCardId, card.getId())
                        .eq(GymMembership::getCardHolderType, "PRIMARY")
                        .eq(GymMembership::getStatus, "ACTIVE")
                        .eq(GymMembership::getDeleted, 0));

        if (existing != null && existing.getEndDate() != null && !existing.getEndDate().isBefore(today)) {
            // 续卡：延长有效期
            existing.setEndDate(existing.getEndDate().plusDays(days));
            existing.setPaidAmount(existing.getPaidAmount().add(price));
            existing.setUpdateTime(LocalDateTime.now());
            gymMembershipMapper.updateById(existing);
            return existing;
        } else {
            GymMembership membership = new GymMembership();
            membership.setUserId(user.getId());
            membership.setGymCardId(card.getId());
            membership.setCardHolderType("PRIMARY");
            membership.setStartDate(today);
            membership.setEndDate(today.plusDays(days));
            membership.setStatus("ACTIVE");
            membership.setPaidAmount(price);
            membership.setCreateTime(LocalDateTime.now());
            gymMembershipMapper.insert(membership);
            return membership;
        }
    }

    /* ═══════════════════════════════════════════════════════════
       次卡副卡管理
       ═══════════════════════════════════════════════════════════ */

    /**
     * 主卡持有人创建副卡，分享次数给他人
     */
    @Transactional
    public GymMembership createSubCard(Long userId, Long primaryMembershipId, Long targetUserId, String holderName) {
        // 验证主卡
        GymMembership primary = gymMembershipMapper.selectById(primaryMembershipId);
        if (primary == null) throw new RuntimeException("主卡不存在");
        if (!primary.getUserId().equals(userId)) throw new RuntimeException("只能为自己的主卡创建副卡");
        if (!"PRIMARY".equals(primary.getCardHolderType())) throw new RuntimeException("该卡不是主卡");
        if (!"ACTIVE".equals(primary.getStatus())) throw new RuntimeException("主卡已失效");

        GymCard card = gymCardMapper.selectById(primary.getGymCardId());
        if (card == null) throw new RuntimeException("关联的健身卡不存在");
        if (!"SESSION".equals(card.getCardCategory())) throw new RuntimeException("只有次卡可以创建副卡");

        // 检查副卡数量限制
        long currentSubCount = gymMembershipMapper.selectCount(
                new LambdaQueryWrapper<GymMembership>()
                        .eq(GymMembership::getPrimaryMembershipId, primaryMembershipId)
                        .eq(GymMembership::getCardHolderType, "SUB")
                        .eq(GymMembership::getDeleted, 0));

        int limit = card.getSubCardLimit() != null ? card.getSubCardLimit() : 2;
        if (currentSubCount >= limit) {
            throw new RuntimeException("副卡数量已达上限（" + limit + "张）");
        }

        // 检查目标用户是否已有此主卡的副卡
        GymMembership existingSub = gymMembershipMapper.selectOne(
                new LambdaQueryWrapper<GymMembership>()
                        .eq(GymMembership::getUserId, targetUserId)
                        .eq(GymMembership::getPrimaryMembershipId, primaryMembershipId)
                        .eq(GymMembership::getCardHolderType, "SUB")
                        .eq(GymMembership::getDeleted, 0));
        if (existingSub != null) {
            throw new RuntimeException("该用户已经是此卡的副卡持有人");
        }

        // 创建副卡
        GymMembership sub = new GymMembership();
        sub.setUserId(targetUserId);
        sub.setGymCardId(card.getId());
        sub.setPrimaryMembershipId(primaryMembershipId);
        sub.setCardHolderType("SUB");
        sub.setHolderName(holderName != null ? holderName : "副卡");
        sub.setStartDate(primary.getStartDate());
        sub.setEndDate(primary.getEndDate());
        sub.setRemainingVisits(null); // 副卡不单独计次
        sub.setStatus("ACTIVE");
        sub.setPaidAmount(BigDecimal.ZERO);
        sub.setCreateTime(LocalDateTime.now());
        gymMembershipMapper.insert(sub);
        return sub;
    }

    /**
     * 列出主卡的所有副卡
     */
    public List<Map<String, Object>> listSubCards(Long userId, Long primaryMembershipId) {
        GymMembership primary = gymMembershipMapper.selectById(primaryMembershipId);
        if (primary == null) throw new RuntimeException("主卡不存在");
        if (!primary.getUserId().equals(userId)) throw new RuntimeException("无权查看");

        return listSubCardsInternal(primaryMembershipId);
    }

    private List<Map<String, Object>> listSubCardsInternal(Long primaryMembershipId) {
        List<GymMembership> subs = gymMembershipMapper.selectList(
                new LambdaQueryWrapper<GymMembership>()
                        .eq(GymMembership::getPrimaryMembershipId, primaryMembershipId)
                        .eq(GymMembership::getCardHolderType, "SUB")
                        .eq(GymMembership::getDeleted, 0));

        return subs.stream().map(sub -> {
            User targetUser = userMapper.selectById(sub.getUserId());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", sub.getId());
            m.put("userId", sub.getUserId());
            m.put("userName", targetUser != null ? targetUser.getRealName() : null);
            m.put("holderName", sub.getHolderName());
            m.put("status", sub.getStatus());
            m.put("createTime", sub.getCreateTime());
            return m;
        }).collect(Collectors.toList());
    }

    /**
     * 主卡持有人撤销副卡
     */
    @Transactional
    public void revokeSubCard(Long userId, Long subMembershipId) {
        GymMembership sub = gymMembershipMapper.selectById(subMembershipId);
        if (sub == null) throw new RuntimeException("副卡不存在");
        if (!"SUB".equals(sub.getCardHolderType())) throw new RuntimeException("该卡不是副卡");

        GymMembership primary = gymMembershipMapper.selectById(sub.getPrimaryMembershipId());
        if (primary == null) throw new RuntimeException("关联的主卡不存在");
        if (!primary.getUserId().equals(userId)) throw new RuntimeException("无权操作");

        gymMembershipMapper.deleteById(subMembershipId);
    }
}
