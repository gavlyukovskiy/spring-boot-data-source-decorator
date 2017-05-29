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

package com.github.gavlyukovskiy.decorator.boot;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;

import javax.sql.DataSource;

import java.util.ArrayList;
import java.util.List;

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
        if (bean instanceof DataSource && !this.properties.getExcludeBeans().contains(beanName)) {
            DataSource dataSource = (DataSource) bean;
            DataSource decoratedDataSource = dataSource;
            List<DataSourceDecorator> decorators = new ArrayList<>();
            decorators.addAll(this.applicationContext.getBeansOfType(DataSourceDecorator.class).values());
            AnnotationAwareOrderComparator.sort(decorators);
            for (DataSourceDecorator decorator : decorators) {
                decoratedDataSource = decorator.decorate(beanName, decoratedDataSource);
            }
            if (dataSource != decoratedDataSource) {
                return new DecoratedDataSource(dataSource, decoratedDataSource);
            }
        }
        return bean;
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE - 10;
    }
}
