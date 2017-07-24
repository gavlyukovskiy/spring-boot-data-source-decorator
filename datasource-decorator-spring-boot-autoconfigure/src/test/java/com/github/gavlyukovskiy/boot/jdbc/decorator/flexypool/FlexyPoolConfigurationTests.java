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
import com.vladmihalcea.flexypool.strategy.ConnectionAcquiringStrategy;
import com.vladmihalcea.flexypool.strategy.ConnectionAcquiringStrategyFactory;
import com.vladmihalcea.flexypool.strategy.IncrementPoolOnTimeoutConnectionAcquiringStrategy;
import com.vladmihalcea.flexypool.strategy.RetryConnectionAcquiringStrategy;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.commons.dbcp2.BasicDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ReflectionUtils;

import javax.sql.DataSource;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Random;
import java.util.Set;

import static org.assertj.core.api.Java6Assertions.assertThat;

public class FlexyPoolConfigurationTests {

    private final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

    @Before
    public void init() {
        EnvironmentTestUtils.addEnvironment(context,
                "datasource.initialize:false",
                "datasource.url:jdbc:h2:mem:testdb-" + new Random().nextInt());
        context.setClassLoader(new HidePackagesClassLoader("net.ttddyy.dsproxy", "com.p6spy"));
    }

    @After
    public void restore() {
        context.close();
    }

    @Test
    public void testDecoratingTomcatDataSource() throws Exception {
        context.register(DataSourceAutoConfiguration.class,
                DataSourceDecoratorAutoConfiguration.class,
                PropertyPlaceholderAutoConfiguration.class);
        context.refresh();

        DataSource dataSource = context.getBean(DataSource.class);
        assertDataSourceOfType(dataSource, org.apache.tomcat.jdbc.pool.DataSource.class);
    }

    @Test
    public void testNoDecoratingTomcatDataSourceWithoutAdapterDependency() throws Exception {
        context.setClassLoader(new HidePackagesClassLoader("net.ttddyy.dsproxy", "com.p6spy", "com.vladmihalcea.flexypool.adaptor.TomcatCPPoolAdapter"));
        context.register(DataSourceAutoConfiguration.class,
                DataSourceDecoratorAutoConfiguration.class,
                PropertyPlaceholderAutoConfiguration.class);
        context.refresh();

        DataSource dataSource = context.getBean(DataSource.class);
        assertThat(dataSource).isInstanceOf(org.apache.tomcat.jdbc.pool.DataSource.class);
    }

    @Test
    public void testDecoratingDbcp2DataSource() throws Exception {
        EnvironmentTestUtils.addEnvironment(context,
                "datasource.type:" + BasicDataSource.class.getName());
        context.register(DataSourceAutoConfiguration.class,
                DataSourceDecoratorAutoConfiguration.class,
                PropertyPlaceholderAutoConfiguration.class);
        context.refresh();

        DataSource dataSource = context.getBean(DataSource.class);
        assertDataSourceOfType(dataSource, BasicDataSource.class);
    }

    @Test
    public void testDecoratingHikariDataSourceWithDefaultStrategies() throws Exception {
        EnvironmentTestUtils.addEnvironment(context,
                "datasource.type:" + HikariDataSource.class.getName());
        context.register(DataSourceAutoConfiguration.class,
                DataSourceDecoratorAutoConfiguration.class,
                PropertyPlaceholderAutoConfiguration.class);
        context.refresh();

        DataSource dataSource = context.getBean(DataSource.class);
        assertDataSourceOfType(dataSource, HikariDataSource.class);
        FlexyPoolDataSource<HikariDataSource> flexyPoolDataSource = assertDataSourceOfType(dataSource, HikariDataSource.class);
        IncrementPoolOnTimeoutConnectionAcquiringStrategy strategy1 =
                findStrategy(flexyPoolDataSource, IncrementPoolOnTimeoutConnectionAcquiringStrategy.class);
        assertThat(strategy1).isNotNull();
        assertThat(strategy1).hasFieldOrPropertyWithValue("maxOverflowPoolSize", 15);
        assertThat(strategy1).hasFieldOrPropertyWithValue("timeoutMillis", 500);

        RetryConnectionAcquiringStrategy strategy2 =
                findStrategy(flexyPoolDataSource, RetryConnectionAcquiringStrategy.class);
        assertThat(strategy2).isNotNull();
        assertThat(strategy2).hasFieldOrPropertyWithValue("retryAttempts", 2);
    }

    @Test
    public void testDecoratingHikariDataSourceWithCustomPropertyStrategies() throws Exception {
        EnvironmentTestUtils.addEnvironment(context,
                "datasource.type:" + HikariDataSource.class.getName(),
                "datasource.decorator.flexy-pool.acquiring-strategy.increment-pool.max-overflow-pool-size:15",
                "datasource.decorator.flexy-pool.acquiring-strategy.increment-pool.timeout-millis:500",
                "datasource.decorator.flexy-pool.acquiring-strategy.retry.attempts:5");
        context.register(DataSourceAutoConfiguration.class,
                DataSourceDecoratorAutoConfiguration.class,
                PropertyPlaceholderAutoConfiguration.class,
                HikariConfiguration.class);
        context.refresh();

        DataSource dataSource = context.getBean(DataSource.class);
        FlexyPoolDataSource<HikariDataSource> flexyPoolDataSource = assertDataSourceOfType(dataSource, HikariDataSource.class);
        IncrementPoolOnTimeoutConnectionAcquiringStrategy strategy1 =
                findStrategy(flexyPoolDataSource, IncrementPoolOnTimeoutConnectionAcquiringStrategy.class);
        assertThat(strategy1).isNotNull();
        assertThat(strategy1).hasFieldOrPropertyWithValue("maxOverflowPoolSize", 35);
        assertThat(strategy1).hasFieldOrPropertyWithValue("timeoutMillis", 10000);

        RetryConnectionAcquiringStrategy strategy2 =
                findStrategy(flexyPoolDataSource, RetryConnectionAcquiringStrategy.class);
        assertThat(strategy2).isNotNull();
        assertThat(strategy2).hasFieldOrPropertyWithValue("retryAttempts", 5);
    }

