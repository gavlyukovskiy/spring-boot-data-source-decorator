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

import com.github.gavlyukovskiy.boot.jdbc.decorator.DecoratedDataSource;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.boot.autoconfigure.jdbc.metadata.DataSourcePoolMetadataProvider;
import org.springframework.boot.autoconfigure.jdbc.metadata.DataSourcePoolMetadataProviders;
import org.springframework.context.ApplicationContext;

import javax.sql.DataSource;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Composite {@link DataSource} metrics linked to micrometer's {@link MeterRegistry}.
 *
 * @author Arthur Gavlyukovskiy
 * @since 1.3.0
 */
public class DataSourceMetricsBinder implements MeterBinder {

    private final ApplicationContext applicationContext;
    private final DataSourcePoolMetadataProviders providers;

    private Map<String, DataSourceMetricsHolder> dataSourceMetrics = new ConcurrentHashMap<>();

    public DataSourceMetricsBinder(ApplicationContext applicationContext, Collection<DataSourcePoolMetadataProvider> metadataProviders) {
        this.applicationContext = applicationContext;
        this.providers = new DataSourcePoolMetadataProviders(metadataProviders);
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        Map<String, DataSource> dataSources = applicationContext.getBeansOfType(DataSource.class);

        dataSources.forEach((dataSourceName, dataSource) -> {
            if (dataSource instanceof DecoratedDataSource) {
                DataSourceMetricsHolder dataSourceMetricsHolder = new DataSourceMetricsHolder(dataSourceName, providers.getDataSourcePoolMetadata(dataSource));
                dataSourceMetricsHolder.bindTo(registry);
                dataSourceMetrics.put(dataSourceName, dataSourceMetricsHolder);
            }
        });
    }

    public DataSourceMetricsHolder getMetrics(String dataSourceName) {
        return dataSourceMetrics.get(dataSourceName);
    }
}
