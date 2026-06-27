package com.zhixun.erp.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class WebFluxConfig {

    @Bean
    public Filter utf8SseFilter() {
        return new Filter() {
            @Override
            public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                    throws IOException, ServletException {
                HttpServletRequest httpRequest = (HttpServletRequest) request;
                String accept = httpRequest.getHeader("Accept");
                if (accept != null && accept.contains("text/event-stream")) {
                    response.setContentType("text/event-stream;charset=UTF-8");
                }
                chain.doFilter(request, response);
            }
        };
    }
}