    @Test
    public void testDecoratingHikariDataSourceWithCustomBeanStrategies() throws Exception {
        EnvironmentTestUtils.addEnvironment(context,
                "datasource.type:" + HikariDataSource.class.getName());
        context.register(DataSourceAutoConfiguration.class,
                DataSourceDecoratorAutoConfiguration.class,
                PropertyPlaceholderAutoConfiguration.class,
                HikariConfiguration.class);
        context.refresh();

        DataSource dataSource = context.getBean(DataSource.class);
        FlexyPoolDataSource<HikariDataSource> flexyPoolDataSource = assertDataSourceOfType(dataSource, HikariDataSource.class);
        IncrementPoolOnTimeoutConnectionAcquiringStrategy strategy1 =
                findStrategy(flexyPoolDataSource, IncrementPoolOnTimeoutConnectionAcquiringStrategy.class);
        assertThat(strategy1).isNotNull();
        assertThat(strategy1).hasFieldOrPropertyWithValue("maxOverflowPoolSize", 35);
        assertThat(strategy1).hasFieldOrPropertyWithValue("timeoutMillis", 10000);

        RetryConnectionAcquiringStrategy strategy2 =
                findStrategy(flexyPoolDataSource, RetryConnectionAcquiringStrategy.class);
        assertThat(strategy2).isNotNull();
        assertThat(strategy2).hasFieldOrPropertyWithValue("retryAttempts", 5);

        HikariConnectionAcquiringFactory strategy3 =
                findStrategy(flexyPoolDataSource, HikariConnectionAcquiringFactory.class);
        assertThat(strategy3).isNotNull();

        Dbcp2ConnectionAcquiringFactory strategy4 =
                findStrategy(flexyPoolDataSource, Dbcp2ConnectionAcquiringFactory.class);
        assertThat(strategy4).isNull();
    }

    @SuppressWarnings("unchecked")
    private <T extends DataSource> FlexyPoolDataSource<T> assertDataSourceOfType(DataSource dataSource, Class<T> realDataSourceClass) {
        assertThat(dataSource).isInstanceOf(DecoratedDataSource.class);
        DataSource decoratedDataSource = ((DecoratedDataSource) dataSource).getDecoratedDataSource();
        assertThat(decoratedDataSource).isInstanceOf(FlexyPoolDataSource.class);
        Field field = ReflectionUtils.findField(FlexyPoolDataSource.class, "targetDataSource");
        ReflectionUtils.makeAccessible(field);
        Object targetDataSource = ReflectionUtils.getField(field, decoratedDataSource);
        assertThat(targetDataSource).isInstanceOf(realDataSourceClass);
        return (FlexyPoolDataSource<T>) decoratedDataSource;
    }

    @SuppressWarnings("unchecked")
    private <T extends ConnectionAcquiringStrategy> T findStrategy(FlexyPoolDataSource<?> flexyPoolDataSource,
            Class<T> factoryClass) {
        Field field = ReflectionUtils.findField(FlexyPoolDataSource.class, "connectionAcquiringStrategies");
        ReflectionUtils.makeAccessible(field);
        Set<ConnectionAcquiringStrategy> strategies =
                (Set<ConnectionAcquiringStrategy>) ReflectionUtils.getField(field, flexyPoolDataSource);
        return (T) strategies.stream().filter(factoryClass::isInstance).findFirst().orElse(null);
    }

    @Configuration
    static class HikariConfiguration {

        @Bean
        public IncrementPoolOnTimeoutConnectionAcquiringStrategy.Factory<HikariDataSource> incrementPoolOnTimeoutConnectionAcquiringStrategyFactory() {
            return new IncrementPoolOnTimeoutConnectionAcquiringStrategy.Factory<>(35, 10000);
        }

        @Bean
        public RetryConnectionAcquiringStrategy.Factory<HikariDataSource> retryConnectionAcquiringStrategy() {
            return new RetryConnectionAcquiringStrategy.Factory<>(5);
        }

        @Bean
        public ConnectionAcquiringStrategyFactory<?, HikariDataSource> hikariConnectionAcquiringStrategyForHikari() {
            return configurationProperties -> new HikariConnectionAcquiringFactory();
        }

        @Bean
        public ConnectionAcquiringStrategyFactory<?, BasicDataSource> dbcp2ConnectionAcquiringFactory() {
            return configurationProperties -> new Dbcp2ConnectionAcquiringFactory();
        }
    }

    static class HikariConnectionAcquiringFactory implements ConnectionAcquiringStrategy {

        @Override
        public Connection getConnection(ConnectionRequestContext requestContext) throws SQLException {
            return null;
        }
    }

    static class Dbcp2ConnectionAcquiringFactory implements ConnectionAcquiringStrategy {

        @Override
        public Connection getConnection(ConnectionRequestContext requestContext) throws SQLException {
            return null;
        }
    }
}
