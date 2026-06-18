package com.zhixun.erp.finance.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zhixun.erp.common.response.Result;
import com.zhixun.erp.finance.dto.RechargeRequest;
import com.zhixun.erp.finance.dto.UpdateBalanceRequest;
import com.zhixun.erp.finance.dto.WithdrawRequest;
import com.zhixun.erp.finance.entity.WalletTransaction;
import com.zhixun.erp.finance.service.FinanceService;
import com.zhixun.erp.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@CrossOrigin
@RestController
@RequiredArgsConstructor
@RequestMapping("/finance")
public class FinanceController {

    private final FinanceService financeService;

    @PostMapping("/recharge")
    public Result<User> recharge(@RequestBody RechargeRequest request) {
        User user = financeService.recharge(request.getUserId(), request.getAmount(), request.getRemark());
        return Result.success("充值成功", user);
    }

    @PostMapping("/withdraw")
    public Result<User> withdraw(@RequestBody WithdrawRequest request) {
        User user = financeService.withdraw(request.getUserId(), request.getAmount(), request.getRemark());
        return Result.success("提现成功", user);
    }

    @GetMapping("/balance/{userId}")
    public Result<BigDecimal> getBalance(@PathVariable Long userId) {
        return Result.success(financeService.getBalance(userId));
    }

    @GetMapping("/transactions")
    public Result<IPage<WalletTransaction>> getTransactions(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(financeService.getTransactionList(pageNum, pageSize, userId));
    }

    @PutMapping("/balance")
    public Result<User> updateBalance(@RequestBody UpdateBalanceRequest request) {
        User user = financeService.updateBalance(request.getUserId(), request.getBalance());
        return Result.success("余额更新成功", user);
    }
}
