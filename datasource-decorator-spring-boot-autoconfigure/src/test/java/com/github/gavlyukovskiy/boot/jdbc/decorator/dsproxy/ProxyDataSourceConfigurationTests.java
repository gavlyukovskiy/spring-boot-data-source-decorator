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

package com.github.gavlyukovskiy.boot.jdbc.decorator.dsproxy;

import com.github.gavlyukovskiy.boot.jdbc.decorator.DataSourceDecoratorAutoConfiguration;
import com.github.gavlyukovskiy.boot.jdbc.decorator.DecoratedDataSource;
import com.github.gavlyukovskiy.boot.jdbc.decorator.HidePackagesClassLoader;
import net.ttddyy.dsproxy.ExecutionInfo;
import net.ttddyy.dsproxy.QueryInfo;
import net.ttddyy.dsproxy.listener.ChainListener;
import net.ttddyy.dsproxy.listener.QueryExecutionListener;
import net.ttddyy.dsproxy.listener.logging.AbstractQueryLoggingListener;
import net.ttddyy.dsproxy.listener.logging.CommonsQueryLoggingListener;
import net.ttddyy.dsproxy.listener.logging.CommonsSlowQueryListener;
import net.ttddyy.dsproxy.listener.logging.JULQueryLoggingListener;
import net.ttddyy.dsproxy.listener.logging.JULSlowQueryListener;
import net.ttddyy.dsproxy.listener.logging.LoggingFilter;
import net.ttddyy.dsproxy.listener.logging.SLF4JQueryLoggingListener;
import net.ttddyy.dsproxy.listener.logging.SLF4JSlowQueryListener;
import net.ttddyy.dsproxy.listener.logging.SystemOutQueryLoggingListener;
import net.ttddyy.dsproxy.listener.logging.SystemOutSlowQueryListener;
import net.ttddyy.dsproxy.proxy.DefaultConnectionIdManager;
import net.ttddyy.dsproxy.proxy.GlobalConnectionIdManager;
import net.ttddyy.dsproxy.support.ProxyDataSource;
import net.ttddyy.dsproxy.transform.ParameterTransformer;
import net.ttddyy.dsproxy.transform.QueryTransformer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class ProxyDataSourceConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    DataSourceAutoConfiguration.class,
                    DataSourceDecoratorAutoConfiguration.class,
                    PropertyPlaceholderAutoConfiguration.class
            ))
            .withPropertyValues("spring.sql.init.mode=never",
                    "spring.datasource.url:jdbc:h2:mem:testdb-" + ThreadLocalRandom.current().nextInt())
            .withClassLoader(new HidePackagesClassLoader("com.vladmihalcea.flexypool", "com.p6spy"));

    @Test
    void testRegisterLogAndSlowQueryLogByDefaultToSlf4j() {
        contextRunner.run(context -> {
            DataSource dataSource = context.getBean(DataSource.class);
            ProxyDataSource proxyDataSource = (ProxyDataSource) ((DecoratedDataSource) dataSource).getDecoratedDataSource();
            ChainListener chainListener = proxyDataSource.getProxyConfig().getQueryListener();
            assertThat(chainListener.getListeners()).hasExactlyElementsOfTypes(
                    SLF4JQueryLoggingListener.class,
                    SLF4JSlowQueryListener.class
            );
        });
    }

    @Test
    void testRegisterLogAndSlowQueryLogByUsingSlf4j() {
        ApplicationContextRunner contextRunner = this.contextRunner.withPropertyValues("decorator.datasource.datasource-proxy.logging:slf4j");

        contextRunner.run(context -> {
            DataSource dataSource = context.getBean(DataSource.class);
            ProxyDataSource proxyDataSource = (ProxyDataSource) ((DecoratedDataSource) dataSource).getDecoratedDataSource();
            ChainListener chainListener = proxyDataSource.getProxyConfig().getQueryListener();
            assertThat(chainListener.getListeners()).hasExactlyElementsOfTypes(
                    SLF4JQueryLoggingListener.class,
                    SLF4JSlowQueryListener.class
            );
        });
    }

    @Test
    void testRegisterLogAndSlowQueryLogUsingSystemOut() {
        ApplicationContextRunner contextRunner = this.contextRunner.withPropertyValues("decorator.datasource.datasource-proxy.logging:sysout");

        contextRunner.run(context -> {
            DataSource dataSource = context.getBean(DataSource.class);
            ProxyDataSource proxyDataSource = (ProxyDataSource) ((DecoratedDataSource) dataSource).getDecoratedDataSource();
            ChainListener chainListener = proxyDataSource.getProxyConfig().getQueryListener();
            assertThat(chainListener.getListeners()).hasExactlyElementsOfTypes(
                    SystemOutQueryLoggingListener.class,
                    SystemOutSlowQueryListener.class
            );
        });
    }

    @Test
    void testRegisterLogAndSlowQueryLogUsingJUL() {
        ApplicationContextRunner contextRunner = this.contextRunner.withPropertyValues("decorator.datasource.datasourceProxy.logging:jul");

        contextRunner.run(context -> {
            DataSource dataSource = context.getBean(DataSource.class);
            ProxyDataSource proxyDataSource = (ProxyDataSource) ((DecoratedDataSource) dataSource).getDecoratedDataSource();
            ChainListener chainListener = proxyDataSource.getProxyConfig().getQueryListener();
            assertThat(chainListener.getListeners()).hasExactlyElementsOfTypes(
                    JULQueryLoggingListener.class,
                    JULSlowQueryListener.class
            );
        });
    }

    @Test
    void testRegisterLogAndSlowQueryLogUsingApacheCommons() {
        ApplicationContextRunner contextRunner = this.contextRunner.withPropertyValues("decorator.datasource.datasourceProxy.logging:commons");

        contextRunner.run(context -> {
            DataSource dataSource = context.getBean(DataSource.class);
            ProxyDataSource proxyDataSource = (ProxyDataSource) ((DecoratedDataSource) dataSource).getDecoratedDataSource();
            ChainListener chainListener = proxyDataSource.getProxyConfig().getQueryListener();
            assertThat(chainListener.getListeners()).hasExactlyElementsOfTypes(
                    CommonsQueryLoggingListener.class,
                    CommonsSlowQueryListener.class
            );
        });
    }

    @Test
    void testSlowQueryWithoutTimeUnit() {
        ApplicationContextRunner contextRunner = this.contextRunner.withPropertyValues(
                "decorator.datasource.datasource-proxy.slow-query.threshold:50"
        );

        contextRunner.run(context -> {
            DataSource dataSource = context.getBean(DataSource.class);
            ProxyDataSource proxyDataSource = (ProxyDataSource) ((DecoratedDataSource) dataSource).getDecoratedDataSource();
            var slowQueryListener = findListener(proxyDataSource, SLF4JSlowQueryListener.class);
            assertThat(slowQueryListener.getThreshold()).isEqualTo(50000);
            assertThat(slowQueryListener.getThresholdTimeUnit()).isEqualTo(TimeUnit.MILLISECONDS);
        });
    }

    @Test
    void testSlowQueryMilliseconds() {
        ApplicationContextRunner contextRunner = this.contextRunner.withPropertyValues(
                "decorator.datasource.datasource-proxy.slow-query.threshold:50ms"
        );

        contextRunner.run(context -> {
            DataSource dataSource = context.getBean(DataSource.class);
            ProxyDataSource proxyDataSource = (ProxyDataSource) ((DecoratedDataSource) dataSource).getDecoratedDataSource();
            var slowQueryListener = findListener(proxyDataSource, SLF4JSlowQueryListener.class);
            assertThat(slowQueryListener.getThreshold()).isEqualTo(50);
            assertThat(slowQueryListener.getThresholdTimeUnit()).isEqualTo(TimeUnit.MILLISECONDS);
        });
    }

    private static <T> T findListener(ProxyDataSource proxyDataSource, Class<T> clazz) {
        return proxyDataSource.getProxyConfig()
                .getQueryListener()
                .getListeners()
                .stream()
                .filter(clazz::isInstance)
                .map(clazz::cast)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Listener of type " + clazz.getName() + " not found"));
    }

    @Test
    void testCustomParameterAndQueryTransformer() {
        ApplicationContextRunner contextRunner = this.contextRunner.withUserConfiguration(CustomDataSourceProxyConfiguration.class);

        contextRunner.run(context -> {
            DataSource dataSource = context.getBean(DataSource.class);
            ProxyDataSource proxyDataSource = (ProxyDataSource) ((DecoratedDataSource) dataSource).getDecoratedDataSource();
            ParameterTransformer parameterTransformer = context.getBean(ParameterTransformer.class);
            QueryTransformer queryTransformer = context.getBean(QueryTransformer.class);
            assertThat(proxyDataSource.getProxyConfig().getParameterTransformer()).isSameAs(parameterTransformer);
            assertThat(proxyDataSource.getProxyConfig().getQueryTransformer()).isSameAs(queryTransformer);
        });
    }

    @Test
    void testCustomListeners() {
        ApplicationContextRunner contextRunner = this.contextRunner.withUserConfiguration(CustomListenerConfiguration.class);

        contextRunner.run(context -> {
            DataSource dataSource = context.getBean(DataSource.class);
            ProxyDataSource proxyDataSource = (ProxyDataSource) ((DecoratedDataSource) dataSource).getDecoratedDataSource();
            QueryExecutionListener queryExecutionListener = context.getBean(QueryExecutionListener.class);

            ChainListener chainListener = proxyDataSource.getProxyConfig().getQueryListener();
            assertThat(chainListener.getListeners()).contains(queryExecutionListener);
        });
    }

    @Test
    void testGlobalConnectionIdManagerByDefault() {
        contextRunner.run(context -> {
            DataSource dataSource = context.getBean(DataSource.class);
            ProxyDataSource proxyDataSource = (ProxyDataSource) ((DecoratedDataSource) dataSource).getDecoratedDataSource();

            assertThat(proxyDataSource.getConnectionIdManager()).isInstanceOf(GlobalConnectionIdManager.class);
        });
    }

    @Test
    void testCustomConnectionIdManager() {
        ApplicationContextRunner contextRunner = this.contextRunner.withUserConfiguration(CustomDataSourceProxyConfiguration.class);

        contextRunner.run(context -> {
            DataSource dataSource = context.getBean(DataSource.class);
            ProxyDataSource proxyDataSource = (ProxyDataSource) ((DecoratedDataSource) dataSource).getDecoratedDataSource();

            assertThat(proxyDataSource.getConnectionIdManager()).isInstanceOf(DefaultConnectionIdManager.class);
        });
    }

    @Test
    void testCustomLoggingFilter() {
        ApplicationContextRunner contextRunner = this.contextRunner.withUserConfiguration(CustomLoggingFilterConfiguration.class);

        contextRunner.run(context -> {
            DataSource dataSource = context.getBean(DataSource.class);
            ProxyDataSource proxyDataSource = (ProxyDataSource) ((DecoratedDataSource) dataSource).getDecoratedDataSource();
            LoggingFilter loggingFilter = context.getBean(LoggingFilter.class);
            var queryListener = findListener(proxyDataSource, AbstractQueryLoggingListener.class);
            assertThat(queryListener.getLoggingFilter()).isSameAs(loggingFilter);
        });
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomDataSourceProxyConfiguration {

        @Bean
        public ParameterTransformer parameterTransformer() {
            return (replacer, transformInfo) -> {};
        }

        @Bean
        public QueryTransformer queryTransformer() {
            return (transformInfo) -> "TestQuery";
        }

        @Bean
        public ConnectionIdManagerProvider connectionIdManagerProvider() {
            return DefaultConnectionIdManager::new;
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomListenerConfiguration {

        @Bean
        public QueryExecutionListener queryExecutionListener() {
            return new QueryExecutionListener() {
                @Override
                public void beforeQuery(ExecutionInfo execInfo, List<QueryInfo> queryInfoList) {
                    System.out.println("beforeQuery");
                }

                @Override
                public void afterQuery(ExecutionInfo execInfo, List<QueryInfo> queryInfoList) {
                    System.out.println("afterQuery");
                }
            };
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomLoggingFilterConfiguration {

        @Bean
        public LoggingFilter loggingFilter() {
            return (execInfo, queryInfoList) -> true;
        }
    }
}
