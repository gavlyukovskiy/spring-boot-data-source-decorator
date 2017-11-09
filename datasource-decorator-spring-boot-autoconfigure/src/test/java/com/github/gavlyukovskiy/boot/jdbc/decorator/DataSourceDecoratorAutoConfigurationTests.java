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
import com.vladmihalcea.flexypool.config.PropertyLoader;
import com.vladmihalcea.flexypool.metric.MetricsFactory;
import com.vladmihalcea.flexypool.metric.MetricsFactoryService;
import com.zaxxer.hikari.HikariDataSource;
import net.ttddyy.dsproxy.support.ProxyDataSource;
import org.apache.commons.dbcp2.BasicDataSource;
import org.assertj.core.api.AbstractListAssert;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.ObjectAssert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.metadata.CommonsDbcp2DataSourcePoolMetadata;
import org.springframework.boot.autoconfigure.jdbc.metadata.DataSourcePoolMetadata;
import org.springframework.boot.autoconfigure.jdbc.metadata.DataSourcePoolMetadataProvider;
import org.springframework.boot.autoconfigure.jdbc.metadata.DataSourcePoolMetadataProviders;
import org.springframework.boot.autoconfigure.jdbc.metadata.HikariDataSourcePoolMetadata;
import org.springframework.boot.autoconfigure.jdbc.metadata.TomcatDataSourcePoolMetadata;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_MOCKS;
import static org.mockito.Mockito.mock;

public class DataSourceDecoratorAutoConfigurationTests {

    private final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

    @Before
    public void init() {
        EnvironmentTestUtils.addEnvironment(context,
                "datasource.initialize:false",
                "datasource.url:jdbc:h2:mem:testdb-" + new Random().nextInt());
    }

    @After
    public void restore() {
        System.clearProperty(PropertyLoader.PROPERTIES_FILE_PATH);
        context.close();
    }

    @Test
    public void testDecoratingInDefaultOrder() throws Exception {
        context.register(DataSourceAutoConfiguration.class,
                DataSourceDecoratorAutoConfiguration.class,
                PropertyPlaceholderAutoConfiguration.class);
        context.refresh();

        DataSource dataSource = context.getBean(DataSource.class);

        assertThatDataSourceDecoratingChain(dataSource).containsExactly(P6DataSource.class, ProxyDataSource.class, FlexyPoolDataSource.class);
    }

    @Test
    public void testNoDecoratingForExcludeBeans() throws Exception {
        EnvironmentTestUtils.addEnvironment(context,
                "decorator.datasource.exclude-beans:dataSource");
        context.register(DataSourceAutoConfiguration.class,
                DataSourceDecoratorAutoConfiguration.class,
                PropertyPlaceholderAutoConfiguration.class);
        context.refresh();

        DataSource dataSource = context.getBean(DataSource.class);

        assertThat(dataSource).isInstanceOf(org.apache.tomcat.jdbc.pool.DataSource.class);
    }

    @Test
    public void testDecoratingWhenDefaultProxyProviderNotAvailable() throws Exception {
        context.register(DataSourceAutoConfiguration.class,
                DataSourceDecoratorAutoConfiguration.class,
                PropertyPlaceholderAutoConfiguration.class);
        context.setClassLoader(new HidePackagesClassLoader("com.vladmihalcea.flexypool"));
        context.refresh();
        DataSource dataSource = context.getBean(DataSource.class);

        assertThat(((DecoratedDataSource) dataSource).getRealDataSource()).isInstanceOf(org.apache.tomcat.jdbc.pool.DataSource.class);
        assertThatDataSourceDecoratingChain(dataSource).containsExactly(P6DataSource.class, ProxyDataSource.class);
    }

    @Test
    public void testDecoratedHikariSpecificPropertiesIsSet() throws Exception {
        EnvironmentTestUtils.addEnvironment(context,
                "spring.datasource.type:" + HikariDataSource.class.getName(),
                "spring.datasource.hikari.catalog:test_catalog");
        context.register(DataSourceAutoConfiguration.class,
                DataSourceDecoratorAutoConfiguration.class,
                PropertyPlaceholderAutoConfiguration.class);
        context.refresh();
        DataSource dataSource = context.getBean(DataSource.class);
        assertThat(dataSource).isNotNull();
        assertThat(dataSource).isInstanceOf(DecoratedDataSource.class);
        DataSource realDataSource = ((DecoratedDataSource) dataSource).getRealDataSource();
        assertThat(realDataSource).isInstanceOf(HikariDataSource.class);
        assertThat(((HikariDataSource) realDataSource).getCatalog()).isEqualTo("test_catalog");
    }

    @Test
    public void testDefaultDataSourceIsDecorated() throws Exception {
        context.register(TestDataSourceConfiguration.class,
                DataSourceAutoConfiguration.class,
                DataSourceDecoratorAutoConfiguration.class,
                PropertyPlaceholderAutoConfiguration.class);
        context.refresh();
        DataSource dataSource = context.getBean(DataSource.class);
        assertThat(dataSource).isInstanceOf(DecoratedDataSource.class);
        DataSource realDataSource = ((DecoratedDataSource) dataSource).getRealDataSource();
        assertThat(realDataSource).isInstanceOf(BasicDataSource.class);
    }

