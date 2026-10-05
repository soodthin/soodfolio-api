package com.soodfolio.api.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TrailingSlashFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (request instanceof HttpServletRequest httpRequest) {
            String uri = httpRequest.getRequestURI();

            if (uri.length() > 1 && uri.endsWith("/")) {
                String normalizedUri = uri.substring(0, uri.length() - 1);
                HttpServletRequest wrappedRequest = new HttpServletRequestWrapper(httpRequest) {
                    @Override
                    public String getRequestURI() {
                        return normalizedUri;
                    }

                    @Override
                    public StringBuffer getRequestURL() {
                        StringBuffer url = super.getRequestURL();
                        if (url.charAt(url.length() - 1) == '/') {
                            url.deleteCharAt(url.length() - 1);
                        }
                        return url;
                    }
                };
                chain.doFilter(wrappedRequest, response);
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
