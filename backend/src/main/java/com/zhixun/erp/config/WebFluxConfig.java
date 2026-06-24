package com.zhixun.erp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.server.WebFilter;

@Configuration
public class WebFluxConfig {

    @Bean
    public WebFilter utf8SseFilter() {
        return (exchange, chain) -> {
            String accept = exchange.getRequest().getHeaders().getFirst("Accept");
            if (accept != null && accept.contains("text/event-stream")) {
                exchange.getResponse().getHeaders().set("Content-Type", "text/event-stream;charset=UTF-8");
            }
            return chain.filter(exchange);
        };
    }
}
