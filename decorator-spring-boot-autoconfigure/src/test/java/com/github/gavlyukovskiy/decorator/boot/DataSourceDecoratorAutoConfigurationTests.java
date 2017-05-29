package com.github.gavlyukovskiy.decorator.boot;

import com.p6spy.engine.spy.P6DataSource;
import com.vladmihalcea.flexypool.FlexyPoolDataSource;
import com.vladmihalcea.flexypool.config.PropertyLoader;
import com.vladmihalcea.flexypool.metric.MetricsFactory;
import com.vladmihalcea.flexypool.metric.MetricsFactoryService;
import com.zaxxer.hikari.HikariDataSource;
import net.ttddyy.dsproxy.support.ProxyDataSource;
import org.apache.commons.dbcp2.BasicDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

import java.io.PrintWriter;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Random;
import java.util.logging.Logger;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_MOCKS;
import static org.mockito.Mockito.mock;

public class DataSourceDecoratorAutoConfigurationTests {

    private final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

    @Before
    public void init() {
        EnvironmentTestUtils.addEnvironment(this.context,
                "spring.datasource.initialize:false",
                "spring.datasource.url:jdbc:hsqldb:mem:testdb-" + new Random().nextInt());
    }

    @After
    public void restore() {
        System.clearProperty(PropertyLoader.PROPERTIES_FILE_PATH);
        this.context.close();
    }

    @Test
    public void testDecoratingInDefaultOrder() throws Exception {
        System.setProperty(PropertyLoader.PROPERTIES_FILE_PATH, "db/decorator/flexy-pool.properties");
        this.context.register(DataSourceAutoConfiguration.class,
                DataSourceDecoratorAutoConfiguration.class,
                PropertyPlaceholderAutoConfiguration.class);
        this.context.refresh();

        assertDataSourceInDefaultOrder(org.apache.tomcat.jdbc.pool.DataSource.class);
    }

    @Test
    public void testDecoratingWhenDefaultProxyProviderInstanceThrowException() throws Exception {
        this.context.register(DataSourceAutoConfiguration.class,
                DataSourceDecoratorAutoConfiguration.class,
                PropertyPlaceholderAutoConfiguration.class);
        this.context.refresh();
        DataSource dataSource = this.context.getBean(DataSource.class);
        assertThat(dataSource).isNotNull();
        assertThat(dataSource).isInstanceOf(DecoratedDataSource.class);
        DataSource proxyDataSource = ((DecoratedDataSource) dataSource).getDecoratedDataSource();
        assertThat(proxyDataSource).isInstanceOf(ProxyDataSource.class);

        DataSource p6DataSource = (DataSource) new DirectFieldAccessor(proxyDataSource)
                .getPropertyValue("dataSource");
        assertThat(p6DataSource).isNotNull();
        assertThat(p6DataSource).isInstanceOf(P6DataSource.class);

        DataSource realDataSource = (DataSource) new DirectFieldAccessor(p6DataSource)
                .getPropertyValue("realDataSource");
        assertThat(realDataSource).isNotNull();
        assertThat(realDataSource).isInstanceOf(org.apache.tomcat.jdbc.pool.DataSource.class);
    }

    @Test
    public void testDecoratingWhenDefaultProxyProviderNotAvailable() throws Exception {
        this.context.register(DataSourceAutoConfiguration.class,
                DataSourceDecoratorAutoConfiguration.class,
                PropertyPlaceholderAutoConfiguration.class);
        this.context.setClassLoader(new HidePackagesClassLoader("com.vladmihalcea.flexypool"));
        this.context.refresh();
        DataSource dataSource = this.context.getBean(DataSource.class);
        assertThat(dataSource).isNotNull();
        assertThat(dataSource).isInstanceOf(DecoratedDataSource.class);
        DataSource proxyDataSource = ((DecoratedDataSource) dataSource).getDecoratedDataSource();
        assertThat(proxyDataSource).isInstanceOf(ProxyDataSource.class);

        DataSource p6DataSource = (DataSource) new DirectFieldAccessor(proxyDataSource)
                .getPropertyValue("dataSource");
        assertThat(p6DataSource).isNotNull();
        assertThat(p6DataSource).isInstanceOf(P6DataSource.class);

        DataSource realDataSource = (DataSource) new DirectFieldAccessor(p6DataSource)
                .getPropertyValue("realDataSource");
        assertThat(realDataSource).isNotNull();
        assertThat(realDataSource).isInstanceOf(org.apache.tomcat.jdbc.pool.DataSource.class);
    }

    @Test
    public void testDecoratedHikariSpecificPropertiesIsSet() throws Exception {
        EnvironmentTestUtils.addEnvironment(this.context,
                "spring.datasource.type:" + HikariDataSource.class.getName(),
                "spring.datasource.hikari.catalog:test_catalog");
        this.context.register(DataSourceAutoConfiguration.class,
                DataSourceDecoratorAutoConfiguration.class,
                PropertyPlaceholderAutoConfiguration.class);
        this.context.refresh();
        DataSource dataSource = this.context.getBean(DataSource.class);
        assertThat(dataSource).isNotNull();
        assertThat(dataSource).isInstanceOf(DecoratedDataSource.class);
        DataSource realDataSource = ((DecoratedDataSource) dataSource).getRealDataSource();
        assertThat(realDataSource).isInstanceOf(HikariDataSource.class);
        assertThat(((HikariDataSource) realDataSource).getCatalog()).isEqualTo("test_catalog");
    }

