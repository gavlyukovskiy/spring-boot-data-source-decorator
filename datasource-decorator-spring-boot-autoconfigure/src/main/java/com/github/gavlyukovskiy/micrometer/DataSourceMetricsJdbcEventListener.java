package com.github.gavlyukovskiy.micrometer;

import com.p6spy.engine.common.ConnectionInformation;
import com.p6spy.engine.event.JdbcEventListener;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.metadata.DataSourcePoolMetadata;
import org.springframework.boot.autoconfigure.jdbc.metadata.DataSourcePoolMetadataProvider;
import org.springframework.boot.autoconfigure.jdbc.metadata.DataSourcePoolMetadataProviders;
import org.springframework.context.ApplicationContext;

import javax.sql.CommonDataSource;
import javax.sql.DataSource;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * P6Spy listener to collect connection pool metrics.
 *
 * @author Arthur Gavlyukovskiy
 * @since 1.2.2
 */
public class DataSourceMetricsJdbcEventListener extends JdbcEventListener {

    private Map<CommonDataSource, DataSourceMetrics> dataSourceMetrics = new ConcurrentHashMap<>();

    private final MeterRegistry meterRegistry;
    private final ApplicationContext applicationContext;
    private final DataSourcePoolMetadataProviders providers;

    public DataSourceMetricsJdbcEventListener(ApplicationContext applicationContext, MeterRegistry meterRegistry, Collection<DataSourcePoolMetadataProvider> metadataProviders) {
        this.applicationContext = applicationContext;
        this.meterRegistry = meterRegistry;
        this.providers = new DataSourcePoolMetadataProviders(metadataProviders);
    }

    @Override
    public void onAfterGetConnection(ConnectionInformation connectionInformation, SQLException e) {
        DataSourceMetrics metrics = dataSourceMetrics.computeIfAbsent(connectionInformation.getDataSource(), dataSource -> {
            Map<String, DataSource> dataSources = applicationContext.getBeansOfType(DataSource.class);
            String dataSourceName = resolveDataSourceName(dataSource, dataSources);
            DataSourcePoolMetadata metadata = dataSource instanceof DataSource
                    ? providers.getDataSourcePoolMetadata(((DataSource) dataSource))
                    : null;
            return new DataSourceMetrics(dataSourceName, meterRegistry, metadata);
        });
        metrics.beforeAcquireConnection(); // TODO move to onBeforeConnectionGet after implementation in p6spy
        metrics.afterAcquireConnection(connectionInformation.getConnection(),
                TimeUnit.NANOSECONDS.toMillis(connectionInformation.getTimeToGetConnectionNs()), e);
    }

    @Override
    public void onAfterConnectionClose(ConnectionInformation connectionInformation, SQLException e) {
        DataSourceMetrics metrics = dataSourceMetrics.get(connectionInformation.getDataSource());
        metrics.closeConnection(connectionInformation.getConnection());
    }

    private String resolveDataSourceName(CommonDataSource dataSource, Map<String, DataSource> dataSources) {
        return dataSources.entrySet()
                .stream()
                .filter(entry -> entry.getValue() == dataSource)
                .findFirst()
                .map(Entry::getKey)
                .orElse("dataSource");
    }
}
