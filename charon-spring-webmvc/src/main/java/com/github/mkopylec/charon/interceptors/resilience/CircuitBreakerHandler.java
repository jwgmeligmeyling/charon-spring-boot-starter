package com.github.mkopylec.charon.interceptors.resilience;

import com.github.mkopylec.charon.interceptors.HttpRequest;
import com.github.mkopylec.charon.interceptors.HttpRequestExecution;
import com.github.mkopylec.charon.interceptors.HttpResponse;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.micrometer.tagged.TaggedCircuitBreakerMetrics;
import io.github.resilience4j.micrometer.tagged.TaggedCircuitBreakerMetrics.MetricNames;
import org.slf4j.Logger;

import static com.github.mkopylec.charon.interceptors.MetricsUtils.metricName;
import static io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom;
import static io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry.of;
import static io.github.resilience4j.micrometer.tagged.TaggedCircuitBreakerMetrics.ofCircuitBreakerRegistry;
import static org.slf4j.LoggerFactory.getLogger;

class CircuitBreakerHandler extends ResilienceHandler<CircuitBreakerRegistry> {

    private static final String CIRCUIT_BREAKER_METRICS_NAME = "circuit-breaker";

    private static final Logger log = getLogger(CircuitBreakerHandler.class);

    CircuitBreakerHandler() {
        // TODO Handle 5xx after https://github.com/resilience4j/resilience4j/issues/384 is done
        super(of(custom().build()));
    }

    @Override
    protected HttpResponse forwardRequest(HttpRequest request, HttpRequestExecution execution) {
        log.trace("[Start] Circuit breaker for '{}' request mapping", execution.getMappingName());
        CircuitBreaker circuitBreaker = registry.circuitBreaker(execution.getMappingName());
        setupMetrics(registry -> createMetrics(registry, execution.getMappingName()));
        HttpResponse response = circuitBreaker.executeSupplier(() -> execution.execute(request));
        log.trace("[End] Circuit breaker for '{}' request mapping", execution.getMappingName());
        return response;
    }

    @Override
    public int getOrder() {
        return CIRCUIT_BREAKER_HANDLER_ORDER;
    }

    private TaggedCircuitBreakerMetrics createMetrics(CircuitBreakerRegistry registry, String mappingName) {
        String bufferedCallsMetricName = metricName(mappingName, CIRCUIT_BREAKER_METRICS_NAME, "buffered-calls");
        String callsMetricName = metricName(mappingName, CIRCUIT_BREAKER_METRICS_NAME, "calls");
        String maxBufferedCallsMetricName = metricName(mappingName, CIRCUIT_BREAKER_METRICS_NAME, "max-buffered-calls");
        String stateMetricName = metricName(mappingName, CIRCUIT_BREAKER_METRICS_NAME, "state");
        MetricNames metricNames = MetricNames.custom()
                .bufferedCallsMetricName(bufferedCallsMetricName)
                .callsMetricName(callsMetricName)
                .maxBufferedCallsMetricName(maxBufferedCallsMetricName)
                .stateMetricName(stateMetricName)
                .build();
        return ofCircuitBreakerRegistry(metricNames, registry);
    }
}
