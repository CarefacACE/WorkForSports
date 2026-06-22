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

import jakarta.servlet.http.HttpServletResponse;

import java.math.BigDecimal;
import java.util.List;

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

    @GetMapping("/export")
    public void exportBill(
            @RequestParam Long userId,
            @RequestParam(required = false) String type,
            @RequestParam String period,
            HttpServletResponse response) throws Exception {
        List<WalletTransaction> transactions = financeService.getExportTransactions(userId, type, period);

        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment;filename=bill.csv");
        response.setCharacterEncoding("UTF-8");

        // 写入BOM头，防止Excel中文乱码
        response.getOutputStream().write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});

        StringBuilder sb = new StringBuilder();
        sb.append("时间,类型,金额,备注\n");

        for (WalletTransaction t : transactions) {
            sb.append(t.getCreateTime()).append(",");
            sb.append(getTypeLabel(t.getType())).append(",");
            sb.append(t.getAmount()).append(",");
            sb.append(t.getRemark() == null ? "" : t.getRemark().replace(",", "，"));
            sb.append("\n");
        }

        response.getOutputStream().write(sb.toString().getBytes("UTF-8"));
        response.getOutputStream().flush();
    }

    private String getTypeLabel(String type) {
        switch (type) {
            case "RECHARGE": return "充值";
            case "WITHDRAW": return "提现";
            case "CONSUME": return "课程消费";
            case "COURSE_INCOME": return "卖课收入";
            case "ADJUST": return "余额调整";
            default: return type;
        }
    }
}
