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

package com.github.gavlyukovskiy.cloud.sleuth;

import com.github.gavlyukovskiy.boot.jdbc.decorator.DataSourceDecoratorAutoConfiguration;
import com.github.gavlyukovskiy.boot.jdbc.decorator.DecoratedDataSource;
import com.github.gavlyukovskiy.boot.jdbc.decorator.HidePackagesClassLoader;
import com.p6spy.engine.event.CompoundJdbcEventListener;
import com.p6spy.engine.spy.JdbcEventListenerFactory;
import net.ttddyy.dsproxy.listener.ChainListener;
import net.ttddyy.dsproxy.support.ProxyDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.cloud.sleuth.autoconfig.TraceAutoConfiguration;
import org.springframework.cloud.sleuth.log.SleuthLogAutoConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import javax.sql.DataSource;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

public class SleuthListenerAutoConfigurationTests {

    private final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

    @Before
    public void init() {
        EnvironmentTestUtils.addEnvironment(context,
                "spring.datasource.initialize:false",
                "spring.datasource.url:jdbc:h2:mem:testdb-" + new Random().nextInt());
    }

    @After
    public void restore() {
        context.close();
    }

    @Test
    public void testAddsP6SpyListener() {
        context.setClassLoader(new HidePackagesClassLoader("com.vladmihalcea.flexypool", "net.ttddyy.dsproxy"));
        context.register(DataSourceAutoConfiguration.class,
                DataSourceDecoratorAutoConfiguration.class,
                TraceAutoConfiguration.class,
                SleuthLogAutoConfiguration.class,
                SleuthListenerAutoConfiguration.class,
                PropertyPlaceholderAutoConfiguration.class);
        context.refresh();

        JdbcEventListenerFactory jdbcEventListenerFactory = context.getBean(JdbcEventListenerFactory.class);
        CompoundJdbcEventListener jdbcEventListener = (CompoundJdbcEventListener) jdbcEventListenerFactory.createJdbcEventListener();
        assertThat(jdbcEventListener.getEventListeners()).extracting("class").contains(TracingJdbcEventListener.class);
    }

    @Test
    public void testDoesNotAddP6SpyListenerIfNoTracer() {
        context.setClassLoader(new HidePackagesClassLoader("com.vladmihalcea.flexypool", "net.ttddyy.dsproxy"));
        context.register(DataSourceAutoConfiguration.class,
                DataSourceDecoratorAutoConfiguration.class,
                SleuthListenerAutoConfiguration.class,
                PropertyPlaceholderAutoConfiguration.class);
        context.refresh();

        JdbcEventListenerFactory jdbcEventListenerFactory = context.getBean(JdbcEventListenerFactory.class);
        CompoundJdbcEventListener jdbcEventListener = (CompoundJdbcEventListener) jdbcEventListenerFactory.createJdbcEventListener();
        assertThat(jdbcEventListener.getEventListeners()).extracting("class").doesNotContain(TracingJdbcEventListener.class);
    }

    @Test
    public void testAddsDatasourceProxyListener() {
        context.setClassLoader(new HidePackagesClassLoader("com.vladmihalcea.flexypool", "com.p6spy"));
        context.register(DataSourceAutoConfiguration.class,
                DataSourceDecoratorAutoConfiguration.class,
                TraceAutoConfiguration.class,
                SleuthLogAutoConfiguration.class,
                SleuthListenerAutoConfiguration.class,
                PropertyPlaceholderAutoConfiguration.class);
        context.refresh();

        DataSource dataSource = context.getBean(DataSource.class);
        ProxyDataSource proxyDataSource = (ProxyDataSource) ((DecoratedDataSource) dataSource).getDecoratedDataSource();
        ChainListener chainListener = proxyDataSource.getProxyConfig().getQueryListener();
        assertThat(chainListener.getListeners()).extracting("class").contains(TracingQueryExecutionListener.class);
    }

    @Test
    public void testDoesntAddDatasourceProxyListenerIfNoTracer() {
        context.setClassLoader(new HidePackagesClassLoader("com.vladmihalcea.flexypool", "com.p6spy"));
        context.register(DataSourceAutoConfiguration.class,
                DataSourceDecoratorAutoConfiguration.class,
                SleuthListenerAutoConfiguration.class,
                PropertyPlaceholderAutoConfiguration.class);
        context.refresh();

        DataSource dataSource = context.getBean(DataSource.class);
        ProxyDataSource proxyDataSource = (ProxyDataSource) ((DecoratedDataSource) dataSource).getDecoratedDataSource();
        ChainListener chainListener = proxyDataSource.getProxyConfig().getQueryListener();
        assertThat(chainListener.getListeners()).extracting("class").doesNotContain(TracingQueryExecutionListener.class);
    }
}