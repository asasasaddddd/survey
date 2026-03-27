package com.example.demo.Config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;

import java.io.IOException;

@Configuration
public class SecurityHeadersConfig {

    @Bean
    public FilterRegistrationBean<SecurityHeadersFilter> securityHeadersFilter() {
        FilterRegistrationBean<SecurityHeadersFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new SecurityHeadersFilter());
        registrationBean.addUrlPatterns("/*");
        registrationBean.setOrder(1);
        return registrationBean;
    }

    public static class SecurityHeadersFilter extends OncePerRequestFilter {
        @Override
        protected void doFilterInternal(jakarta.servlet.http.HttpServletRequest request,
                                        jakarta.servlet.http.HttpServletResponse response,
                                        jakarta.servlet.FilterChain filterChain) throws ServletException, IOException {
            response.setHeader("X-Content-Type-Options", "nosniff");
            response.setHeader("X-XSS-Protection", "1; mode=block");
            response.setHeader("X-Frame-Options", "DENY");
            response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
            response.setHeader("X-Download-Options", "noopen");
            response.setHeader("X-Permitted-Cross-Domain-Policies", "none");
            response.setHeader("Content-Security-Policy", "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self'; connect-src 'self'; frame-ancestors 'none'");
            // TODO: 等配置HTTPS后再启用
            // response.setHeader("Strict-Transport-Security", "max-age=16070400; includeSubDomains");
            filterChain.doFilter(request, response);
        }
    }
}
