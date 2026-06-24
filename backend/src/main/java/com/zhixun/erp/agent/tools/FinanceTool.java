package com.zhixun.erp.agent.tools;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhixun.erp.finance.entity.WalletTransaction;
import com.zhixun.erp.finance.mapper.WalletTransactionMapper;
import com.zhixun.erp.user.entity.User;
import com.zhixun.erp.user.mapper.UserMapper;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class FinanceTool {

    private final UserMapper userMapper;
    private final WalletTransactionMapper walletTransactionMapper;

    @Tool(name = "query_balance", value = "查询用户的钱包余额")
    public String queryBalance(@P("用户ID") Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) return "用户不存在。";
        return String.format("当前账户余额：%.2f 元", user.getBalance() != null ? user.getBalance() : 0);
    }

    @Tool(name = "query_transactions", value = "查询用户的近期交易记录。返回最近的充值、消费、提现等流水。")
    public String queryTransactions(
            @P("用户ID") Long userId,
            @P("查询条数，默认10条") Integer limit) {
        int count = limit != null ? limit : 10;
        List<WalletTransaction> transactions = walletTransactionMapper.selectList(
                new LambdaQueryWrapper<WalletTransaction>()
                        .eq(WalletTransaction::getUserId, userId)
                        .orderByDesc(WalletTransaction::getCreateTime)
                        .last("LIMIT " + count));

        if (transactions.isEmpty()) {
            return "暂无交易记录。";
        }

        StringBuilder sb = new StringBuilder("=== 近期交易记录 ===\n");
        for (WalletTransaction t : transactions) {
            String typeLabel = switch (t.getType()) {
                case "RECHARGE" -> "充值";
                case "WITHDRAW" -> "提现";
                case "CONSUME" -> "课程消费";
                case "COURSE_INCOME" -> "卖课收入";
                case "ADJUST" -> "余额调整";
                default -> t.getType();
            };
            sb.append(String.format("%s %s %.2f 元 %s\n",
                    t.getCreateTime() != null ? t.getCreateTime().toLocalDate() : "-",
                    typeLabel,
                    t.getAmount(),
                    t.getRemark() != null ? t.getRemark() : ""));
        }
        return sb.toString();
    }
}
