package com.github.gavlyukovskiy.boot.jdbc.decorator.metadata;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.PublicMetrics;
import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.context.ApplicationContext;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class DecoratedDataSourcePublicMetrics implements PublicMetrics {

    private static final String DATASOURCE_SUFFIX = "datasource";

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private Collection<DecoratedDataSourceMetadataProvider> providers;

    private Map<String, DataSource> dataSourceByPrefix = new HashMap<>();

    @PostConstruct
    void initialize() {
        DataSource primaryDataSource = getPrimaryDataSource();
        for (Map.Entry<String, DataSource> entry : this.applicationContext.getBeansOfType(DataSource.class).entrySet()) {
            String beanName = entry.getKey();
            DataSource bean = entry.getValue();
            String prefix = createPrefix(beanName, bean.equals(primaryDataSource));
            dataSourceByPrefix.put(prefix, bean);
        }

    }
    @Override
    public Collection<Metric<?>> metrics() {
        Collection<Metric<?>> metrics = new ArrayList<>();
        for (Entry<String, DataSource> entry : dataSourceByPrefix.entrySet()) {
            String prefix = entry.getKey();
            DataSource dataSource = entry.getValue();
            for (DecoratedDataSourceMetadataProvider provider : providers) {
                Map<String, Number> providerMetrics = provider.getMetrics(dataSource);
                if (providerMetrics != null) {
                    providerMetrics.forEach((metric, value) -> metrics.add(new Metric<>(prefix + "." + metric, value)));
                }
            }
        }
        return metrics;
    }

    private String createPrefix(String name, boolean primary) {
        if (primary) {
            return DATASOURCE_SUFFIX + ".primary";
        }
        if (name.length() > DATASOURCE_SUFFIX.length()
                && name.toLowerCase().endsWith(DATASOURCE_SUFFIX)) {
            name = name.substring(0, name.length() - DATASOURCE_SUFFIX.length());
        }
        return DATASOURCE_SUFFIX + "." + name;
    }

    private DataSource getPrimaryDataSource() {
        try {
            return this.applicationContext.getBean(DataSource.class);
        }
        catch (NoSuchBeanDefinitionException ex) {
            return null;
        }
    }
}
