package com.github.gavlyukovskiy.micrometer;

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

    public DataSourceMetricsJdbcEventListener(DataSourceMetricsBinder dataSourceMetricsBinder) {
        this.dataSourceMetricsBinder = dataSourceMetricsBinder;
    }

    @Override
    public void onAfterGetConnection(ConnectionInformation connectionInformation, SQLException e) {
        DataSourceMetricsHolder metrics = dataSourceMetricsBinder.getMetrics(connectionInformation.getDataSource());
        metrics.beforeAcquireConnection(); // TODO move to onBeforeConnectionGet after implementation in p6spy
        metrics.afterAcquireConnection(connectionInformation.getConnection(), connectionInformation.getTimeToGetConnectionNs(), TimeUnit.NANOSECONDS, e);
    }

    @Override
    public void onAfterConnectionClose(ConnectionInformation connectionInformation, SQLException e) {
        DataSourceMetricsHolder metrics = dataSourceMetricsBinder.getMetrics(connectionInformation.getDataSource());
        metrics.closeConnection(connectionInformation.getConnection());
    }
}
