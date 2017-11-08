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

import net.ttddyy.dsproxy.listener.MethodExecutionContext;
import net.ttddyy.dsproxy.listener.MethodExecutionListener;

import javax.sql.DataSource;

import java.sql.Connection;
import java.util.concurrent.TimeUnit;

/**
 * Datasource Proxy listener to collect connection pool metrics.
 *
 * @author Arthur Gavlyukovskiy
 * @since 1.3.0
 */
public class DataSourceMetricsMethodExecutionListener implements MethodExecutionListener {

    private final DataSourceMetricsBinder dataSourceMetricsBinder;

    public DataSourceMetricsMethodExecutionListener(DataSourceMetricsBinder dataSourceMetricsBinder) {
        this.dataSourceMetricsBinder = dataSourceMetricsBinder;
    }

    @Override
    public void beforeMethod(MethodExecutionContext executionContext) {
        Object target = executionContext.getTarget();
        String methodName = executionContext.getMethod().getName();
        if (target instanceof DataSource) {
            if (methodName.equals("getConnection")) {
                DataSourceMetricsHolder metrics = dataSourceMetricsBinder.getMetrics(executionContext.getProxyConfig().getDataSourceName());
                metrics.beforeAcquireConnection();
            }
        }
    }

    @Override
    public void afterMethod(MethodExecutionContext executionContext) {
        Object target = executionContext.getTarget();
        String methodName = executionContext.getMethod().getName();
        if (target instanceof DataSource) {
            if (methodName.equals("getConnection")) {
                DataSourceMetricsHolder metrics = dataSourceMetricsBinder.getMetrics(executionContext.getProxyConfig().getDataSourceName());
                metrics.afterAcquireConnection(executionContext.getConnectionInfo(), executionContext.getElapsedTime(), TimeUnit.MILLISECONDS, executionContext.getThrown());
            }
        }
        else if (target instanceof Connection) {
            if (methodName.equals("close")) {
                DataSourceMetricsHolder metrics = dataSourceMetricsBinder.getMetrics(executionContext.getProxyConfig().getDataSourceName());
                metrics.closeConnection(executionContext.getConnectionInfo());
            }
        }
    }
}
