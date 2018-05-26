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
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
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
class DataSourceMetricsHolder {

    private final Map<Object, Long> connectionAcquireTimestamp = new ConcurrentHashMap<>();

    private final Timer connectionObtainTimer;
    private final Timer connectionUsageTimer;
    private final Counter connectionCreatedCounter;
    private final Counter connectionFailedCounter;
    private final AtomicInteger activeConnections;
    private final AtomicInteger pendingConnections;

    DataSourceMetricsHolder(String dataSourceName, MeterRegistry registry) {
        Tags tags = Tags.of("pool", dataSourceName);
        connectionObtainTimer = registry.timer("datasource.connections.acquire", tags);
        connectionUsageTimer = registry.timer("datasource.connections.usage", tags);
        connectionCreatedCounter = registry.counter("datasource.connections.created", tags);
        connectionFailedCounter = registry.counter("datasource.connections.failed", tags);
        activeConnections = registry.gauge("datasource.connections.active", tags, new AtomicInteger());
        pendingConnections = registry.gauge("datasource.connections.pending", tags, new AtomicInteger());
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
