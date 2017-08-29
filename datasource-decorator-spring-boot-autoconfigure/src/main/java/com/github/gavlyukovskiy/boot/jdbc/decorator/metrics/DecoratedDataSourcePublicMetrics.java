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

package com.github.gavlyukovskiy.boot.jdbc.decorator.metrics;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.DataSourcePublicMetrics;
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

/**
 * Extension for the {@link DataSourcePublicMetrics} that exposes data source proxy provider metrics.
 *
 * @author Arthur Gavlyukovskiy
 * @since 1.2.2
 */
public class DecoratedDataSourcePublicMetrics implements PublicMetrics {

    private static final String DATASOURCE_SUFFIX = "datasource";

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private Collection<DecoratedDataSourceMetricsProvider> providers;

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
            for (DecoratedDataSourceMetricsProvider provider : providers) {
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
