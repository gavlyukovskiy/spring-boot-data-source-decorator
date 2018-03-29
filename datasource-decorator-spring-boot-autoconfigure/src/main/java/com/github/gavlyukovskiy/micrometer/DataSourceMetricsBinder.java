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

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.ApplicationContext;

import javax.sql.DataSource;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Composite {@link DataSource} metrics linked to micrometer's {@link MeterRegistry}.
 *
 * @author Arthur Gavlyukovskiy
 * @since 1.3.0
 */
class DataSourceMetricsBinder {

    private final ApplicationContext applicationContext;
    private final Map<String, DataSourceMetricsHolder> dataSourceMetrics = new ConcurrentHashMap<>();
    private MeterRegistry registry;

    DataSourceMetricsBinder(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    DataSourceMetricsHolder getMetrics(String dataSourceName) {
        return dataSourceMetrics.computeIfAbsent(dataSourceName, name -> new DataSourceMetricsHolder(name, getMeterRegistry()));
    }

    private MeterRegistry getMeterRegistry() {
        if (registry == null) {
            registry = applicationContext.getBean(MeterRegistry.class);
        }
        return registry;
    }
}
