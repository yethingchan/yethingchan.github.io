package com.example.admin.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.io.IOException;

/**
 * XSS 防护（装饰器模式）：用 HttpServletRequestWrapper 重写 getParameter/
 * getHeader，对 < > 做转义。JSON 请求体经 @RequestBody 直接读取，不受影响。
 */
@Configuration
public class XssConfig {

    @Bean
    public FilterRegistrationBean<Filter> xssFilterRegistration() {
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new XssFilter());
        registration.addUrlPatterns("/*");
        registration.setName("xssFilter");
        registration.setOrder(1);
        return registration;
    }

    static class XssFilter implements Filter {
        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                throws IOException, ServletException {
            chain.doFilter(new XssHttpServletRequestWrapper((HttpServletRequest) request), response);
        }
    }

    static class XssHttpServletRequestWrapper extends HttpServletRequestWrapper {

        public XssHttpServletRequestWrapper(HttpServletRequest request) {
            super(request);
        }

        private static String clean(String value) {
            if (!StringUtils.hasText(value)) {
                return value;
            }
            return value
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("(?i)<script", "&lt;script")
                    .replace("(?i)</script", "&lt;/script");
        }

        @Override
        public String getParameter(String name) {
            String value = super.getParameter(name);
            return value == null ? null : clean(value);
        }

        @Override
        public String[] getParameterValues(String name) {
            String[] values = super.getParameterValues(name);
            if (values == null) {
                return null;
            }
            String[] out = new String[values.length];
            for (int i = 0; i < values.length; i++) {
                out[i] = clean(values[i]);
            }
            return out;
        }

        @Override
        public String getHeader(String name) {
            String value = super.getHeader(name);
            // 不要转义 Authorization 等头部，避免破坏 Bearer Token
            if ("authorization".equalsIgnoreCase(name)) {
                return value;
            }
            return value == null ? null : clean(value);
        }
    }
}
