/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.gavlyukovskiy.micrometer;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.boot.autoconfigure.jdbc.metadata.DataSourcePoolMetadata;
import org.springframework.util.Assert;

import javax.sql.DataSource;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Single {@link DataSource} metrics linked to micrometer's {@link MeterRegistry}.
 *
 * @author Arthur Gavlyukovskiy
 * @since 1.3.0
 */
public class DataSourceMetricsHolder {

    private final AtomicInteger activeConnections = new AtomicInteger();
    private final AtomicInteger pendingConnections = new AtomicInteger();
    private final Map<Object, Long> connectionAcquireTimestamp = new ConcurrentHashMap<>();

    private Timer connectionObtainTimer;
    private Timer connectionUsageTimer;
    private Counter connectionCreatedCounter;
    private Counter connectionFailedCounter;

    DataSourceMetricsHolder(String dataSourceName, DataSourcePoolMetadata poolMetadata, MeterRegistry registry) {
        connectionObtainTimer = Timer.builder("data.source.connections.wait")
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
        Assert.notNull(connectionKey, "connectionKey must not be null");
        connectionObtainTimer.record(timeToAcquireConnection, timeUnit);
        pendingConnections.decrementAndGet();
        if (e == null) {
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
