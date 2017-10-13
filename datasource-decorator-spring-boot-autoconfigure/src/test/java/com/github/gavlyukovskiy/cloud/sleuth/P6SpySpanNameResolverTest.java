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
import com.p6spy.engine.common.ConnectionInformation;
import com.p6spy.engine.common.StatementInformation;
import com.p6spy.engine.spy.P6DataSource;
import com.zaxxer.hikari.HikariDataSource;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.cloud.sleuth.autoconfig.TraceAutoConfiguration;
import org.springframework.cloud.sleuth.log.SleuthLogAutoConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

import java.util.Random;

@RunWith(MockitoJUnitRunner.class)
public class P6SpySpanNameResolverTest {

    private final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

    private P6SpySpanNameResolver resolver;
    @Mock
    private ConnectionInformation connectionInformation;
    @Mock
    private StatementInformation statementInformation;

    @Before
    public void init() {
        EnvironmentTestUtils.addEnvironment(context,
                "spring.datasource.initialize:false",
                "spring.datasource.url:jdbc:h2:mem:testdb-" + new Random().nextInt());
        context.setClassLoader(new HidePackagesClassLoader("com.vladmihalcea.flexypool", "net.ttddyy.dsproxy"));
        context.register(MyDataSourceConfiguration.class,
                DataSourceAutoConfiguration.class,
                DataSourceDecoratorAutoConfiguration.class,
                TraceAutoConfiguration.class,
                SleuthLogAutoConfiguration.class,
                SleuthListenerAutoConfiguration.class,
                PropertyPlaceholderAutoConfiguration.class);
        context.refresh();

        resolver = context.getBean(P6SpySpanNameResolver.class);
        DataSource dataSource = context.getBean(DataSource.class);
        DataSource decoratedDataSource = ((DecoratedDataSource) dataSource).getDecoratedDataSource();
        Assertions.assertThat(decoratedDataSource).isInstanceOf(P6DataSource.class);
        Mockito.when(statementInformation.getConnectionInformation()).thenReturn(connectionInformation);
        Mockito.when(connectionInformation.getDataSource()).thenReturn(decoratedDataSource);
    }

    @After
    public void restore() {
        context.close();
    }

    @Test
    public void testShouldReturnConnectionSpanNameFromBeanName() throws Exception {
        String querySpanName = resolver.connectionSpanName(connectionInformation);

        Assertions.assertThat(querySpanName).isEqualTo("jdbc:/myDs/connection");
    }

    @Test
    public void testShouldReturnQuerySpanNameFromBeanName() throws Exception {
        String querySpanName = resolver.querySpanName(statementInformation);

        Assertions.assertThat(querySpanName).isEqualTo("jdbc:/myDs/query");
    }

    @Configuration
    static class MyDataSourceConfiguration {

        @Bean
        public DataSource myDs() {
            return new HikariDataSource();
        }
    }
}