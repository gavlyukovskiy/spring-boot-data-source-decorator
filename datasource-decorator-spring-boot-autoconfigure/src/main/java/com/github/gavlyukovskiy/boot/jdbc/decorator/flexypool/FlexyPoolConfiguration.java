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

import com.github.gavlyukovskiy.boot.jdbc.decorator.flexypool.FlexyPoolConfiguration.Dbcp2FlexyConfiguration;
import com.github.gavlyukovskiy.boot.jdbc.decorator.flexypool.FlexyPoolConfiguration.PropertyFlexyConfiguration;
import com.github.gavlyukovskiy.boot.jdbc.decorator.flexypool.FlexyPoolConfiguration.HikariFlexyConfiguration;
import com.github.gavlyukovskiy.boot.jdbc.decorator.flexypool.FlexyPoolConfiguration.TomcatFlexyConfiguration;
import com.vladmihalcea.flexypool.FlexyPoolDataSource;
import com.vladmihalcea.flexypool.adaptor.DBCP2PoolAdapter;
import com.vladmihalcea.flexypool.adaptor.HikariCPPoolAdapter;
import com.vladmihalcea.flexypool.adaptor.TomcatCPPoolAdapter;
import com.vladmihalcea.flexypool.config.PropertyLoader;
import com.vladmihalcea.flexypool.strategy.ConnectionAcquiringStrategyFactory;
import com.vladmihalcea.flexypool.util.ClassLoaderUtils;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.commons.dbcp2.BasicDataSource;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import static org.slf4j.LoggerFactory.getLogger;

@ConditionalOnClass(FlexyPoolDataSource.class)
@Import({ PropertyFlexyConfiguration.class, HikariFlexyConfiguration.class, TomcatFlexyConfiguration.class, Dbcp2FlexyConfiguration.class })
public class FlexyPoolConfiguration {

    @Configuration
    @ConditionalOnMissingBean(FlexyPoolDataSourceDecorator.class)
    @ConditionalOnClass(HikariCPPoolAdapter.class)
    @ConditionalOnBean(HikariDataSource.class)
    static class HikariFlexyConfiguration {

        @Autowired(required = false)
        private List<ConnectionAcquiringStrategyFactory<?, HikariDataSource>> connectionAcquiringStrategyFactories;

        @Bean
        public FlexyPoolDataSourceDecorator<HikariDataSource> flexyPoolDataSourceDecorator() {
            return new FlexyPoolDataSourceDecorator<>(connectionAcquiringStrategyFactories, HikariCPPoolAdapter.FACTORY, HikariDataSource.class);
        }
    }

    @Configuration
    @ConditionalOnMissingBean(FlexyPoolDataSourceDecorator.class)
    @ConditionalOnClass(TomcatCPPoolAdapter.class)
    @ConditionalOnBean(org.apache.tomcat.jdbc.pool.DataSource.class)
    static class TomcatFlexyConfiguration {

        @Autowired(required = false)
        private List<ConnectionAcquiringStrategyFactory<?, org.apache.tomcat.jdbc.pool.DataSource>> connectionAcquiringStrategyFactories;

        @Bean
        public FlexyPoolDataSourceDecorator<org.apache.tomcat.jdbc.pool.DataSource> flexyPoolDataSourceDecorator() {
            return new FlexyPoolDataSourceDecorator<>(connectionAcquiringStrategyFactories, TomcatCPPoolAdapter.FACTORY, org.apache.tomcat.jdbc.pool.DataSource.class);
        }
    }

    @Configuration
    @ConditionalOnMissingBean(FlexyPoolDataSourceDecorator.class)
    @ConditionalOnClass(DBCP2PoolAdapter.class)
    @ConditionalOnBean(BasicDataSource.class)
    static class Dbcp2FlexyConfiguration {

        @Autowired(required = false)
        private List<ConnectionAcquiringStrategyFactory<?, BasicDataSource>> connectionAcquiringStrategyFactories;

        @Bean
        public FlexyPoolDataSourceDecorator<BasicDataSource> flexyPoolDataSourceDecorator() {
            return new FlexyPoolDataSourceDecorator<>(connectionAcquiringStrategyFactories, DBCP2PoolAdapter.FACTORY, BasicDataSource.class);
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
        public FlexyPoolDataSourceDecorator<javax.sql.DataSource> flexyPoolDataSourceDecorator() {
            return new FlexyPoolDataSourceDecorator<>();
        }
    }

    private static class FlexyPoolConfigurationAvailableCondition extends SpringBootCondition {

        @Override
        public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
            ConditionMessage.Builder message = ConditionMessage.forCondition("FlexyPoolConfigurationAvailable");
            String propertiesFilePath = System.getProperty(PropertyLoader.PROPERTIES_FILE_PATH);
            if (propertiesFilePath != null) {
                URL propertiesFileUrl;
                try {
                    propertiesFileUrl = new URL(propertiesFilePath);
                } catch (MalformedURLException ignored) {
                    propertiesFileUrl = ClassLoaderUtils.getResource(propertiesFilePath);
                    if (propertiesFileUrl == null) {
                        File f = new File(propertiesFilePath);
                        if (f.exists() && f.isFile()) {
                            try {
                                propertiesFileUrl = f.toURI().toURL();
                            } catch (MalformedURLException ignored2) {
                            }
                        }
                    }
                }
                if (propertiesFileUrl != null) {
                    return ConditionOutcome.match(message.found("FlexyPool configuration file").items(propertiesFilePath));
                }
            }
            if (ClassLoaderUtils.getResource(PropertyLoader.PROPERTIES_FILE_NAME) != null) {
                return ConditionOutcome.match(message.found("FlexyPool configuration file").items(PropertyLoader.PROPERTIES_FILE_NAME));
            }
            return ConditionOutcome.noMatch(message.didNotFind("FlexyPool configuration file").atAll());
        }
    }
}
