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
import net.ttddyy.dsproxy.listener.logging.CommonsQueryLoggingListener;
import net.ttddyy.dsproxy.listener.logging.CommonsSlowQueryListener;
import net.ttddyy.dsproxy.listener.logging.JULQueryLoggingListener;
import net.ttddyy.dsproxy.listener.logging.JULSlowQueryListener;
import net.ttddyy.dsproxy.listener.logging.SLF4JQueryLoggingListener;
import net.ttddyy.dsproxy.listener.logging.SLF4JSlowQueryListener;
import net.ttddyy.dsproxy.listener.logging.SystemOutQueryLoggingListener;
import net.ttddyy.dsproxy.listener.logging.SystemOutSlowQueryListener;
import net.ttddyy.dsproxy.support.ProxyDataSource;
import net.ttddyy.dsproxy.transform.ParameterTransformer;
import net.ttddyy.dsproxy.transform.QueryTransformer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Java6Assertions.assertThat;

class ProxyDataSourceConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    DataSourceAutoConfiguration.class,
                    DataSourceDecoratorAutoConfiguration.class,
                    PropertyPlaceholderAutoConfiguration.class
            ))
            .withPropertyValues("spring.datasource.initialization-mode=never",
                    "spring.datasource.url:jdbc:h2:mem:testdb-" + new Random().nextInt())
            .withClassLoader(new HidePackagesClassLoader("com.vladmihalcea.flexypool", "com.p6spy"));

    @Test
    void testRegisterLogAndSlowQueryLogByDefaultToSlf4j() throws Exception {
        contextRunner.run(context -> {
            DataSource dataSource = context.getBean(DataSource.class);
            ProxyDataSource proxyDataSource = (ProxyDataSource) ((DecoratedDataSource) dataSource).getDecoratedDataSource();
            ChainListener chainListener = proxyDataSource.getProxyConfig().getQueryListener();
            assertThat(chainListener.getListeners()).extracting("class").contains(SLF4JSlowQueryListener.class);
            assertThat(chainListener.getListeners()).extracting("class").contains(SLF4JQueryLoggingListener.class);
        });
    }
    @Test
    void testRegisterLogAndSlowQueryLogByUsingSlf4j() throws Exception {
        ApplicationContextRunner contextRunner = this.contextRunner.withPropertyValues("decorator.datasource.datasource-proxy.logging:slf4j");

        contextRunner.run(context -> {
            DataSource dataSource = context.getBean(DataSource.class);
            ProxyDataSource proxyDataSource = (ProxyDataSource) ((DecoratedDataSource) dataSource).getDecoratedDataSource();
            ChainListener chainListener = proxyDataSource.getProxyConfig().getQueryListener();
            assertThat(chainListener.getListeners()).extracting("class").contains(SLF4JSlowQueryListener.class);
            assertThat(chainListener.getListeners()).extracting("class").contains(SLF4JQueryLoggingListener.class);
        });
    }

    @Test
    void testRegisterLogAndSlowQueryLogUsingSystemOut() throws Exception {
        ApplicationContextRunner contextRunner = this.contextRunner.withPropertyValues("decorator.datasource.datasource-proxy.logging:sysout");

        contextRunner.run(context -> {
            DataSource dataSource = context.getBean(DataSource.class);
            ProxyDataSource proxyDataSource = (ProxyDataSource) ((DecoratedDataSource) dataSource).getDecoratedDataSource();
            ChainListener chainListener = proxyDataSource.getProxyConfig().getQueryListener();
            assertThat(chainListener.getListeners()).extracting("class").contains(SystemOutSlowQueryListener.class);
            assertThat(chainListener.getListeners()).extracting("class").contains(SystemOutQueryLoggingListener.class);
        });
    }

    @Test
    void testRegisterLogAndSlowQueryLogUsingJUL() throws Exception {
        ApplicationContextRunner contextRunner = this.contextRunner.withPropertyValues("decorator.datasource.datasourceProxy.logging:jul");

        contextRunner.run(context -> {
            DataSource dataSource = context.getBean(DataSource.class);
            ProxyDataSource proxyDataSource = (ProxyDataSource) ((DecoratedDataSource) dataSource).getDecoratedDataSource();
            ChainListener chainListener = proxyDataSource.getProxyConfig().getQueryListener();
            assertThat(chainListener.getListeners()).extracting("class").contains(JULSlowQueryListener.class);
            assertThat(chainListener.getListeners()).extracting("class").contains(JULQueryLoggingListener.class);
        });
    }

    @Test
    void testRegisterLogAndSlowQueryLogUsingApacheCommons() throws Exception {
        ApplicationContextRunner contextRunner = this.contextRunner.withPropertyValues("decorator.datasource.datasourceProxy.logging:commons");

        contextRunner.run(context -> {
            DataSource dataSource = context.getBean(DataSource.class);
            ProxyDataSource proxyDataSource = (ProxyDataSource) ((DecoratedDataSource) dataSource).getDecoratedDataSource();
            ChainListener chainListener = proxyDataSource.getProxyConfig().getQueryListener();
            assertThat(chainListener.getListeners()).extracting("class").contains(CommonsSlowQueryListener.class);
            assertThat(chainListener.getListeners()).extracting("class").contains(CommonsQueryLoggingListener.class);
        });
    }

    @Test
    void testCustomParameterAndQueryTransformer() throws Exception {
        ApplicationContextRunner contextRunner = this.contextRunner.withUserConfiguration(CustomParameterAndQueryTransformerConfiguration.class);

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
    void testCustomListeners() throws Exception {
        ApplicationContextRunner contextRunner = this.contextRunner.withUserConfiguration(CustomListenerConfiguration.class);

        contextRunner.run(context -> {
            DataSource dataSource = context.getBean(DataSource.class);
            ProxyDataSource proxyDataSource = (ProxyDataSource) ((DecoratedDataSource) dataSource).getDecoratedDataSource();
            QueryExecutionListener queryExecutionListener = context.getBean(QueryExecutionListener.class);

            ChainListener chainListener = proxyDataSource.getProxyConfig().getQueryListener();
            assertThat(chainListener.getListeners()).contains(queryExecutionListener);
        });
    }

    @Configuration
    static class CustomParameterAndQueryTransformerConfiguration {

        @Bean
        public ParameterTransformer parameterTransformer() {
            return (replacer, transformInfo) -> {};
        }

        @Bean
        public QueryTransformer queryTransformer() {
            return (transformInfo) -> "TestQuery";
        }
    }

    @Configuration
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
}
