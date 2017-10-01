package com.github.gavlyukovskiy.micrometer;

import io.micrometer.core.instrument.MeterRegistry;
import net.ttddyy.dsproxy.listener.MethodExecutionContext;
import net.ttddyy.dsproxy.listener.MethodExecutionListener;
import org.springframework.boot.autoconfigure.jdbc.metadata.DataSourcePoolMetadata;
import org.springframework.boot.autoconfigure.jdbc.metadata.DataSourcePoolMetadataProvider;
import org.springframework.boot.autoconfigure.jdbc.metadata.DataSourcePoolMetadataProviders;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;

import java.sql.Connection;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Datasource Proxy listener to collect connection pool metrics.
 *
 * @author Arthur Gavlyukovskiy
 * @since 1.2.2
 */
public class DataSourceMetricsMethodExecutionListener implements MethodExecutionListener {

    private Map<DataSource, DataSourceMetrics> dataSourceMetrics = new ConcurrentHashMap<>();
    private Map<Connection, DataSource> connectionToDataSource = new ConcurrentHashMap<>();

    private final MeterRegistry meterRegistry;
    private final DataSourcePoolMetadataProviders providers;

    public DataSourceMetricsMethodExecutionListener(MeterRegistry meterRegistry, Collection<DataSourcePoolMetadataProvider> metadataProviders) {
        this.meterRegistry = meterRegistry;
        this.providers = new DataSourcePoolMetadataProviders(metadataProviders);
    }

    @Override
    public void beforeMethod(MethodExecutionContext executionContext) {
        Object target = executionContext.getTarget();
        String methodName = executionContext.getMethod().getName();
        if (target instanceof DataSource) {
            DataSource dataSource = (DataSource) target;
            DataSourceMetrics metrics = initializeMetrics(executionContext, dataSource);
            if (methodName.equals("getConnection")) {
                metrics.beforeAcquireConnection();
            }
        }
    }

    private DataSourceMetrics initializeMetrics(MethodExecutionContext executionContext, DataSource dataSource) {
        return dataSourceMetrics.computeIfAbsent(dataSource, ds -> {
                    String dataSourceName = StringUtils.hasText(executionContext.getProxyConfig().getDataSourceName()) ? executionContext.getProxyConfig().getDataSourceName() : "dataSource";
                    DataSourcePoolMetadata metadata = providers.getDataSourcePoolMetadata(ds);
                    return new DataSourceMetrics(dataSourceName, meterRegistry, metadata);
                });
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

                DataSourceMetrics metrics = dataSourceMetrics.get(dataSource);
                metrics.afterAcquireConnection(connection, executionContext.getElapsedTime(), executionContext.getThrown());
            }
        }
        else if (target instanceof Connection) {
            Connection connection = (Connection) target;
            if (methodName.equals("close")) {
                DataSource dataSource = connectionToDataSource.remove(connection);
                DataSourceMetrics metrics = dataSourceMetrics.get(dataSource);
                metrics.closeConnection(connection);
            }
        }
    }
}
