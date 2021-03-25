package org.openl.rules.ruleservice.tracing.jaeger.filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;

import io.jaegertracing.Configuration;
import io.opentracing.Tracer;
import io.opentracing.contrib.web.servlet.filter.TracingFilter;

@WebFilter(value = "/*")
public class OpenTracingFilter implements Filter {

    public final String JAEGER_ENABLED = "ruleservice.tracing.jaeger.enabled";

    private TracingFilter tracingFilter = null;
    private boolean enabled;

    public static Tracer jaegerTracer() {
        return Configuration.fromEnv().getTracer();
    }

    @Override
    public void init(FilterConfig filterConfig) {
        enabled = Boolean.parseBoolean(System.getProperty(JAEGER_ENABLED, System.getenv(JAEGER_ENABLED)));
        if (enabled) {
            Tracer tracer = jaegerTracer();
            tracingFilter = new TracingFilter(tracer);
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
