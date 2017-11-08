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

import com.github.gavlyukovskiy.boot.jdbc.decorator.p6spy.P6SpyDataSourceNameResolver;
import com.p6spy.engine.common.ConnectionInformation;
import com.p6spy.engine.event.JdbcEventListener;

import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

/**
 * P6Spy listener to collect connection pool metrics.
 *
 * @author Arthur Gavlyukovskiy
 * @since 1.3.0
 */
public class DataSourceMetricsJdbcEventListener extends JdbcEventListener {

    private final DataSourceMetricsBinder dataSourceMetricsBinder;
    private final P6SpyDataSourceNameResolver p6SpyDataSourceNameResolver;

    public DataSourceMetricsJdbcEventListener(DataSourceMetricsBinder dataSourceMetricsBinder, P6SpyDataSourceNameResolver p6SpyDataSourceNameResolver) {
        this.dataSourceMetricsBinder = dataSourceMetricsBinder;
        this.p6SpyDataSourceNameResolver = p6SpyDataSourceNameResolver;
    }

    @Override
    public void onBeforeGetConnection(ConnectionInformation connectionInformation) {
        String dataSourceName = p6SpyDataSourceNameResolver.resolveDataSourceName(connectionInformation.getDataSource());
        DataSourceMetricsHolder metrics = dataSourceMetricsBinder.getMetrics(dataSourceName);
        metrics.beforeAcquireConnection();
    }

    @Override
    public void onAfterGetConnection(ConnectionInformation connectionInformation, SQLException e) {
        String dataSourceName = p6SpyDataSourceNameResolver.resolveDataSourceName(connectionInformation.getDataSource());
        DataSourceMetricsHolder metrics = dataSourceMetricsBinder.getMetrics(dataSourceName);
        metrics.afterAcquireConnection(connectionInformation, connectionInformation.getTimeToGetConnectionNs(), TimeUnit.NANOSECONDS, e);
    }

    @Override
    public void onAfterConnectionClose(ConnectionInformation connectionInformation, SQLException e) {
        String dataSourceName = p6SpyDataSourceNameResolver.resolveDataSourceName(connectionInformation.getDataSource());
        DataSourceMetricsHolder metrics = dataSourceMetricsBinder.getMetrics(dataSourceName);
        metrics.closeConnection(connectionInformation);
    }
}
