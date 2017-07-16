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

import com.github.gavlyukovskiy.boot.jdbc.decorator.DataSourceDecoratorProperties;
import com.github.gavlyukovskiy.boot.jdbc.decorator.flexypool.FlexyPoolProperties.AcquiringStrategy.IncrementPool;
import com.github.gavlyukovskiy.boot.jdbc.decorator.flexypool.FlexyPoolProperties.AcquiringStrategy.Retry;
import com.github.gavlyukovskiy.cloud.sleuth.SleuthListenerAutoConfiguration;
import com.vladmihalcea.flexypool.FlexyPoolDataSource;
import com.vladmihalcea.flexypool.adaptor.DBCP2PoolAdapter;
import com.vladmihalcea.flexypool.adaptor.HikariCPPoolAdapter;
import com.vladmihalcea.flexypool.adaptor.TomcatCPPoolAdapter;
import com.vladmihalcea.flexypool.config.PropertyLoader;
import com.vladmihalcea.flexypool.connection.ConnectionProxyFactory;
import com.vladmihalcea.flexypool.event.Event;
import com.vladmihalcea.flexypool.event.EventListener;
import com.vladmihalcea.flexypool.metric.MetricsFactory;
import com.vladmihalcea.flexypool.strategy.ConnectionAcquiringStrategyFactory;
import com.vladmihalcea.flexypool.strategy.IncrementPoolOnTimeoutConnectionAcquiringStrategy;
import com.vladmihalcea.flexypool.strategy.RetryConnectionAcquiringStrategy;
import com.vladmihalcea.flexypool.util.ClassLoaderUtils;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.commons.dbcp2.BasicDataSource;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.type.AnnotatedTypeMetadata;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Configuration for integration with flexy-pool, allows to use define custom {@link ConnectionAcquiringStrategyFactory},
 * {@link MetricsFactory}, {@link ConnectionProxyFactory} and {@link EventListener}.
 *
 * @author Arthur Gavlyukovskiy
 * @since 1.2
 */
public class FlexyPoolConfiguration {

    static <T extends DataSource> List<ConnectionAcquiringStrategyFactory<?, T>> mergeFactories(
            List<ConnectionAcquiringStrategyFactory<?, T>> factories, FlexyPoolProperties flexyPool) {
        List<ConnectionAcquiringStrategyFactory<?, T>> newFactories = new ArrayList<>();
        List<? extends Class<?>> factoryClasses;
        if (factories != null) {
            factoryClasses = factories.stream().map(Object::getClass).collect(Collectors.toList());
            newFactories.addAll(factories);
        }
        else {
            factoryClasses = Collections.emptyList();
        }
        if (!factoryClasses.contains(IncrementPoolOnTimeoutConnectionAcquiringStrategy.Factory.class)) {
            IncrementPool incrementPool = flexyPool.getAcquiringStrategy().getIncrementPool();
            if (incrementPool.getMaxOverflowPoolSize() > 0) {
                newFactories.add(new IncrementPoolOnTimeoutConnectionAcquiringStrategy.Factory<>(
                        incrementPool.getMaxOverflowPoolSize(), incrementPool.getTimeoutMillis()));
            }
        }
        if (!factoryClasses.contains(RetryConnectionAcquiringStrategy.Factory.class)) {
            Retry retry = flexyPool.getAcquiringStrategy().getRetry();
            if (retry.getAttempts() > 0) {
                newFactories.add(new RetryConnectionAcquiringStrategy.Factory<>(retry.getAttempts()));
            }
        }
        return newFactories;
    }

    @ConditionalOnClass(FlexyPoolDataSource.class)
    @Import({
            PropertyFlexyConfiguration.class,
            HikariFlexyConfiguration.class,
            TomcatFlexyConfiguration.class,
            Dbcp2FlexyConfiguration.class,
            FlexyPoolCustomizerConfiguration.class
    })
    public static class Ordered {
    }

    @ConditionalOnMissingBean(PropertyFlexyConfiguration.class)
    static class FlexyPoolCustomizerConfiguration {

        @Autowired
        private DataSourceDecoratorProperties dataSourceDecoratorProperties;
        @Autowired(required = false)
        private MetricsFactory metricsFactory;
        @Autowired(required = false)
        private ConnectionProxyFactory connectionProxyFactory;
        @Autowired(required = false)
        private List<EventListener<? extends Event>> eventListeners;

        @Bean
        public FlexyPoolConfigurationBuilderCustomizer flexyPoolConfigurationBuilderCustomizer() {
            return (beanName, builder, dataSourceClass) -> {
                FlexyPoolProperties flexyPool = dataSourceDecoratorProperties.getFlexyPool();
                builder.setMetricLogReporterMillis(flexyPool.getMetrics().getReporter().getLog().getMillis());
                builder.setJmxEnabled(flexyPool.getMetrics().getReporter().getJmx().isEnabled());
                builder.setJmxAutoStart(flexyPool.getMetrics().getReporter().getJmx().isAutoStart());
                builder.setConnectionAcquireTimeThresholdMillis(flexyPool.getThreshold().getConnection().getAcquire());
                builder.setConnectionLeaseTimeThresholdMillis(flexyPool.getThreshold().getConnection().getLease());
                if (metricsFactory != null) {
                    builder.setMetricsFactory(metricsFactory);
                }
                if (connectionProxyFactory != null) {
                    builder.setConnectionProxyFactory(connectionProxyFactory);
                }
                if (eventListeners != null) {
                    builder.setEventListenerResolver(() -> eventListeners);
                }
            };
        }
    }

