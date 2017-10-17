package com.github.gavlyukovskiy.micrometer;

import net.ttddyy.dsproxy.listener.MethodExecutionContext;
import net.ttddyy.dsproxy.listener.MethodExecutionListener;
import net.ttddyy.dsproxy.proxy.ConnectionProxyLogic;
import net.ttddyy.dsproxy.proxy.jdk.ConnectionInvocationHandler;
import org.springframework.beans.DirectFieldAccessor;

import javax.sql.DataSource;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Datasource Proxy listener to collect connection pool metrics.
 *
 * @author Arthur Gavlyukovskiy
 * @since 1.2.2
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
                long connectionId = executionContext.getConnectionInfo().getConnectionId();

                DataSourceMetricsHolder metrics = dataSourceMetricsBinder.getMetrics(executionContext.getProxyConfig().getDataSourceName());
                metrics.afterAcquireConnection(connectionId, executionContext.getElapsedTime(), TimeUnit.MILLISECONDS, executionContext.getThrown());
            }
        }
        else if (target instanceof Connection) {
            if (methodName.equals("close")) {
                long connectionId = executionContext.getConnectionInfo().getConnectionId();
                DataSourceMetricsHolder metrics = dataSourceMetricsBinder.getMetrics(executionContext.getProxyConfig().getDataSourceName());
                metrics.closeConnection(connectionId);
            }
        }
    }
}
