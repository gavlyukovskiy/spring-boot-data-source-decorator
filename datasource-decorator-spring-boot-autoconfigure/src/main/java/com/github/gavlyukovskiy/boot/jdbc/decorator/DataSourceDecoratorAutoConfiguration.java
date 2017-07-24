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

import com.github.gavlyukovskiy.boot.jdbc.decorator.dsproxy.DataSourceProxyConfiguration;
import com.github.gavlyukovskiy.boot.jdbc.decorator.flexypool.FlexyPoolConfiguration;
import com.github.gavlyukovskiy.boot.jdbc.decorator.metadata.DecoratedDataSourcePoolMetadataProvider;
import com.github.gavlyukovskiy.boot.jdbc.decorator.p6spy.P6SpyConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.metadata.DataSourcePoolMetadataProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for proxying DataSource.
 *
 * @author Arthur Gavlyukovskiy
 */
@Configuration
@EnableConfigurationProperties({ DataSourceDecoratorProperties.class, SpringDataSourceDecoratorProperties.class })
@ConditionalOnProperty(prefix = "datasource.decorator", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnBean(DataSource.class)
@AutoConfigureAfter(DataSourceAutoConfiguration.class)
@Import({
        P6SpyConfiguration.class,
        DataSourceProxyConfiguration.class,
        FlexyPoolConfiguration.Ordered.class
})
public class DataSourceDecoratorAutoConfiguration {

    @Autowired
    private SpringDataSourceDecoratorProperties springDataSourceDecoratorProperties;

    @Autowired
    private DataSourceDecoratorProperties dataSourceDecoratorProperties;

    @PostConstruct
    public void initializeDeprecatedProperties() {
        springDataSourceDecoratorProperties.replacePropertiesAndWarn(dataSourceDecoratorProperties);
    }

    @Bean
    @ConditionalOnBean(DataSourceDecorator.class)
    public static DataSourceDecoratorBeanPostProcessor dataSourceDecoratorBeanPostProcessor() {
        return new DataSourceDecoratorBeanPostProcessor();
    }

    @Bean
    public DataSourcePoolMetadataProvider proxyDataSourcePoolMetadataProvider() {
        return new DecoratedDataSourcePoolMetadataProvider();
    }
}
