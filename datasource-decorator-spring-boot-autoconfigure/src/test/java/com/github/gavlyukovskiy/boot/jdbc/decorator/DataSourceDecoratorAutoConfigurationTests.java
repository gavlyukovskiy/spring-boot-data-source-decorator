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

import com.p6spy.engine.spy.P6DataSource;
import com.vladmihalcea.flexypool.FlexyPoolDataSource;
import com.zaxxer.hikari.HikariDataSource;
import net.ttddyy.dsproxy.support.ProxyDataSource;
import org.apache.commons.dbcp2.BasicDataSource;
import org.assertj.core.api.AbstractListAssert;
import org.assertj.core.api.ObjectAssert;
import org.junit.jupiter.api.Test;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;

class DataSourceDecoratorAutoConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    DataSourceAutoConfiguration.class,
                    DataSourceDecoratorAutoConfiguration.class,
                    PropertyPlaceholderAutoConfiguration.class
            ))
            .withPropertyValues("spring.sql.init.mode=never",
                    "spring.datasource.url:jdbc:h2:mem:testdb-" + ThreadLocalRandom.current().nextInt());

    @Test
    void testDecoratingInDefaultOrder() {
        contextRunner.run(context -> {
            DataSource dataSource = context.getBean(DataSource.class);

            assertThatDataSourceDecoratingChain(dataSource).containsExactly(P6DataSource.class, ProxyDataSource.class, FlexyPoolDataSource.class);
        });
    }

    @Test
    void testNoDecoratingForExcludeBeans() {
        ApplicationContextRunner contextRunner = this.contextRunner.withPropertyValues("decorator.datasource.exclude-beans:dataSource");

        contextRunner.run(context -> {
            DataSource dataSource = context.getBean(DataSource.class);

            assertThat(dataSource).isInstanceOf(HikariDataSource.class);
        });
    }

    @Test
    void testDecoratingWhenDefaultProxyProviderNotAvailable() {
        ApplicationContextRunner contextRunner = this.contextRunner.withClassLoader(new HidePackagesClassLoader("com.vladmihalcea.flexypool"));

        contextRunner.run(context -> {
            DataSource dataSource = context.getBean(DataSource.class);

            assertThat(((DecoratedDataSource) dataSource).getRealDataSource()).isInstanceOf(HikariDataSource.class);
            assertThatDataSourceDecoratingChain(dataSource).containsExactly(P6DataSource.class, ProxyDataSource.class);
        });
    }

    @Test
    void testDecoratedHikariSpecificPropertiesIsSet() {
        ApplicationContextRunner contextRunner = this.contextRunner.withPropertyValues(
                "spring.datasource.type:" + HikariDataSource.class.getName(),
                "spring.datasource.hikari.catalog:test_catalog"
        );

        contextRunner.run(context -> {
            DataSource dataSource = context.getBean(DataSource.class);
            assertThat(dataSource).isNotNull();
            assertThat(dataSource).isInstanceOf(DecoratedDataSource.class);
            DataSource realDataSource = ((DecoratedDataSource) dataSource).getRealDataSource();
            assertThat(realDataSource).isInstanceOf(HikariDataSource.class);
            assertThat(((HikariDataSource) realDataSource).getCatalog()).isEqualTo("test_catalog");
        });
    }

    @Test
    void testCustomDataSourceIsDecorated() {
        ApplicationContextRunner contextRunner = this.contextRunner.withUserConfiguration(TestDataSourceConfiguration.class);

        contextRunner.run(context -> {
            DataSource dataSource = context.getBean(DataSource.class);
            assertThat(dataSource).isInstanceOf(DecoratedDataSource.class);
            DataSource realDataSource = ((DecoratedDataSource) dataSource).getRealDataSource();
            assertThat(realDataSource).isInstanceOf(BasicDataSource.class);
        });
    }

    @Test
    void testScopedDataSourceIsNotDecorated() {
        ApplicationContextRunner contextRunner = this.contextRunner.withUserConfiguration(TestScopedDataSourceConfiguration.class);

        contextRunner.run(context -> {
            assertThat(context).getBeanNames(DataSource.class).containsOnly("dataSource", "scopedTarget.dataSource");
            assertThat(context).getBean("dataSource").isInstanceOf(DecoratedDataSource.class);
            assertThat(context).getBean("scopedTarget.dataSource").isNotInstanceOf(DecoratedDataSource.class);
        });
    }

    @Test
    void testCustomDataSourceDecoratorApplied() {
        ApplicationContextRunner contextRunner = this.contextRunner.withUserConfiguration(TestDataSourceDecoratorConfiguration.class);

        contextRunner.run(context -> {
            DataSource dataSource = context.getBean(DataSource.class);
            assertThat(dataSource).isNotNull();

            DataSource customDataSource = ((DecoratedDataSource) dataSource).getDecoratedDataSource();
            assertThat(customDataSource).isInstanceOf(CustomDataSourceProxy.class);

            DataSource realDataSource = ((DecoratedDataSource) dataSource).getRealDataSource();
            assertThat(realDataSource).isInstanceOf(HikariDataSource.class);

            assertThatDataSourceDecoratingChain(dataSource).containsExactly(CustomDataSourceProxy.class, P6DataSource.class, ProxyDataSource.class,
                    FlexyPoolDataSource.class);
        });
    }

    @Test
    void testDecoratingCanBeDisabled() {
        ApplicationContextRunner contextRunner = this.contextRunner.withPropertyValues("decorator.datasource.enabled:false");

        contextRunner.run(context -> {
            DataSource dataSource = context.getBean(DataSource.class);
            assertThat(dataSource).isInstanceOf(HikariDataSource.class);
        });
    }

    @Test
    void testDecoratingCanBeDisabledForSpecificBeans() {
        ApplicationContextRunner contextRunner = this.contextRunner.withPropertyValues("decorator.datasource.exclude-beans:secondDataSource")
                .withUserConfiguration(TestMultiDataSourceConfiguration.class);

        contextRunner.run(context -> {
            DataSource dataSource = context.getBean("dataSource", DataSource.class);
            assertThat(dataSource).isInstanceOf(DecoratedDataSource.class);

            DataSource secondDataSource = context.getBean("secondDataSource", DataSource.class);
            assertThat(secondDataSource).isInstanceOf(BasicDataSource.class);
        });
    }

    @Test
    void testDecoratingChainBuiltCorrectly() {
        contextRunner.run(context -> {
            DataSource dataSource = context.getBean(DataSource.class);

            DecoratedDataSource dataSource1 = context.getBean(DecoratedDataSource.class);
            assertThat(dataSource1).isNotNull();

            DataSource p6DataSource = dataSource1.getDecoratedDataSource();
            assertThat(p6DataSource).isNotNull();
            assertThat(p6DataSource).isInstanceOf(P6DataSource.class);

            DataSource proxyDataSource = (DataSource) new DirectFieldAccessor(p6DataSource)
                    .getPropertyValue("realDataSource");
            assertThat(proxyDataSource).isNotNull();
            assertThat(proxyDataSource).isInstanceOf(ProxyDataSource.class);

            DataSource flexyDataSource = (DataSource) new DirectFieldAccessor(proxyDataSource)
                    .getPropertyValue("dataSource");
            assertThat(flexyDataSource).isNotNull();
            assertThat(flexyDataSource).isInstanceOf(FlexyPoolDataSource.class);

            DataSource realDataSource = (DataSource) new DirectFieldAccessor(flexyDataSource)
                    .getPropertyValue("targetDataSource");
            assertThat(realDataSource).isNotNull();
            assertThat(realDataSource).isInstanceOf(HikariDataSource.class);

            assertThatDataSourceDecoratingChain(dataSource).containsExactly(P6DataSource.class, ProxyDataSource.class, FlexyPoolDataSource.class);

            assertThat(dataSource.toString()).isEqualTo(
                    "p6SpyDataSourceDecorator [com.p6spy.engine.spy.P6DataSource] -> " +
                            "proxyDataSourceDecorator [net.ttddyy.dsproxy.support.ProxyDataSource] -> " +
                            "flexyPoolDataSourceDecorator [com.vladmihalcea.flexypool.FlexyPoolDataSource] -> " +
                            "dataSource [com.zaxxer.hikari.HikariDataSource]");
        });
    }

    @Test
    void testDecorateDynamicallyRegisteredBeans() {
        ApplicationContextRunner contextRunner = this.contextRunner.withInitializer(context -> {
            GenericApplicationContext gac = (GenericApplicationContext) context;
            gac.registerBean("ds1", DataSource.class, () -> new HikariDataSource());
            gac.registerBean("ds2", DataSource.class, () -> new HikariDataSource());
        });

        contextRunner.run(context -> {
            DataSource dataSource1 = context.getBean("ds1", DataSource.class);
            assertThat(dataSource1).isNotNull();
            assertThat(dataSource1).isInstanceOf(DecoratedDataSource.class);

            DataSource dataSource2 = context.getBean("ds2", DataSource.class);
            assertThat(dataSource2).isNotNull();
            assertThat(dataSource2).isInstanceOf(DecoratedDataSource.class);
        });
    }
    
    @Test
    void testRoutingDataSourceIsDecorated() {
        ApplicationContextRunner contextRunner = this.contextRunner.withUserConfiguration(TestAbstractRoutingDataSourceConfiguration.class);

        contextRunner.run(context -> {
            DataSource dataSource = context.getBean(DataSource.class);
            assertThat(dataSource).isInstanceOf(DecoratedDataSource.class);
            DataSource realDataSource = ((DecoratedDataSource) dataSource).getRealDataSource();
            assertThat(realDataSource).isInstanceOf(AbstractRoutingDataSource.class);
        });
    }
    
    @Test
    void testRoutingDataSourceIsNotDecorated() {
        ApplicationContextRunner contextRunner = this.contextRunner.withPropertyValues("decorator.datasource.ignore-routing-data-sources=true")
                .withUserConfiguration(TestAbstractRoutingDataSourceConfiguration.class);

        contextRunner.run(context -> {
            DataSource dataSource = context.getBean(DataSource.class);
            assertThat(dataSource).isNotInstanceOf(DecoratedDataSource.class);
            assertThat(dataSource).isInstanceOf(AbstractRoutingDataSource.class);
        });
    }

    private AbstractListAssert<?, List<?>, Object, ObjectAssert<Object>> assertThatDataSourceDecoratingChain(DataSource dataSource) {
        return assertThat(((DecoratedDataSource) dataSource).getDecoratingChain()).extracting("dataSource").extracting("class");
    }

    @Configuration(proxyBeanMethods = false)
    static class TestDataSourceConfiguration {

        @Bean
        public DataSource dataSource() {
            BasicDataSource pool = new BasicDataSource();
            pool.setDriverClassName("org.hsqldb.jdbcDriver");
            pool.setUrl("jdbc:hsqldb:target/overridedb");
            pool.setUsername("sa");
            return pool;
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class TestDataSourceDecoratorConfiguration {

        @Bean
        public DataSourceDecorator customDataSourceDecorator() {
            return (beanName, dataSource) -> new CustomDataSourceProxy(dataSource);
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class TestMultiDataSourceConfiguration {

        @Bean
        @Primary
        public DataSource dataSource() {
            BasicDataSource pool = new BasicDataSource();
            pool.setDriverClassName("org.hsqldb.jdbcDriver");
            pool.setUrl("jdbc:hsqldb:target/db");
            pool.setUsername("sa");
            return pool;
        }

        @Bean
        public DataSource secondDataSource() {
            BasicDataSource pool = new BasicDataSource();
            pool.setDriverClassName("org.hsqldb.jdbcDriver");
            pool.setUrl("jdbc:hsqldb:target/db2");
            pool.setUsername("sa");
            return pool;
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class TestScopedDataSourceConfiguration {

        @Bean
        @Scope(value = "prototype", proxyMode = ScopedProxyMode.TARGET_CLASS)
        public DataSource dataSource() {
            BasicDataSource pool = new BasicDataSource();
            pool.setDriverClassName("org.hsqldb.jdbcDriver");
            pool.setUrl("jdbc:hsqldb:target/overridedb");
            pool.setUsername("sa");
            return pool;
        }
    }
    
    @Configuration(proxyBeanMethods = false)
    static class TestAbstractRoutingDataSourceConfiguration {

        @Bean
        public DataSource dataSource() {
            AbstractRoutingDataSource routingDs = new AbstractRoutingDataSource() {
              @Override
              protected Object determineCurrentLookupKey() {
                return "ds1";
              }
            };
            BasicDataSource pool = new BasicDataSource();
            pool.setDriverClassName("org.hsqldb.jdbcDriver");
            pool.setUrl("jdbc:hsqldb:target/routingds");
            pool.setUsername("sa");
            Map<Object, Object> targetDataSources = new HashMap<>();
            targetDataSources.put("ds1", pool);
            routingDs.setTargetDataSources(targetDataSources);
            routingDs.setDefaultTargetDataSource(pool);
            return routingDs;
        }
    }

    /**
     * Custom proxy data source for tests.
     *
     * @author Arthur Gavlyukovskiy
     */
    static class CustomDataSourceProxy implements DataSource {

        private DataSource delegate;

        CustomDataSourceProxy(DataSource delegate) {
            this.delegate = delegate;
        }

        @Override
        public Connection getConnection() {
            return null;
        }

        @Override
        public Connection getConnection(String username, String password) {
            return null;
        }

        @Override
        public <T> T unwrap(Class<T> iface) {
            return null;
        }

        @Override
        public boolean isWrapperFor(Class<?> iface) {
            return false;
        }

        @Override
        public PrintWriter getLogWriter() {
            return null;
        }

        @Override
        public void setLogWriter(PrintWriter out) {

        }

        @Override
        public void setLoginTimeout(int seconds) {

        }

        @Override
        public int getLoginTimeout() {
            return 0;
        }

        @Override
        public Logger getParentLogger() {
            return null;
        }
    }
}
