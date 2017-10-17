package com.github.gavlyukovskiy.micrometer;

import com.github.gavlyukovskiy.boot.jdbc.decorator.DecoratedDataSource;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.boot.autoconfigure.jdbc.metadata.DataSourcePoolMetadataProvider;
import org.springframework.boot.autoconfigure.jdbc.metadata.DataSourcePoolMetadataProviders;
import org.springframework.context.ApplicationContext;

import javax.sql.CommonDataSource;
import javax.sql.DataSource;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
