package com.prop.wo.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 允许通过 query 参数 token 访问受保护资源（图片 / PDF 预览无法携带自定义请求头）
 * 仅把 token 复制为 Authorization 头，鉴权逻辑仍由 Sa-Token 统一处理
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class TokenParamFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (request instanceof HttpServletRequest httpRequest) {
            String token = httpRequest.getParameter("token");
            String header = httpRequest.getHeader("Authorization");
            if (token != null && !token.isBlank() && (header == null || header.isBlank())) {
                chain.doFilter(new TokenHeaderRequest(httpRequest, token), response);
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private static class TokenHeaderRequest extends HttpServletRequestWrapper {

        private final String token;

        TokenHeaderRequest(HttpServletRequest request, String token) {
            super(request);
            this.token = token;
        }

        @Override
        public String getHeader(String name) {
            if ("Authorization".equalsIgnoreCase(name)) {
                return "Bearer " + token;
            }
            return super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            if ("Authorization".equalsIgnoreCase(name)) {
                return Collections.enumeration(java.util.List.of("Bearer " + token));
            }
            return super.getHeaders(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            Set<String> names = new LinkedHashSet<>(Collections.list(super.getHeaderNames()));
            names.add("Authorization");
            return Collections.enumeration(names);
        }
    }
}
