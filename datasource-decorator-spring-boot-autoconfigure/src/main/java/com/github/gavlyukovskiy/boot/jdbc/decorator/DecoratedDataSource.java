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

package com.github.gavlyukovskiy.boot.jdbc.decorator;

import org.springframework.jdbc.datasource.DelegatingDataSource;

import javax.sql.DataSource;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Interface that implicitly added to the CGLIB proxy of {@link DataSource}.
 *
 * Returns link of both real {@link DataSource}, decorated {@link DataSource}
 * and all decorating chain including decorator bean name, instance and result of decorating.
 *
 * @author Arthur Gavlyukovskiy
 * @since 1.2.2
 */
public class DecoratedDataSource extends DelegatingDataSource {

    private final String beanName;
    private final DataSource realDataSource;
    private final DataSource decoratedDataSource;
    private final List<DataSourceDecorationStage> decoratingChain;

    DecoratedDataSource(String beanName, DataSource realDataSource, DataSource decoratedDataSource, List<DataSourceDecorationStage> decoratingChain) {
        super(decoratedDataSource);
        this.beanName = beanName;
        this.realDataSource = realDataSource;
        this.decoratedDataSource = decoratedDataSource;
        this.decoratingChain = decoratingChain;
    }

    /**
     * Returns data source bean name.
     *
     * @return data source bean name
     */
    public String getBeanName() {
        return beanName;
    }

    /**
     * Returns initial data source, before applying any decorator.
     *
     * @return initial data source
     */
    public DataSource getRealDataSource() {
        return realDataSource;
    }

    /**
     * Returns wrapped data source with all decorators applied.
     *
     * @return decorated data source
     */
    public DataSource getDecoratedDataSource() {
        return decoratedDataSource;
    }

    /**
     * Returns list with all decorators applied on a {@link DataSource} reverse ordered with applying order.
     *
     * @return decorating information chain
     */
    public List<DataSourceDecorationStage> getDecoratingChain() {
        return decoratingChain;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T unwrap(Class<T> iface) throws SQLException {
        // Spring Boot unwrapping simply passes 'unwrap(DataSource.class)' expecting real datasource to be returned
        // if the real datasource type matches - return real datasource
        if (iface.isInstance(getRealDataSource())) {
            return (T) getRealDataSource();
        }
        // As some decorators don't consider their types during unwrapping
        // if their type is specifically requested, we can return the decorator itself
        for (DataSourceDecorationStage dataSourceDecorationStage : decoratingChain) {
            if (iface.isInstance(dataSourceDecorationStage.getDataSource())) {
                return (T) dataSourceDecorationStage.getDataSource();
            }
        }
        return super.unwrap(iface);
    }

    @Override
    public String toString() {
        return decoratingChain.stream()
                .map(entry -> entry.getBeanName() + " [" + entry.getDataSource().getClass().getName() + "]")
                .collect(Collectors.joining(" -> ")) + " -> " + beanName + " [" + realDataSource.getClass().getName() + "]";
    }
}
