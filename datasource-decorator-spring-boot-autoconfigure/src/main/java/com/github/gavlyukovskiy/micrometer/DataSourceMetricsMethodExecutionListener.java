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
                Connection connection = (Connection) executionContext.getResult();
                if (Proxy.isProxyClass(connection.getClass())) {
                    ConnectionInvocationHandler connectionInvocationHandler = (ConnectionInvocationHandler) Proxy.getInvocationHandler(connection);
                    ConnectionProxyLogic connectionProxyLogic = (ConnectionProxyLogic) new DirectFieldAccessor(connectionInvocationHandler).getPropertyValue("delegate");
                    connection = (Connection) new DirectFieldAccessor(connectionProxyLogic).getPropertyValue("connection");
                }

                DataSourceMetricsHolder metrics = dataSourceMetricsBinder.getMetrics(executionContext.getProxyConfig().getDataSourceName());
                metrics.afterAcquireConnection(connection, executionContext.getElapsedTime(), TimeUnit.MILLISECONDS, executionContext.getThrown());
            }
        }
        else if (target instanceof Connection) {
            Connection connection = (Connection) target;
            if (methodName.equals("close")) {
                DataSourceMetricsHolder metrics = dataSourceMetricsBinder.getMetrics(executionContext.getProxyConfig().getDataSourceName());
                metrics.closeConnection(connection);
            }
        }
    }
}
