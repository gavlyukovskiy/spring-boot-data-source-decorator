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

package com.github.gavlyukovskiy.boot.jdbc.decorator.flexypool;

import com.github.gavlyukovskiy.boot.jdbc.decorator.DataSourceDecorator;
import com.vladmihalcea.flexypool.FlexyPoolDataSource;
import com.vladmihalcea.flexypool.adaptor.PoolAdapterFactory;
import com.vladmihalcea.flexypool.config.FlexyPoolConfiguration;
import com.vladmihalcea.flexypool.strategy.ConnectionAcquisitionStrategyFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;

import javax.sql.DataSource;

import java.util.List;
/**
 * {@link Ordered} decorator for {@link FlexyPoolDataSource}. Supposed to be the first to be able to adjust pool size at runtime.
 *
 * @author Arthur Gavlyukovskiy
 * @since 1.1
 */
public class FlexyPoolDataSourceDecorator implements DataSourceDecorator, Ordered {

    private final ConnectionAcquisitionStrategyFactory<?, DataSource>[] connectionAcquisitionStrategyFactories;
    private final PoolAdapterFactory<DataSource> poolAdapterFactory;
    private final Class<DataSource> dataSourceClass;

    @Autowired(required = false)
    private List<FlexyPoolConfigurationBuilderCustomizer> customizers;

    @SuppressWarnings("unchecked")
    <T extends DataSource> FlexyPoolDataSourceDecorator(
            List<ConnectionAcquisitionStrategyFactory<?, T>> connectionAcquisitionStrategyFactories,
            PoolAdapterFactory<T> poolAdapterFactory,
            Class<T> dataSourceClass) {
        this.connectionAcquisitionStrategyFactories = (ConnectionAcquisitionStrategyFactory<?, DataSource>[])
                connectionAcquisitionStrategyFactories.toArray(new ConnectionAcquisitionStrategyFactory[0]);
        this.poolAdapterFactory = (PoolAdapterFactory<DataSource>) poolAdapterFactory;
        this.dataSourceClass = (Class<DataSource>) dataSourceClass;
    }

    FlexyPoolDataSourceDecorator() {
        connectionAcquisitionStrategyFactories = null;
        poolAdapterFactory = null;
        dataSourceClass = null;
    }

    @Override
    public DataSource decorate(String beanName, DataSource dataSource) {
        if (dataSourceClass == null) {
            // property based configuration
            FlexyPoolDataSource<DataSource> flexyPoolDataSource = new FlexyPoolDataSource<>(dataSource);
            flexyPoolDataSource.start();
            return flexyPoolDataSource;
        }
        if (dataSourceClass.isInstance(dataSource)) {
            FlexyPoolConfiguration.Builder<DataSource> configurationBuilder = new FlexyPoolConfiguration.Builder<>(
                    beanName,
                    dataSourceClass.cast(dataSource),
                    poolAdapterFactory
            );
            if (customizers != null) {
                customizers.forEach(customizer -> customizer.customize(beanName, configurationBuilder, dataSourceClass));
            }
            FlexyPoolDataSource<DataSource> flexyPoolDataSource = new FlexyPoolDataSource<>(configurationBuilder.build(), connectionAcquisitionStrategyFactories);
            flexyPoolDataSource.start();
            return flexyPoolDataSource;
        }
        return dataSource;
    }

    @Override
    public int getOrder() {
        return 10;
    }
}
