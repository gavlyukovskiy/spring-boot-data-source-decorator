package com.github.gavlyukovskiy.micrometer;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.boot.autoconfigure.jdbc.metadata.DataSourcePoolMetadata;

import javax.sql.DataSource;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * {@link DataSource} metrics linked to micrometer's {@link MeterRegistry}.
 *
 * @author Arthur Gavlyukovskiy
 * @since 1.2.2
 */
public class DataSourceMetricsHolder implements MeterBinder {

    private final AtomicInteger activeConnections = new AtomicInteger();
    private final AtomicInteger pendingConnections = new AtomicInteger();
    private final Map<Object, Long> connectionAcquireTimestamp = new ConcurrentHashMap<>();
    private final String dataSourceName;
    private final DataSourcePoolMetadata poolMetadata;

    private Timer connectionObtainTimer;
    private Timer connectionUsageTimer;
    private Counter connectionCreatedCounter;
    private Counter connectionFailedCounter;

    DataSourceMetricsHolder(String dataSourceName, DataSourcePoolMetadata poolMetadata) {
        this.dataSourceName = dataSourceName;
        this.poolMetadata = poolMetadata;
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        connectionObtainTimer = Timer.builder("data.source.connections.wait")
                .publishPercentileHistogram()
                .tags("pool", dataSourceName)
                .register(registry);

        connectionUsageTimer = Timer.builder("data.source.connections.usage")
                .tags("pool", dataSourceName)
                .register(registry);

        connectionCreatedCounter = Counter.builder("data.source.connections.created")
                .tags("pool", dataSourceName)
                .register(registry);

        connectionFailedCounter = Counter.builder("data.source.connections.failed")
                .tags("pool", dataSourceName)
                .register(registry);

        Gauge.builder("data.source.connections.active", this, metrics -> activeConnections.doubleValue())
                .tags("pool", dataSourceName)
                .register(registry);

        Gauge.builder("data.source.connections.pending", this, metrics -> pendingConnections.doubleValue())
                .tags("pool", dataSourceName)
                .register(registry);

        if (poolMetadata != null) {
            Gauge.builder("data.source.connections.max", this, metrics -> poolMetadata.getMax())
                    .tags("pool", dataSourceName)
                    .register(registry);

            Gauge.builder("data.source.connections.min", this, metrics -> poolMetadata.getMin())
                    .tags("pool", dataSourceName)
                    .register(registry);
        }
    }

    void beforeAcquireConnection() {
        pendingConnections.incrementAndGet();
    }

    void afterAcquireConnection(Object connectionKey, long timeToAcquireConnection, TimeUnit timeUnit, Throwable e) {
        connectionObtainTimer.record(timeToAcquireConnection, timeUnit);
        pendingConnections.decrementAndGet();
        if (e == null && connectionKey != null) {
            connectionAcquireTimestamp.put(connectionKey, System.nanoTime());
            connectionCreatedCounter.increment();
            activeConnections.incrementAndGet();
        }
        else {
            connectionFailedCounter.increment();
        }
    }

    void closeConnection(Object connectionKey) {
        Long acquisitionTime = connectionAcquireTimestamp.remove(connectionKey);
        connectionUsageTimer.record(System.nanoTime() - acquisitionTime, TimeUnit.NANOSECONDS);
        activeConnections.decrementAndGet();
    }
}