    @Configuration
    @ConditionalOnMissingBean(FlexyPoolDataSourceDecorator.class)
    @ConditionalOnClass(HikariCPPoolAdapter.class)
    @ConditionalOnBean(HikariDataSource.class)
    static class HikariFlexyConfiguration {

        @Autowired(required = false)
        private List<ConnectionAcquiringStrategyFactory<?, HikariDataSource>> connectionAcquiringStrategyFactories;
        @Autowired
        private DataSourceDecoratorProperties dataSourceDecoratorProperties;

        @Bean
        public FlexyPoolDataSourceDecorator flexyPoolDataSourceDecorator() {
            return new FlexyPoolDataSourceDecorator(
                    mergeFactories(connectionAcquiringStrategyFactories, dataSourceDecoratorProperties.getFlexyPool()),
                    HikariCPPoolAdapter.FACTORY, HikariDataSource.class);
        }
    }

    @Configuration
    @ConditionalOnMissingBean(FlexyPoolDataSourceDecorator.class)
    @ConditionalOnClass(TomcatCPPoolAdapter.class)
    @ConditionalOnBean(org.apache.tomcat.jdbc.pool.DataSource.class)
    static class TomcatFlexyConfiguration {

        @Autowired(required = false)
        private List<ConnectionAcquiringStrategyFactory<?, org.apache.tomcat.jdbc.pool.DataSource>> connectionAcquiringStrategyFactories;
        @Autowired
        private DataSourceDecoratorProperties dataSourceDecoratorProperties;

        @Bean
        public FlexyPoolDataSourceDecorator flexyPoolDataSourceDecorator() {
            return new FlexyPoolDataSourceDecorator(
                    mergeFactories(connectionAcquiringStrategyFactories, dataSourceDecoratorProperties.getFlexyPool()),
                    TomcatCPPoolAdapter.FACTORY, org.apache.tomcat.jdbc.pool.DataSource.class);
        }
    }

    @Configuration
    @ConditionalOnMissingBean(FlexyPoolDataSourceDecorator.class)
    @ConditionalOnClass(DBCP2PoolAdapter.class)
    @ConditionalOnBean(BasicDataSource.class)
    static class Dbcp2FlexyConfiguration {

        @Autowired(required = false)
        private List<ConnectionAcquiringStrategyFactory<?, BasicDataSource>> connectionAcquiringStrategyFactories;
        @Autowired
        private DataSourceDecoratorProperties dataSourceDecoratorProperties;

        @Bean
        public FlexyPoolDataSourceDecorator flexyPoolDataSourceDecorator() {
            return new FlexyPoolDataSourceDecorator(
                    mergeFactories(connectionAcquiringStrategyFactories, dataSourceDecoratorProperties.getFlexyPool()),
                    DBCP2PoolAdapter.FACTORY, BasicDataSource.class);
        }
    }

    @Configuration
    @ConditionalOnMissingBean(FlexyPoolDataSourceDecorator.class)
    @Conditional(FlexyPoolConfiguration.FlexyPoolConfigurationAvailableCondition.class)
    static class PropertyFlexyConfiguration {

        private static final Logger log = getLogger(PropertyFlexyConfiguration.class);

        @Autowired(required = false)
        private List<ConnectionAcquiringStrategyFactory<?, javax.sql.DataSource>> connectionAcquiringStrategyFactories;

        @PostConstruct
        public void warnIfAnyStrategyFound() {
            if (connectionAcquiringStrategyFactories != null) {
                log.warn("ConnectionAcquiringStrategyFactory beans found in the context will not be applied to " +
                        "FlexyDataSource due to property based configuration of FlexyPool");
            }
        }

        @Bean
        public FlexyPoolDataSourceDecorator flexyPoolDataSourceDecorator() {
            return new FlexyPoolDataSourceDecorator();
        }
    }

    private static class FlexyPoolConfigurationAvailableCondition extends SpringBootCondition {

        @Override
        public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
            ConditionMessage.Builder message = ConditionMessage.forCondition("FlexyPoolConfigurationAvailable");
            String propertiesFilePath = System.getProperty(PropertyLoader.PROPERTIES_FILE_PATH);
            if (propertiesFilePath != null && ClassLoaderUtils.getResource(propertiesFilePath) != null) {
                return ConditionOutcome.match(message.found("FlexyPool configuration file").items(propertiesFilePath));
            }
            if (ClassLoaderUtils.getResource(PropertyLoader.PROPERTIES_FILE_NAME) != null) {
                return ConditionOutcome.match(message.found("FlexyPool configuration file").items(PropertyLoader.PROPERTIES_FILE_NAME));
            }
            return ConditionOutcome.noMatch(message.didNotFind("FlexyPool configuration file").atAll());
        }
    }
}
