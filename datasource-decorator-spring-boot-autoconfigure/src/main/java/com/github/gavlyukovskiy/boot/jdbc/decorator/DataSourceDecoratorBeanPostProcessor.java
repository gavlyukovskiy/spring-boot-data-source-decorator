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

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;

import javax.sql.DataSource;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * {@link BeanPostProcessor} that wraps all data source beans in {@link DataSource}
 * proxies specified in property 'spring.datasource.type'.
 *
 * @author Arthur Gavlyukovskiy
 */
public class DataSourceDecoratorBeanPostProcessor implements BeanPostProcessor, Ordered {

    private final ApplicationContext applicationContext;
    private final DataSourceDecoratorProperties properties;

    DataSourceDecoratorBeanPostProcessor(ApplicationContext applicationContext, DataSourceDecoratorProperties properties) {
        this.applicationContext = applicationContext;
        this.properties = properties;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof DataSource && !properties.getExcludeBeans().contains(beanName)) {
            DataSource dataSource = (DataSource) bean;
            DataSource decoratedDataSource = dataSource;
            Map<String, DataSourceDecorator> decorators = new LinkedHashMap<>();
            applicationContext.getBeansOfType(DataSourceDecorator.class)
                    .entrySet()
                    .stream()
                    .sorted(Entry.comparingByValue(AnnotationAwareOrderComparator.INSTANCE))
                    .forEach(entry -> decorators.put(entry.getKey(), entry.getValue()));
            StringBuilder decoratingChain = new StringBuilder(beanName);
            for (Entry<String, DataSourceDecorator> decoratorEntry : decorators.entrySet()) {
                String decoratorBeanName = decoratorEntry.getKey();
                DataSourceDecorator decorator = decoratorEntry.getValue();

                DataSource dataSourceBeforeDecorating = decoratedDataSource;
                decoratedDataSource = decorator.decorate(beanName, decoratedDataSource);

                if (dataSourceBeforeDecorating != decoratedDataSource) {
                    decoratingChain.insert(0, decoratorBeanName + " -> ");
                }
            }
            if (dataSource != decoratedDataSource) {
                return new DecoratedDataSource(dataSource, decoratedDataSource, decoratingChain.toString());
            }
        }
        return bean;
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE - 10;
    }
}
