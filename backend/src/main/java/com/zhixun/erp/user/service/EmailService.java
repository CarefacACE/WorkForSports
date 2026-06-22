package com.zhixun.erp.user.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final Map<String, CodeInfo> codeMap = new ConcurrentHashMap<>();
    private final Random random = new Random();

    @Value("${spring.mail.username}")
    private String fromEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public String sendCode(String email) {
        String code = String.format("%06d", random.nextInt(1000000));
        codeMap.put(email, new CodeInfo(code, System.currentTimeMillis() + 5 * 60 * 1000));

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(email);
            message.setSubject("【智训业财云】验证码");
            message.setText("您的验证码是：" + code + "，5分钟内有效。如非本人操作，请忽略此邮件。");
            mailSender.send(message);
            System.out.println("[EMAIL] 向 " + email + " 发送验证码: " + code);
        } catch (Exception e) {
            System.err.println("[EMAIL] 发送失败: " + e.getMessage());
            throw new RuntimeException("邮件发送失败，请稍后重试");
        }

        return code;
    }

    public boolean verifyCode(String email, String code) {
        CodeInfo info = codeMap.get(email);
        if (info == null) {
            return false;
        }
        if (System.currentTimeMillis() > info.expireTime) {
            codeMap.remove(email);
            return false;
        }
        if (info.code.equals(code)) {
            codeMap.remove(email);
            return true;
        }
        return false;
    }

    private static class CodeInfo {
        String code;
        long expireTime;

        CodeInfo(String code, long expireTime) {
            this.code = code;
            this.expireTime = expireTime;
        }
    }
}
