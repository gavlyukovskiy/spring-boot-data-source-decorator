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
import net.ttddyy.dsproxy.support.ProxyDataSource;
import net.ttddyy.dsproxy.transform.ParameterTransformer;
import net.ttddyy.dsproxy.transform.QueryTransformer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Java6Assertions.assertThat;

public class ProxyDataSourceConfigurationTests {

    private final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

    @Before
    public void init() {
        EnvironmentTestUtils.addEnvironment(context,
                "datasource.initialize:false",
                "datasource.url:jdbc:h2:mem:testdb-" + new Random().nextInt());
        context.setClassLoader(new HidePackagesClassLoader("com.vladmihalcea.flexypool", "com.p6spy"));
    }

    @After
    public void restore() {
        context.close();
    }

    @Test
    public void testRegisterLogAndSlowQueryLogByDefaultToSlf4j() throws Exception {
        context.register(DataSourceAutoConfiguration.class,
                DataSourceDecoratorAutoConfiguration.class,
                PropertyPlaceholderAutoConfiguration.class);
        context.refresh();

        DataSource dataSource = context.getBean(DataSource.class);
        ProxyDataSource proxyDataSource = (ProxyDataSource) ((DecoratedDataSource) dataSource).getDecoratedDataSource();
        ChainListener chainListener = (ChainListener) proxyDataSource.getInterceptorHolder().getListener();
        assertThat(chainListener.getListeners()).extracting("class").contains(SLF4JSlowQueryListener.class);
        assertThat(chainListener.getListeners()).extracting("class").contains(SLF4JQueryLoggingListener.class);
    }

    @Test
    public void testRegisterLogAndSlowQueryLogUsingSlf4j() throws Exception {
        EnvironmentTestUtils.addEnvironment(context,
                "datasource.decorator.datasource-proxy.logging:slf4j");
        context.register(DataSourceAutoConfiguration.class,
                DataSourceDecoratorAutoConfiguration.class,
                PropertyPlaceholderAutoConfiguration.class);
        context.refresh();

        DataSource dataSource = context.getBean(DataSource.class);
        ProxyDataSource proxyDataSource = (ProxyDataSource) ((DecoratedDataSource) dataSource).getDecoratedDataSource();
        ChainListener chainListener = (ChainListener) proxyDataSource.getInterceptorHolder().getListener();
        assertThat(chainListener.getListeners()).extracting("class").contains(SLF4JSlowQueryListener.class);
        assertThat(chainListener.getListeners()).extracting("class").contains(SLF4JQueryLoggingListener.class);
    }

    @Test
    public void testRegisterLogAndSlowQueryLogUsingJUL() throws Exception {
        EnvironmentTestUtils.addEnvironment(context,
                "datasource.decorator.datasourceProxy.logging:jul");
        context.register(DataSourceAutoConfiguration.class,
                DataSourceDecoratorAutoConfiguration.class,
                PropertyPlaceholderAutoConfiguration.class);
        context.refresh();

        DataSource dataSource = context.getBean(DataSource.class);
        ProxyDataSource proxyDataSource = (ProxyDataSource) ((DecoratedDataSource) dataSource).getDecoratedDataSource();
        ChainListener chainListener = (ChainListener) proxyDataSource.getInterceptorHolder().getListener();
        assertThat(chainListener.getListeners()).extracting("class").contains(JULSlowQueryListener.class);
        assertThat(chainListener.getListeners()).extracting("class").contains(JULQueryLoggingListener.class);
    }

    @Test
    public void testRegisterLogAndSlowQueryLogUsingApacheCommons() throws Exception {
        EnvironmentTestUtils.addEnvironment(context,
                "datasource.decorator.datasourceProxy.logging:commons");
        context.register(DataSourceAutoConfiguration.class,
                DataSourceDecoratorAutoConfiguration.class,
                PropertyPlaceholderAutoConfiguration.class);
        context.refresh();

        DataSource dataSource = context.getBean(DataSource.class);
        ProxyDataSource proxyDataSource = (ProxyDataSource) ((DecoratedDataSource) dataSource).getDecoratedDataSource();
        ChainListener chainListener = (ChainListener) proxyDataSource.getInterceptorHolder().getListener();
        assertThat(chainListener.getListeners()).extracting("class").contains(CommonsSlowQueryListener.class);
        assertThat(chainListener.getListeners()).extracting("class").contains(CommonsQueryLoggingListener.class);
    }

    @Test
    public void testCustomParameterAndQueryTransformer() throws Exception {
        context.register(CustomParameterAndQueryTransformerConfiguration.class,
                DataSourceAutoConfiguration.class,
                DataSourceDecoratorAutoConfiguration.class,
                PropertyPlaceholderAutoConfiguration.class);
        context.refresh();

        DataSource dataSource = context.getBean(DataSource.class);
        ProxyDataSource proxyDataSource = (ProxyDataSource) ((DecoratedDataSource) dataSource).getDecoratedDataSource();
        ParameterTransformer parameterTransformer = context.getBean(ParameterTransformer.class);
        QueryTransformer queryTransformer = context.getBean(QueryTransformer.class);
        assertThat(proxyDataSource.getInterceptorHolder().getParameterTransformer()).isSameAs(parameterTransformer);
        assertThat(proxyDataSource.getInterceptorHolder().getQueryTransformer()).isSameAs(queryTransformer);
    }

    @Test
    public void testCustomListeners() throws Exception {
        context.register(CustomListenerConfiguration.class,
                DataSourceAutoConfiguration.class,
                DataSourceDecoratorAutoConfiguration.class,
                PropertyPlaceholderAutoConfiguration.class);
        context.refresh();

        DataSource dataSource = context.getBean(DataSource.class);
        ProxyDataSource proxyDataSource = (ProxyDataSource) ((DecoratedDataSource) dataSource).getDecoratedDataSource();
        QueryExecutionListener queryExecutionListener = context.getBean(QueryExecutionListener.class);

        ChainListener chainListener = (ChainListener) proxyDataSource.getInterceptorHolder().getListener();
        assertThat(chainListener.getListeners()).contains(queryExecutionListener);
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
