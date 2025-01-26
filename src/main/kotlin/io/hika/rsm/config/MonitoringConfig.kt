package io.hika.rsm.config

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration
import java.util.concurrent.atomic.AtomicReference

@Configuration
class MonitoringConfig {
    @Bean
    fun meterRegistry(): MeterRegistry {
        return PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    }

    @Bean
    fun replicationMetrics(meterRegistry: MeterRegistry) = ReplicationMetrics(meterRegistry)
}

class ReplicationMetrics(private val registry: MeterRegistry) {
    private val eventProcessingTime = registry.timer("replication.event.processing")
    private val snapshotCreationTime = registry.timer("replication.snapshot.creation")
    private val replicationLagRef = AtomicReference<Double>(0.0)

    init {
        registry.gauge("replication.lag", replicationLagRef) { it.get() ?: 0.0 }
    }

    fun recordEventProcessing(duration: Duration) {
        eventProcessingTime.record(duration)
    }

    fun recordSnapshotCreation(duration: Duration) {
        snapshotCreationTime.record(duration)
    }

    fun updateReplicationLag(lag: Double) {
        replicationLagRef.set(lag)
    }
}
