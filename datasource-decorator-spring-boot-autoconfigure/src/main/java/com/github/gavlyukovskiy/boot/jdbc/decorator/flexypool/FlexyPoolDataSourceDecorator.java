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
import com.vladmihalcea.flexypool.config.Configuration;
import com.vladmihalcea.flexypool.strategy.ConnectionAcquiringStrategyFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;

import javax.sql.DataSource;

import java.util.List;

public class FlexyPoolDataSourceDecorator<T extends DataSource> implements DataSourceDecorator, Ordered {

    private final ConnectionAcquiringStrategyFactory<?, T>[] connectionAcquiringStrategyFactories;
    private final PoolAdapterFactory<T> poolAdapterFactory;
    private final Class<T> dataSourceClass;

    @Autowired(required = false)
    private List<FlexyPoolConfigurationBuilderCustomizer> customizers;

    public FlexyPoolDataSourceDecorator(
            List<ConnectionAcquiringStrategyFactory<?, T>> connectionAcquiringStrategyFactories,
            PoolAdapterFactory<T> poolAdapterFactory,
            Class<T> dataSourceClass) {
        this.connectionAcquiringStrategyFactories = toArrayNullSafe(connectionAcquiringStrategyFactories);
        this.poolAdapterFactory = poolAdapterFactory;
        this.dataSourceClass = dataSourceClass;
    }

    FlexyPoolDataSourceDecorator() {
        this.connectionAcquiringStrategyFactories = null;
        this.poolAdapterFactory = null;
        this.dataSourceClass = null;
    }

    @Override
    public DataSource decorate(String beanName, DataSource dataSource) {
        if (dataSourceClass == null) {
            // property based configuration
            return new FlexyPoolDataSource<>(dataSource);
        }
        if (dataSourceClass.isInstance(dataSource)) {
            Configuration.Builder<T> configurationBuilder = new Configuration.Builder<>(
                    beanName,
                    dataSourceClass.cast(dataSource),
                    poolAdapterFactory
            );
            if (customizers != null) {
                customizers.forEach(customizer -> customizer.customize(beanName, configurationBuilder, dataSourceClass));
            }
            return new FlexyPoolDataSource<>(configurationBuilder.build(), connectionAcquiringStrategyFactories);
        }
        return dataSource;
    }

    @SuppressWarnings("unchecked")
    private ConnectionAcquiringStrategyFactory<?, T>[] toArrayNullSafe(List<ConnectionAcquiringStrategyFactory<?, T>> factories) {
        if (factories == null) {
            return new ConnectionAcquiringStrategyFactory[0];
        }
        return factories.toArray(new ConnectionAcquiringStrategyFactory[factories.size()]);
    }

    @Override
    public int getOrder() {
        return 10;
    }
}
