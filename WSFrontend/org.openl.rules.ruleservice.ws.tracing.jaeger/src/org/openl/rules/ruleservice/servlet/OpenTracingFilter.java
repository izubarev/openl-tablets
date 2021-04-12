package org.openl.rules.ruleservice.servlet;

import java.io.IOException;
import java.util.ServiceLoader;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;

import org.openl.rules.ruleservice.spi.ContextProvider;
import org.springframework.context.ApplicationContext;

import io.opentracing.contrib.web.servlet.filter.TracingFilter;

@WebFilter(value = "/*")
public class OpenTracingFilter implements Filter {

    public static final String JAEGER_ENABLED = "ruleservice.tracing.jaeger.enabled";

    private TracingFilter tracingFilter;
    private boolean enabled;

    @Override
    public void init(FilterConfig filterConfig) {
        ServiceLoader<ContextProvider> loader = ServiceLoader.load(ContextProvider.class);
        if (!loader.iterator().hasNext()) {
            enabled = false;
        } else {
            ContextProvider provider = loader.iterator().next();
            ApplicationContext context = provider.getContext(filterConfig.getServletContext());
            enabled = Boolean.parseBoolean(context.getEnvironment().getProperty(JAEGER_ENABLED));
            if (enabled) {
                tracingFilter = new TracingFilter();
            }
        }
    }

    @Override
    public void doFilter(ServletRequest servletRequest,
            ServletResponse servletResponse,
            FilterChain filterChain) throws IOException, ServletException {
        if (!enabled) {
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }
        tracingFilter.doFilter(servletRequest, servletResponse, filterChain);
    }

    @Override
    public void destroy() {
        if (enabled) {
            tracingFilter.destroy();
        }
    }
}
