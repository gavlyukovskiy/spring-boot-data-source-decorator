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
 * @since 1.2.2
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
        metrics.afterAcquireConnection(connectionInformation.getConnection(), connectionInformation.getTimeToGetConnectionNs(), TimeUnit.NANOSECONDS, e);
    }

    @Override
    public void onAfterConnectionClose(ConnectionInformation connectionInformation, SQLException e) {
        String dataSourceName = p6SpyDataSourceNameResolver.resolveDataSourceName(connectionInformation.getDataSource());
        DataSourceMetricsHolder metrics = dataSourceMetricsBinder.getMetrics(dataSourceName);
        metrics.closeConnection(connectionInformation.getConnection());
    }
}