    @Test
    public void testDefaultDataSourceIsDecorated() throws Exception {
        this.context.register(TestDataSourceConfiguration.class,
                DataSourceAutoConfiguration.class,
                DataSourceDecoratorAutoConfiguration.class,
                PropertyPlaceholderAutoConfiguration.class);
        this.context.refresh();
        DataSource dataSource = this.context.getBean(DataSource.class);
        assertThat(dataSource).isInstanceOf(DecoratedDataSource.class);
        DataSource realDataSource = ((DecoratedDataSource) dataSource).getRealDataSource();
        assertThat(realDataSource).isInstanceOf(BasicDataSource.class);
    }

    @Test
    public void testCustomDataSourceDecoratorApplied() throws Exception {
        System.setProperty(PropertyLoader.PROPERTIES_FILE_PATH, "db/decorator/flexy-pool.properties");
        this.context.register(TestDataSourceDecoratorConfiguration.class,
                DataSourceAutoConfiguration.class,
                DataSourceDecoratorAutoConfiguration.class,
                PropertyPlaceholderAutoConfiguration.class);
        this.context.refresh();

        assertDataSourceInDefaultOrder(CustomDataSourceProxy.class);

        DataSource dataSource = this.context.getBean(DataSource.class);
        DataSource realDataSource = ((DecoratedDataSource) dataSource).getRealDataSource();
        assertThat(realDataSource).isInstanceOf(org.apache.tomcat.jdbc.pool.DataSource.class);
    }

    @Test
    public void testDecoratingCanBeDisabled() throws Exception {
        EnvironmentTestUtils.addEnvironment(this.context,
                "spring.datasource.decorator.enabled:false");
        this.context.register(DataSourceAutoConfiguration.class,
                DataSourceDecoratorAutoConfiguration.class,
                PropertyPlaceholderAutoConfiguration.class);
        this.context.refresh();

        DataSource dataSource = this.context.getBean(DataSource.class);
        assertThat(dataSource).isInstanceOf(org.apache.tomcat.jdbc.pool.DataSource.class);
    }

    @Test
    public void testDecoratingCanBeDisabledForSpecificBeans() throws Exception {
        EnvironmentTestUtils.addEnvironment(this.context,
                "spring.datasource.decorator.exclude-beans:secondDataSource");
        this.context.register(TestMultiDataSourceConfiguration.class,
                DataSourceAutoConfiguration.class,
                DataSourceDecoratorAutoConfiguration.class,
                PropertyPlaceholderAutoConfiguration.class);
        this.context.refresh();

        DataSource dataSource = this.context.getBean("dataSource", DataSource.class);
        assertThat(dataSource).isInstanceOf(DecoratedDataSource.class);

        DataSource secondDataSource = this.context.getBean("secondDataSource", DataSource.class);
        assertThat(secondDataSource).isInstanceOf(BasicDataSource.class);
    }

    private void assertDataSourceInDefaultOrder(Class<? extends DataSource> realDataSourceClass) {
        DecoratedDataSource dataSource = this.context.getBean(DecoratedDataSource.class);
        assertThat(dataSource).isNotNull();

        DataSource flexyDataSource = dataSource.getDecoratedDataSource();
        assertThat(flexyDataSource).isNotNull();
        assertThat(flexyDataSource).isInstanceOf(FlexyPoolDataSource.class);

        DataSource proxyDataSource = (DataSource) new DirectFieldAccessor(flexyDataSource)
                .getPropertyValue("targetDataSource");
        assertThat(proxyDataSource).isNotNull();
        assertThat(proxyDataSource).isInstanceOf(ProxyDataSource.class);

        DataSource p6DataSource = (DataSource) new DirectFieldAccessor(proxyDataSource)
                .getPropertyValue("dataSource");
        assertThat(p6DataSource).isNotNull();
        assertThat(p6DataSource).isInstanceOf(P6DataSource.class);

        DataSource realDataSource = (DataSource) new DirectFieldAccessor(p6DataSource)
                .getPropertyValue("realDataSource");
        assertThat(realDataSource).isNotNull();
        assertThat(realDataSource).isInstanceOf(realDataSourceClass);
    }

    @Configuration
    static class TestDataSourceConfiguration {

        private BasicDataSource pool;

        @Bean
        public DataSource dataSource() {
            this.pool = new BasicDataSource();
            this.pool.setDriverClassName("org.hsqldb.jdbcDriver");
            this.pool.setUrl("jdbc:hsqldb:target/overridedb");
            this.pool.setUsername("sa");
            return this.pool;
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
            this.pool = new BasicDataSource();
            this.pool.setDriverClassName("org.hsqldb.jdbcDriver");
            this.pool.setUrl("jdbc:hsqldb:target/db");
            this.pool.setUsername("sa");
            return this.pool;
        }

        @Bean
        public DataSource secondDataSource() {
            this.pool = new BasicDataSource();
            this.pool.setDriverClassName("org.hsqldb.jdbcDriver");
            this.pool.setUrl("jdbc:hsqldb:target/db2");
            this.pool.setUsername("sa");
            return this.pool;
        }

    }

    // see META-INF/services/com.vladmihalcea.flexypool.metric.MetricsFactoryService
    public static class TestMetricsFactoryService implements MetricsFactoryService {

        @Override
        public MetricsFactory load() {
            return mock(MetricsFactory.class, RETURNS_MOCKS);
        }
    }

    private static final class HidePackagesClassLoader extends URLClassLoader {

        private final String[] hiddenPackages;

        private HidePackagesClassLoader(String... hiddenPackages) {
            super(new URL[0], DataSourceDecoratorAutoConfigurationTests.class.getClassLoader());
            this.hiddenPackages = hiddenPackages;
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve)
                throws ClassNotFoundException {
            for (String hiddenPackage : this.hiddenPackages) {
                if (name.startsWith(hiddenPackage)) {
                    throw new ClassNotFoundException();
                }
            }
            return super.loadClass(name, resolve);
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