    @Test
    public void testCustomDataSourceDecoratorApplied() throws Exception {
        System.setProperty(PropertyLoader.PROPERTIES_FILE_PATH, "db/decorator/flexy-pool.properties");
        context.register(TestDataSourceDecoratorConfiguration.class,
                DataSourceAutoConfiguration.class,
                DataSourceDecoratorAutoConfiguration.class,
                PropertyPlaceholderAutoConfiguration.class);
        context.refresh();

        DataSource dataSource = context.getBean(DataSource.class);
        assertThat(dataSource).isNotNull();

        DataSource customDataSource = ((DecoratedDataSource) dataSource).getDecoratedDataSource();
        assertThat(customDataSource).isInstanceOf(CustomDataSourceProxy.class);

        DataSource realDataSource = ((DecoratedDataSource) dataSource).getRealDataSource();
        assertThat(realDataSource).isInstanceOf(org.apache.tomcat.jdbc.pool.DataSource.class);

        assertThatDataSourceDecoratingChain(dataSource).containsExactly(CustomDataSourceProxy.class, P6DataSource.class, ProxyDataSource.class, FlexyPoolDataSource.class);
    }

    @Test
    public void testDecoratingCanBeDisabled() throws Exception {
        EnvironmentTestUtils.addEnvironment(context,
                "decorator.datasource.enabled:false");
        context.register(DataSourceAutoConfiguration.class,
                DataSourceDecoratorAutoConfiguration.class,
                PropertyPlaceholderAutoConfiguration.class);
        context.refresh();

        DataSource dataSource = context.getBean(DataSource.class);
        assertThat(dataSource).isInstanceOf(org.apache.tomcat.jdbc.pool.DataSource.class);
    }

    @Test
    public void testDecoratingCanBeDisabledForSpecificBeans() throws Exception {
        EnvironmentTestUtils.addEnvironment(context,
                "decorator.datasource.exclude-beans:secondDataSource");
        context.register(TestMultiDataSourceConfiguration.class,
                DataSourceAutoConfiguration.class,
                DataSourceDecoratorAutoConfiguration.class,
                PropertyPlaceholderAutoConfiguration.class);
        context.refresh();

        DataSource dataSource = context.getBean("dataSource", DataSource.class);
        assertThat(dataSource).isInstanceOf(DecoratedDataSource.class);

        DataSource secondDataSource = context.getBean("secondDataSource", DataSource.class);
        assertThat(secondDataSource).isInstanceOf(BasicDataSource.class);
    }

    @Test
    public void testDecoratingChainBuiltCorrectly() throws Exception {
        System.setProperty(PropertyLoader.PROPERTIES_FILE_PATH, "db/decorator/flexy-pool.properties");
        context.register(DataSourceAutoConfiguration.class,
                DataSourceDecoratorAutoConfiguration.class,
                PropertyPlaceholderAutoConfiguration.class);
        context.refresh();

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
        assertThat(realDataSource).isInstanceOf((Class<? extends DataSource>) org.apache.tomcat.jdbc.pool.DataSource.class);

        assertThatDataSourceDecoratingChain(dataSource).containsExactly(P6DataSource.class, ProxyDataSource.class, FlexyPoolDataSource.class);

        assertThat(dataSource.toString()).isEqualTo(
                "p6SpyDataSourceDecorator [com.p6spy.engine.spy.P6DataSource] -> " +
                        "proxyDataSourceDecorator [net.ttddyy.dsproxy.support.ProxyDataSource] -> " +
                        "flexyPoolDataSourceDecorator [com.vladmihalcea.flexypool.FlexyPoolDataSource] -> " +
                        "dataSource [org.apache.tomcat.jdbc.pool.DataSource]");
    }

    @Test
    public void testReturnDataSourcePoolMetadataProviderForHikari() {
        EnvironmentTestUtils.addEnvironment(context,
                "spring.datasource.type:" + HikariDataSource.class.getName());
        context.register(DataSourceAutoConfiguration.class,
                DataSourceDecoratorAutoConfiguration.class,
                PropertyPlaceholderAutoConfiguration.class);
        context.refresh();

        DataSource dataSource = context.getBean(DataSource.class);
        Assertions.assertThat(dataSource).isInstanceOf(DecoratedDataSource.class);
        DataSourcePoolMetadataProviders poolMetadataProvider = new DataSourcePoolMetadataProviders(context.getBeansOfType(DataSourcePoolMetadataProvider.class).values());
        DataSourcePoolMetadata dataSourcePoolMetadata = poolMetadataProvider.getDataSourcePoolMetadata(dataSource);
        Assertions.assertThat(dataSourcePoolMetadata).isInstanceOf(HikariDataSourcePoolMetadata.class);
    }

