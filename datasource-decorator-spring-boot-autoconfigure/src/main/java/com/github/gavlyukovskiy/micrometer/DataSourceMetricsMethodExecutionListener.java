package com.github.gavlyukovskiy.micrometer;

import net.ttddyy.dsproxy.listener.MethodExecutionContext;
import net.ttddyy.dsproxy.listener.MethodExecutionListener;

import javax.sql.DataSource;

import java.sql.Connection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Datasource Proxy listener to collect connection pool metrics.
 *
 * @author Arthur Gavlyukovskiy
 * @since 1.2.2
 */
public class DataSourceMetricsMethodExecutionListener implements MethodExecutionListener {

    private final DataSourceMetricsBinder dataSourceMetricsBinder;
    private Map<Connection, DataSource> connectionToDataSource = new ConcurrentHashMap<>();

    public DataSourceMetricsMethodExecutionListener(DataSourceMetricsBinder dataSourceMetricsBinder) {
        this.dataSourceMetricsBinder = dataSourceMetricsBinder;
    }

    @Override
    public void beforeMethod(MethodExecutionContext executionContext) {
        Object target = executionContext.getTarget();
        String methodName = executionContext.getMethod().getName();
        if (target instanceof DataSource) {
            DataSource dataSource = (DataSource) target;
            DataSourceMetricsHolder metrics = dataSourceMetricsBinder.getMetrics(dataSource);
            if (methodName.equals("getConnection")) {
                metrics.beforeAcquireConnection();
            }
        }
    }

    @Override
    public void afterMethod(MethodExecutionContext executionContext) {
        Object target = executionContext.getTarget();
        String methodName = executionContext.getMethod().getName();
        if (target instanceof DataSource) {
            DataSource dataSource = (DataSource) target;
            if (methodName.equals("getConnection")) {
                Connection connection = (Connection) executionContext.getResult();
                connectionToDataSource.put(connection, dataSource);

                DataSourceMetricsHolder metrics = dataSourceMetricsBinder.getMetrics(dataSource);
                metrics.afterAcquireConnection(connection, executionContext.getElapsedTime(), executionContext.getThrown());
            }
        }
        else if (target instanceof Connection) {
            Connection connection = (Connection) target;
            if (methodName.equals("close")) {
                DataSource dataSource = connectionToDataSource.remove(connection);
                DataSourceMetricsHolder metrics = dataSourceMetricsBinder.getMetrics(dataSource);
                metrics.closeConnection(connection);
            }
        }
    }
}
