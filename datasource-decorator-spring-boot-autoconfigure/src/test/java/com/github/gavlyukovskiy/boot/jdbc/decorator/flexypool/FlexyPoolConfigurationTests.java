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

import com.github.gavlyukovskiy.boot.jdbc.decorator.DataSourceDecoratorAutoConfiguration;
import com.github.gavlyukovskiy.boot.jdbc.decorator.DecoratedDataSource;
import com.github.gavlyukovskiy.boot.jdbc.decorator.HidePackagesClassLoader;
import com.vladmihalcea.flexypool.FlexyPoolDataSource;
import com.vladmihalcea.flexypool.connection.ConnectionRequestContext;
import com.vladmihalcea.flexypool.metric.micrometer.MicrometerMetrics;
import com.vladmihalcea.flexypool.strategy.ConnectionAcquisitionStrategy;
import com.vladmihalcea.flexypool.strategy.ConnectionAcquisitionStrategyFactory;
import com.vladmihalcea.flexypool.strategy.IncrementPoolOnTimeoutConnectionAcquisitionStrategy;
import com.vladmihalcea.flexypool.strategy.RetryConnectionAcquisitionStrategy;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.commons.dbcp2.BasicDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ReflectionUtils;

import javax.sql.DataSource;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;

class FlexyPoolConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    DataSourceAutoConfiguration.class,
                    DataSourceDecoratorAutoConfiguration.class,
                    PropertyPlaceholderAutoConfiguration.class
            ))
            .withPropertyValues("spring.sql.init.mode=never",
                    "spring.datasource.url:jdbc:h2:mem:testdb-" + ThreadLocalRandom.current().nextInt())
            .withClassLoader(new HidePackagesClassLoader("net.ttddyy.dsproxy", "com.p6spy"));

    @Test
    void testDecoratingDefaultSource() {
        contextRunner.run(context -> {
            DataSource dataSource = context.getBean(DataSource.class);
            assertDataSourceOfType(dataSource, HikariDataSource.class);
        });
    }

    @Test
    void testNoDecoratingDefaultDataSourceWithoutAdapterDependency() {
        ApplicationContextRunner contextRunner = this.contextRunner.withClassLoader(
                new HidePackagesClassLoader("net.ttddyy.dsproxy", "com.p6spy", "com.vladmihalcea.flexypool.adaptor"));

        contextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(DecoratedDataSource.class);
        });
    }

    @Test
    void testDecoratingDbcp2DataSource() {
        ApplicationContextRunner contextRunner = this.contextRunner.withPropertyValues("spring.datasource.type:" + BasicDataSource.class.getName());

        contextRunner.run(context -> {
            DataSource dataSource = context.getBean(DataSource.class);
            assertDataSourceOfType(dataSource, BasicDataSource.class);
        });
    }

    @Test
    @SuppressWarnings("unchecked")
    void testDecoratingHikariDataSourceWithDefaultStrategies() {
        ApplicationContextRunner contextRunner = this.contextRunner.withPropertyValues("spring.datasource.type:" + HikariDataSource.class.getName());

        contextRunner.run(context -> {
            DataSource dataSource = context.getBean(DataSource.class);
            assertDataSourceOfType(dataSource, HikariDataSource.class);
            FlexyPoolDataSource<HikariDataSource> flexyPoolDataSource = assertDataSourceOfType(dataSource, HikariDataSource.class);
            IncrementPoolOnTimeoutConnectionAcquisitionStrategy<HikariDataSource> strategy1 =
                    findStrategy(flexyPoolDataSource, IncrementPoolOnTimeoutConnectionAcquisitionStrategy.class);
            assertThat(strategy1).isNotNull();
            assertThat(strategy1).hasFieldOrPropertyWithValue("maxOvergrowPoolSize", 15);
            assertThat(strategy1).hasFieldOrPropertyWithValue("timeoutMillis", 500);

            RetryConnectionAcquisitionStrategy<HikariDataSource> strategy2 =
                    findStrategy(flexyPoolDataSource, RetryConnectionAcquisitionStrategy.class);
            assertThat(strategy2).isNotNull();
            assertThat(strategy2).hasFieldOrPropertyWithValue("retryAttempts", 2);
        });
    }

    @Test
    @SuppressWarnings("unchecked")
    void testDecoratingHikariDataSourceWithCustomPropertyStrategies() {
        ApplicationContextRunner contextRunner = this.contextRunner.withPropertyValues("spring.datasource.type:" + HikariDataSource.class.getName(),
                "decorator.datasource.flexy-pool.acquisition-strategy.increment-pool.max-overgrow-pool-size:35",
                "decorator.datasource.flexy-pool.acquisition-strategy.increment-pool.timeout-millis:10000",
                "decorator.datasource.flexy-pool.acquisition-strategy.retry.attempts:5")
                .withUserConfiguration(FlexyPoolHikariConfiguration.class);

        contextRunner.run(context -> {
            DataSource dataSource = context.getBean(DataSource.class);
            FlexyPoolDataSource<HikariDataSource> flexyPoolDataSource = assertDataSourceOfType(dataSource, HikariDataSource.class);
            IncrementPoolOnTimeoutConnectionAcquisitionStrategy<HikariDataSource> strategy1 =
                    findStrategy(flexyPoolDataSource, IncrementPoolOnTimeoutConnectionAcquisitionStrategy.class);
            assertThat(strategy1).isNotNull();
            assertThat(strategy1).hasFieldOrPropertyWithValue("maxOvergrowPoolSize", 35);
            assertThat(strategy1).hasFieldOrPropertyWithValue("timeoutMillis", 10000);

            RetryConnectionAcquisitionStrategy<HikariDataSource> strategy2 =
                    findStrategy(flexyPoolDataSource, RetryConnectionAcquisitionStrategy.class);
            assertThat(strategy2).isNotNull();
            assertThat(strategy2).hasFieldOrPropertyWithValue("retryAttempts", 5);
        });
    }

    @Test
    @SuppressWarnings("unchecked")
    void testDecoratingHikariDataSourceWithDeprecatedProperties() {
        ApplicationContextRunner contextRunner = this.contextRunner.withPropertyValues("spring.datasource.type:" + HikariDataSource.class.getName(),
                "decorator.datasource.flexy-pool.acquiring-strategy.increment-pool.max-overflow-pool-size:35",
                "decorator.datasource.flexy-pool.acquiring-strategy.increment-pool.timeout-millis:10000",
                "decorator.datasource.flexy-pool.acquiring-strategy.retry.attempts:5")
                .withUserConfiguration(FlexyPoolHikariConfiguration.class);

        contextRunner.run(context -> {
            DataSource dataSource = context.getBean(DataSource.class);
            FlexyPoolDataSource<HikariDataSource> flexyPoolDataSource = assertDataSourceOfType(dataSource, HikariDataSource.class);
            IncrementPoolOnTimeoutConnectionAcquisitionStrategy<HikariDataSource> strategy1 =
                    findStrategy(flexyPoolDataSource, IncrementPoolOnTimeoutConnectionAcquisitionStrategy.class);
            assertThat(strategy1).isNotNull();
            assertThat(strategy1).hasFieldOrPropertyWithValue("maxOvergrowPoolSize", 35);
            assertThat(strategy1).hasFieldOrPropertyWithValue("timeoutMillis", 10000);

            RetryConnectionAcquisitionStrategy<HikariDataSource> strategy2 =
                    findStrategy(flexyPoolDataSource, RetryConnectionAcquisitionStrategy.class);
            assertThat(strategy2).isNotNull();
            assertThat(strategy2).hasFieldOrPropertyWithValue("retryAttempts", 5);
        });
    }

    @Test
    @SuppressWarnings("unchecked")
    void testDecoratingHikariDataSourceWithCustomBeanStrategies() {
        ApplicationContextRunner contextRunner = this.contextRunner.withPropertyValues("spring.datasource.type:" + HikariDataSource.class.getName())
                .withConfiguration(AutoConfigurations.of(FlexyPoolHikariConfiguration.class, FlexyPoolCustomFactoriesHikariConfiguration.class));

        contextRunner.run(context -> {
            DataSource dataSource = context.getBean(DataSource.class);
            FlexyPoolDataSource<HikariDataSource> flexyPoolDataSource = assertDataSourceOfType(dataSource, HikariDataSource.class);
            IncrementPoolOnTimeoutConnectionAcquisitionStrategy<HikariDataSource> strategy1 =
                    findStrategy(flexyPoolDataSource, IncrementPoolOnTimeoutConnectionAcquisitionStrategy.class);
            assertThat(strategy1).isNotNull();
            assertThat(strategy1).hasFieldOrPropertyWithValue("maxOvergrowPoolSize", 35);
            assertThat(strategy1).hasFieldOrPropertyWithValue("timeoutMillis", 10000);

            RetryConnectionAcquisitionStrategy<HikariDataSource> strategy2 =
                    findStrategy(flexyPoolDataSource, RetryConnectionAcquisitionStrategy.class);
            assertThat(strategy2).isNotNull();
            assertThat(strategy2).hasFieldOrPropertyWithValue("retryAttempts", 5);

            HikariConnectionAcquiringFactory strategy3 =
                    findStrategy(flexyPoolDataSource, HikariConnectionAcquiringFactory.class);
            assertThat(strategy3).isNotNull();

            Dbcp2ConnectionAcquiringFactory strategy4 =
                    findStrategy(flexyPoolDataSource, Dbcp2ConnectionAcquiringFactory.class);
            assertThat(strategy4).isNull();
        });
    }

    @Test
    void testSettingMicrometerMetricsFactoryByDefault() {
        contextRunner.run(context -> {
            DataSource dataSource = context.getBean(DataSource.class);
            FlexyPoolDataSource<HikariDataSource> flexyPoolDataSource = assertDataSourceOfType(dataSource, HikariDataSource.class);
            assertThat(flexyPoolDataSource).extracting("metrics").isInstanceOf(MicrometerMetrics.class);
        });
    }

    @SuppressWarnings("unchecked")
    private <T extends DataSource> FlexyPoolDataSource<T> assertDataSourceOfType(DataSource dataSource, Class<T> realDataSourceClass) {
        assertThat(dataSource).isInstanceOf(DecoratedDataSource.class);
        DataSource decoratedDataSource = ((DecoratedDataSource) dataSource).getDecoratedDataSource();
        assertThat(decoratedDataSource).isInstanceOf(FlexyPoolDataSource.class);
        assertThat(decoratedDataSource).extracting("targetDataSource").isInstanceOf(realDataSourceClass);
        return (FlexyPoolDataSource<T>) decoratedDataSource;
    }

    @SuppressWarnings("unchecked")
    private <T extends ConnectionAcquisitionStrategy> T findStrategy(FlexyPoolDataSource<?> flexyPoolDataSource,
            Class<T> factoryClass) {
        Field field = ReflectionUtils.findField(FlexyPoolDataSource.class, "connectionAcquiringStrategies");
        Objects.requireNonNull(field);
        ReflectionUtils.makeAccessible(field);
        Set<ConnectionAcquisitionStrategy> strategies =
                (Set<ConnectionAcquisitionStrategy>) ReflectionUtils.getField(field, flexyPoolDataSource);
        Objects.requireNonNull(strategies);
        return (T) strategies.stream().filter(factoryClass::isInstance).findFirst().orElse(null);
    }

    @Configuration(proxyBeanMethods = false)
    static class FlexyPoolHikariConfiguration {

        @Bean
        public ConnectionAcquisitionStrategyFactory<?, HikariDataSource> hikariConnectionAcquisitionStrategyForHikari() {
            return configurationProperties -> new HikariConnectionAcquiringFactory();
        }

        @Bean
        public ConnectionAcquisitionStrategyFactory<?, BasicDataSource> dbcp2ConnectionAcquiringFactory() {
            return configurationProperties -> new Dbcp2ConnectionAcquiringFactory();
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class FlexyPoolCustomFactoriesHikariConfiguration {

        @Bean
        public IncrementPoolOnTimeoutConnectionAcquisitionStrategy.Factory<HikariDataSource> incrementPoolOnTimeoutConnectionAcquisitionStrategyFactory() {
            return new IncrementPoolOnTimeoutConnectionAcquisitionStrategy.Factory<>(35, 10000);
        }

        @Bean
        public RetryConnectionAcquisitionStrategy.Factory<HikariDataSource> retryConnectionAcquisitionStrategy() {
            return new RetryConnectionAcquisitionStrategy.Factory<>(5);
        }
    }

    static class HikariConnectionAcquiringFactory implements ConnectionAcquisitionStrategy {

        @Override
        public Connection getConnection(ConnectionRequestContext requestContext) {
            return null;
        }
    }

    static class Dbcp2ConnectionAcquiringFactory implements ConnectionAcquisitionStrategy {

        @Override
        public Connection getConnection(ConnectionRequestContext requestContext) {
            return null;
        }
    }
}
