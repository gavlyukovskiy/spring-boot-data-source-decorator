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
import com.github.gavlyukovskiy.boot.jdbc.decorator.p6spy.P6SpyConfiguration;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.metadata.DataSourcePoolMetadata;
import org.springframework.boot.autoconfigure.jdbc.metadata.DataSourcePoolMetadataProvider;
import org.springframework.boot.autoconfigure.jdbc.metadata.HikariDataSourcePoolMetadata;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for proxying DataSource.
 *
 * @author Arthur Gavlyukovskiy
 */
@Configuration
@EnableConfigurationProperties(DataSourceDecoratorProperties.class)
@ConditionalOnProperty(name = { "decorator.datasource.enabled", "spring.datasource.decorator.enabled" }, havingValue = "true", matchIfMissing = true)
@ConditionalOnBean(DataSource.class)
@AutoConfigureAfter(DataSourceAutoConfiguration.class)
@Import({
        P6SpyConfiguration.class,
        DataSourceProxyConfiguration.class,
        FlexyPoolConfiguration.Ordered.class
})
public class DataSourceDecoratorAutoConfiguration {

    @Autowired
    private SpringDataSourceDecoratorPropertiesMigrator springDataSourceDecoratorPropertiesMigrator;

    @Autowired
    private DataSourceDecoratorProperties dataSourceDecoratorProperties;

    @PostConstruct
    public void initializeDeprecatedProperties() {
        springDataSourceDecoratorPropertiesMigrator.replacePropertiesAndWarn(dataSourceDecoratorProperties);
    }

    @Bean
    @ConditionalOnBean(DataSourceDecorator.class)
    public static DataSourceDecoratorBeanPostProcessor dataSourceDecoratorBeanPostProcessor() {
        return new DataSourceDecoratorBeanPostProcessor();
    }

    /**
     * Uses real data source for hikari metadata due to failing of direct field access on {@link HikariDataSource#pool}
     * when data source is proxied by CGLIB.
     */
    @Configuration
    @ConditionalOnClass(HikariDataSource.class)
    static class HikariPoolDataSourceMetadataProviderConfiguration {

        @Bean
        public DataSourcePoolMetadataProvider hikariPoolDataSourceMetadataProvider() {
            return dataSource -> {
                if (dataSource instanceof DecoratedDataSource) {
                    dataSource = ((DecoratedDataSource) dataSource).getRealDataSource();
                }
                if (dataSource instanceof HikariDataSource) {
                    return new HikariDataSourcePoolMetadata((HikariDataSource) dataSource);
                }
                return null;
            };
        }

    }

    @Bean
    public SpringDataSourceDecoratorPropertiesMigrator springDataSourceDecoratorPropertiesMigrator(Environment environment) {
        return new SpringDataSourceDecoratorPropertiesMigrator(environment);
    }
}
