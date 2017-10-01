package com.github.gavlyukovskiy.micrometer;

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
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
public class DataSourceMetrics {

    private final AtomicInteger activeConnections = new AtomicInteger();
    private final AtomicInteger totalConnections = new AtomicInteger();
    private final AtomicInteger pendingConnections = new AtomicInteger();
    private final Map<Connection, Long> connectionAcquireTimestamp = new ConcurrentHashMap<>();

    private final Timer connectionObtainTimer;
    private final DistributionSummary connectionUsageSummary;
    private final DistributionSummary connectionCreatedSummary;
    private final DistributionSummary connectionFailedSummary;

    DataSourceMetrics(String dataSourceName, MeterRegistry meterRegistry, DataSourcePoolMetadata poolMetadata) {
        connectionObtainTimer = Timer.builder("data.source.connection.wait")
                .tags("pool", dataSourceName)
                .register(meterRegistry);

        connectionUsageSummary = DistributionSummary.builder("data.source.connection.usage")
                .quantiles(WindowSketchQuantiles.quantiles(0.5, 0.95).create())
                .histogram(Histogram.linearTime(TimeUnit.MILLISECONDS, 0, 10, 20))
                .tags("pool", dataSourceName)
                .register(meterRegistry);

        connectionCreatedSummary = DistributionSummary.builder("data.source.connection.created")
                .quantiles(WindowSketchQuantiles.quantiles(0.5, 0.95).create())
                .histogram(Histogram.linearTime(TimeUnit.MILLISECONDS, 0, 10, 20))
                .tags("pool", dataSourceName)
                .register(meterRegistry);

        connectionFailedSummary = DistributionSummary.builder("data.source.connection.failed")
                .quantiles(WindowSketchQuantiles.quantiles(0.5, 0.95).create())
                .histogram(Histogram.linearTime(TimeUnit.MILLISECONDS, 0, 10, 20))
                .tags("pool", dataSourceName)
                .register(meterRegistry);

        Gauge.builder("data.source.connection.active", this, metrics -> activeConnections.doubleValue())
                .tags("pool", dataSourceName)
                .register(meterRegistry);

        Gauge.builder("data.source.connection.total", this, metrics -> totalConnections.doubleValue())
                .tags("pool", dataSourceName)
                .register(meterRegistry);

        Gauge.builder("data.source.connection.pending", this, metrics -> pendingConnections.doubleValue())
                .tags("pool", dataSourceName)
                .register(meterRegistry);

        if (poolMetadata != null) {
            Gauge.builder("data.source.connection.max", this, metrics -> poolMetadata.getMax())
                    .tags("pool", dataSourceName)
                    .register(meterRegistry);

            Gauge.builder("data.source.connection.min", this, metrics -> poolMetadata.getMin())
                    .tags("pool", dataSourceName)
                    .register(meterRegistry);
        }
    }

    void beforeAcquireConnection() {
        pendingConnections.incrementAndGet();
    }

    void afterAcquireConnection(Connection connection, long timeToAcquireConnection, Throwable e) {
        connectionObtainTimer.record(timeToAcquireConnection, TimeUnit.MILLISECONDS);
        pendingConnections.decrementAndGet();
        if (e == null && connection != null) {
            connectionAcquireTimestamp.put(connection, System.currentTimeMillis());
            connectionCreatedSummary.count();
            activeConnections.incrementAndGet();
            totalConnections.incrementAndGet();
        }
        else {
            connectionFailedSummary.count();
        }
    }

    void closeConnection(Connection connection) {
        Long acquisitionTime = connectionAcquireTimestamp.remove(connection);
        connectionUsageSummary.record(System.currentTimeMillis() - acquisitionTime);
        activeConnections.decrementAndGet();
    }
}
