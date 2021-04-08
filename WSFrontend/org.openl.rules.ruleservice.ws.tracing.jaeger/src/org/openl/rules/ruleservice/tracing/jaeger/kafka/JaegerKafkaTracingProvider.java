package org.openl.rules.ruleservice.tracing.jaeger.kafka;

import io.opentracing.contrib.kafka.TracingConsumerInterceptor;
import io.opentracing.contrib.kafka.TracingProducerInterceptor;
import org.apache.kafka.common.header.Headers;
import org.openl.rules.ruleservice.spi.KafkaTracingProvider;

import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.contrib.kafka.TracingKafkaUtils;
import io.opentracing.util.GlobalTracer;

public class JaegerKafkaTracingProvider implements KafkaTracingProvider {

    /**
     * This method injects the tracing headers from consumer to the producer for span context propagation.
     * 
     * @param consumerHeaders
     * @param producerHeaders
     */
    @Override
    public void injectTracingHeaders(Object consumerHeaders, Object producerHeaders) {
        if (GlobalTracer.isRegistered()) {
            Tracer tracer = GlobalTracer.get();
            SpanContext spanContext = TracingKafkaUtils.extractSpanContext((Headers) consumerHeaders, tracer);
            TracingKafkaUtils.inject(spanContext, (Headers) producerHeaders, tracer);
        }

    }

    /**
     * Start span as a child of context which comes with consumer headers.
     * 
     * @return active span
     */
    @Override
    public Object start(Object consumerHeaders, String name) {
        if (GlobalTracer.isRegistered()) {
            Tracer tracer = GlobalTracer.get();
            SpanContext spanContext = TracingKafkaUtils.extractSpanContext((Headers) consumerHeaders, tracer);
            return tracer.buildSpan(name).withTag("Service Name", name).asChildOf(spanContext).start();
        } else {
            return null;
        }
    }

    /**
     * Finish the current active span
     *
     */
    @Override
    public void finish(Object span) {
        if (span != null) {
            ((Span) span).finish();
        }
    }

    @Override
    public String getConsumerInterceptorProviders() {
        return GlobalTracer.isRegistered() ? TracingConsumerInterceptor.class.getName() : "";
    }

    @Override
    public String getProducerInterceptorProviders() {
        return GlobalTracer.isRegistered() ? TracingProducerInterceptor.class.getName() : "";
    }
}
