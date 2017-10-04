package com.github.gavlyukovskiy.micrometer;

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.stats.hist.Histogram;
import io.micrometer.core.instrument.stats.quantile.WindowSketchQuantiles;
import org.springframework.boot.autoconfigure.jdbc.metadata.DataSourcePoolMetadata;

import javax.sql.DataSource;

import java.sql.Connection;
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
    private final Map<Connection, Long> connectionAcquireTimestamp = new ConcurrentHashMap<>();
    private final String dataSourceName;
    private final DataSourcePoolMetadata poolMetadata;

    private Timer connectionObtainTimer;
    private Timer connectionUsageTimer;
    private DistributionSummary connectionCreatedSummary;
    private DistributionSummary connectionFailedSummary;

    DataSourceMetricsHolder(String dataSourceName, DataSourcePoolMetadata poolMetadata) {
        this.dataSourceName = dataSourceName;
        this.poolMetadata = poolMetadata;
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        connectionObtainTimer = Timer.builder("data.source.connection.wait")
                .tags("pool", dataSourceName)
                .register(registry);

        connectionUsageTimer = Timer.builder("data.source.connection.usage")
                .tags("pool", dataSourceName)
                .register(registry);

        connectionCreatedSummary = DistributionSummary.builder("data.source.connection")
                .quantiles(WindowSketchQuantiles.quantiles(0.5, 0.95).create())
                .histogram(Histogram.percentilesTime())
                .tags("pool", dataSourceName, "result", "created")
                .register(registry);

        connectionFailedSummary = DistributionSummary.builder("data.source.connection")
                .quantiles(WindowSketchQuantiles.quantiles(0.5, 0.95).create())
                .histogram(Histogram.percentilesTime())
                .tags("pool", dataSourceName, "result", "failed")
                .register(registry);

        Gauge.builder("data.source.connection.active", this, metrics -> activeConnections.doubleValue())
                .tags("pool", dataSourceName)
                .register(registry);

        Gauge.builder("data.source.connection.pending", this, metrics -> pendingConnections.doubleValue())
                .tags("pool", dataSourceName)
                .register(registry);

        if (poolMetadata != null) {
            Gauge.builder("data.source.connection.max", this, metrics -> poolMetadata.getMax())
                    .tags("pool", dataSourceName)
                    .register(registry);

            Gauge.builder("data.source.connection.min", this, metrics -> poolMetadata.getMin())
                    .tags("pool", dataSourceName)
                    .register(registry);
        }
    }

    void beforeAcquireConnection() {
        pendingConnections.incrementAndGet();
    }

    void afterAcquireConnection(Connection connection, long timeToAcquireConnection, TimeUnit timeUnit, Throwable e) {
        connectionObtainTimer.record(timeToAcquireConnection, timeUnit);
        pendingConnections.decrementAndGet();
        if (e == null && connection != null) {
            connectionAcquireTimestamp.put(connection, System.nanoTime());
            connectionCreatedSummary.count();
            activeConnections.incrementAndGet();
        }
        else {
            connectionFailedSummary.count();
        }
    }

    void closeConnection(Connection connection) {
        Long acquisitionTime = connectionAcquireTimestamp.remove(connection);
        connectionUsageTimer.record(System.nanoTime() - acquisitionTime, TimeUnit.NANOSECONDS);
        activeConnections.decrementAndGet();
    }
}
