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

import org.springframework.aop.scope.ScopedProxyUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

/**
 * {@link BeanPostProcessor} that wraps all data source beans in {@link DataSource}
 * proxies specified in property 'spring.datasource.type'.
 *
 * @author Arthur Gavlyukovskiy
 */
public class DataSourceDecoratorBeanPostProcessor implements BeanPostProcessor, Ordered, ApplicationContextAware {

    private ApplicationContext applicationContext;
    private DataSourceDecoratorProperties dataSourceDecoratorProperties;

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof DataSource
                && !ScopedProxyUtils.isScopedTarget(beanName)
                && !((bean instanceof AbstractRoutingDataSource) && getDataSourceDecoratorProperties().isIgnoreRoutingDataSources())
                && !getDataSourceDecoratorProperties().getExcludeBeans().contains(beanName)) {
            DataSource dataSource = (DataSource) bean;
            DataSource decoratedDataSource = dataSource;
            Map<String, DataSourceDecorator> decorators = new LinkedHashMap<>();
            applicationContext.getBeansOfType(DataSourceDecorator.class)
                    .entrySet()
                    .stream()
                    .sorted(Entry.comparingByValue(AnnotationAwareOrderComparator.INSTANCE))
                    .forEach(entry -> decorators.put(entry.getKey(), entry.getValue()));
            List<DataSourceDecorationStage> decoratedDataSourceChainEntries = new ArrayList<>();
            for (Entry<String, DataSourceDecorator> decoratorEntry : decorators.entrySet()) {
                String decoratorBeanName = decoratorEntry.getKey();
                DataSourceDecorator decorator = decoratorEntry.getValue();

                DataSource dataSourceBeforeDecorating = decoratedDataSource;
                decoratedDataSource = Objects.requireNonNull(decorator.decorate(beanName, decoratedDataSource),
                        "DataSourceDecorator (" + decoratorBeanName + ", " + decorator + ") should not return null");

                if (dataSourceBeforeDecorating != decoratedDataSource) {
                    decoratedDataSourceChainEntries.add(0, new DataSourceDecorationStage(decoratorBeanName, decorator, decoratedDataSource));
                }
            }
            if (dataSource != decoratedDataSource) {
                return new DecoratedDataSource(beanName, dataSource, decoratedDataSource, decoratedDataSourceChainEntries);
            }
        }
        return bean;
    }

    private DataSourceDecoratorProperties getDataSourceDecoratorProperties() {
        if (dataSourceDecoratorProperties == null) {
            dataSourceDecoratorProperties = applicationContext.getBean(DataSourceDecoratorProperties.class);
        }
        return dataSourceDecoratorProperties;
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE - 20;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
