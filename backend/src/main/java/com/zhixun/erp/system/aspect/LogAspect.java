package com.zhixun.erp.system.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhixun.erp.system.entity.SysLog;
import com.zhixun.erp.system.service.LogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class LogAspect {

    private final LogService logService;
    private final ObjectMapper objectMapper;

    @Pointcut("execution(* com.zhixun.erp..controller.*Controller.*(..))")
    public void controllerPointcut() {}

    @Around("controllerPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        Object result = joinPoint.proceed();

        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) return result;

            HttpServletRequest request = attributes.getRequest();
            String methodName = joinPoint.getSignature().toShortString();
            String uri = request.getRequestURI();
            String httpMethod = request.getMethod();

            if (uri.contains("/log/list") || uri.contains("/systemMonitor")) {
                return result;
            }

            SysLog sysLog = new SysLog();
            sysLog.setMethod(httpMethod + " " + methodName);
            sysLog.setIp(getClientIp(request));
            sysLog.setCreateTime(LocalDateTime.now());

            try {
                Object[] args = joinPoint.getArgs();
                if (args != null && args.length > 0) {
                    String params = objectMapper.writeValueAsString(args);
                    sysLog.setParams(params.length() > 2000 ? params.substring(0, 2000) : params);
                }
            } catch (Exception ignored) {}

            String operation = resolveOperation(httpMethod, uri);
            sysLog.setOperation(operation);

            String userId = request.getHeader("X-User-Id");
            String username = request.getHeader("X-Username");
            String role = request.getHeader("X-Role");
            if (userId != null) {
                try { sysLog.setUserId(Long.parseLong(userId)); } catch (NumberFormatException ignored) {}
            }
            sysLog.setUsername(username != null ? username : "unknown");
            sysLog.setRole(role != null ? role : "unknown");

            logService.saveLog(sysLog);
        } catch (Exception e) {
            log.warn("记录操作日志失败: {}", e.getMessage());
        }

        return result;
    }

    private String resolveOperation(String httpMethod, String uri) {
        String base = "";
        if (uri.contains("/auth/login")) return "用户登录";
        if (uri.contains("/auth/register")) return "用户注册";
        if (uri.contains("/auth/change-password")) return "修改密码";
        if (uri.contains("/auth/reset-password")) return "重置密码";

        if (uri.contains("/user")) base = "用户";
        else if (uri.contains("/course")) base = "课程";
        else if (uri.contains("/lesson")) base = "课时";
        else if (uri.contains("/enrollment")) base = "选课";
        else if (uri.contains("/finance")) base = "财务";
        else if (uri.contains("/log")) base = "日志";
        else if (uri.contains("/db")) base = "数据库";
        else base = "系统";

        return switch (httpMethod) {
            case "POST" -> "新增" + base;
            case "PUT" -> "编辑" + base;
            case "DELETE" -> "删除" + base;
            case "GET" -> "查询" + base;
            default -> base + "操作";
        };
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
