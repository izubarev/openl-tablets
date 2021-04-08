package org.openl.rules.ruleservice.spi;

/**
 * SPI for tracing the Kafka service invocation
 */
public interface KafkaTracingProvider {

    void injectTracingHeaders(Object consumerHeaders, Object producerHeaders);

    Object start(Object consumerHeaders, String name);

    void finish(Object span);

    String getConsumerInterceptorProviders();

    String getProducerInterceptorProviders();
}