    @Test
    public void testReturnDataSourcePoolMetadataProviderForTomcat() {
        context.register(DataSourceAutoConfiguration.class,
                DataSourceDecoratorAutoConfiguration.class,
                PropertyPlaceholderAutoConfiguration.class);
        context.refresh();

        DataSource dataSource = context.getBean(DataSource.class);
        Assertions.assertThat(dataSource).isInstanceOf(DecoratedDataSource.class);
        DataSourcePoolMetadataProviders poolMetadataProvider = new DataSourcePoolMetadataProviders(context.getBeansOfType(DataSourcePoolMetadataProvider.class).values());
        DataSourcePoolMetadata dataSourcePoolMetadata = poolMetadataProvider.getDataSourcePoolMetadata(dataSource);
        Assertions.assertThat(dataSourcePoolMetadata).isInstanceOf(TomcatDataSourcePoolMetadata.class);
    }

    @Test
    public void testReturnDataSourcePoolMetadataProviderForDbcp2() {
        EnvironmentTestUtils.addEnvironment(context,
                "spring.datasource.type:" + org.apache.commons.dbcp2.BasicDataSource.class.getName());
        context.register(DataSourceAutoConfiguration.class,
                DataSourceDecoratorAutoConfiguration.class,
                PropertyPlaceholderAutoConfiguration.class);
        context.refresh();

        DataSource dataSource = context.getBean(DataSource.class);
        Assertions.assertThat(dataSource).isInstanceOf(DecoratedDataSource.class);
        DataSourcePoolMetadataProviders poolMetadataProvider = new DataSourcePoolMetadataProviders(context.getBeansOfType(DataSourcePoolMetadataProvider.class).values());
        DataSourcePoolMetadata dataSourcePoolMetadata = poolMetadataProvider.getDataSourcePoolMetadata(dataSource);
        Assertions.assertThat(dataSourcePoolMetadata).isInstanceOf(CommonsDbcp2DataSourcePoolMetadata.class);
    }

    private AbstractListAssert<?, List<?>, Object, ObjectAssert<Object>> assertThatDataSourceDecoratingChain(DataSource dataSource) {
        return assertThat(((DecoratedDataSource) dataSource).getDecoratingChain()).extracting("dataSource").extracting("class");
    }

    @Configuration
    static class TestDataSourceConfiguration {

        private BasicDataSource pool;

        @Bean
        public DataSource dataSource() {
            pool = new BasicDataSource();
            pool.setDriverClassName("org.hsqldb.jdbcDriver");
            pool.setUrl("jdbc:hsqldb:target/overridedb");
            pool.setUsername("sa");
            return pool;
        }

    }

    @Configuration
    static class TestDataSourceDecoratorConfiguration {

        @Bean
        public DataSourceDecorator customDataSourceDecorator() {
            return (beanName, dataSource) -> new CustomDataSourceProxy(dataSource);
        }

    }

    @Configuration
    static class TestMultiDataSourceConfiguration {

        private BasicDataSource pool;

        @Bean
        @Primary
        public DataSource dataSource() {
            pool = new BasicDataSource();
            pool.setDriverClassName("org.hsqldb.jdbcDriver");
            pool.setUrl("jdbc:hsqldb:target/db");
            pool.setUsername("sa");
            return pool;
        }

        @Bean
        public DataSource secondDataSource() {
            pool = new BasicDataSource();
            pool.setDriverClassName("org.hsqldb.jdbcDriver");
            pool.setUrl("jdbc:hsqldb:target/db2");
            pool.setUsername("sa");
            return pool;
        }

    }

    // see META-INF/services/com.vladmihalcea.flexypool.metric.MetricsFactoryService
    public static class TestMetricsFactoryService implements MetricsFactoryService {

        @Override
        public MetricsFactory load() {
            return mock(MetricsFactory.class, RETURNS_MOCKS);
        }
    }

    /**
     * Custom proxy data source for tests.
     *
     * @author Arthur Gavlyukovskiy
     */
    public static class CustomDataSourceProxy implements DataSource {

        private DataSource delegate;

        public CustomDataSourceProxy(DataSource delegate) {
            this.delegate = delegate;
        }

        @Override
        public Connection getConnection() throws SQLException {
            return null;
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            return null;
        }

        @Override
        public <T> T unwrap(Class<T> iface) throws SQLException {
            return null;
        }

        @Override
        public boolean isWrapperFor(Class<?> iface) throws SQLException {
            return false;
        }

        @Override
        public PrintWriter getLogWriter() throws SQLException {
            return null;
        }

        @Override
        public void setLogWriter(PrintWriter out) throws SQLException {

        }

        @Override
        public void setLoginTimeout(int seconds) throws SQLException {

        }

        @Override
        public int getLoginTimeout() throws SQLException {
            return 0;
        }

        @Override
        public Logger getParentLogger() throws SQLFeatureNotSupportedException {
            return null;
        }
    }
}
