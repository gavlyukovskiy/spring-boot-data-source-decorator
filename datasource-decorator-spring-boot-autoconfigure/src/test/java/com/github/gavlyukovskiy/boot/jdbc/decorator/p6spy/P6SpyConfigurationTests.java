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

package com.github.gavlyukovskiy.boot.jdbc.decorator.p6spy;

import com.github.gavlyukovskiy.boot.jdbc.decorator.DataSourceDecoratorAutoConfiguration;
import com.github.gavlyukovskiy.boot.jdbc.decorator.DecoratedDataSource;
import com.github.gavlyukovskiy.boot.jdbc.decorator.HidePackagesClassLoader;
import com.p6spy.engine.common.ConnectionInformation;
import com.p6spy.engine.event.CompoundJdbcEventListener;
import com.p6spy.engine.event.JdbcEventListener;
import com.p6spy.engine.spy.JdbcEventListenerFactory;
import com.p6spy.engine.spy.P6DataSource;
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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Random;

import static org.assertj.core.api.Java6Assertions.assertThat;

public class P6SpyConfigurationTests {

    private final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

    @Before
    public void init() {
        EnvironmentTestUtils.addEnvironment(context,
                "datasource.initialize:false",
                "datasource.url:jdbc:h2:mem:testdb-" + new Random().nextInt());
        context.setClassLoader(new HidePackagesClassLoader("com.vladmihalcea.flexypool", "net.ttddyy.dsproxy"));
    }

    @After
    public void restore() {
        context.close();
    }

    @Test
    public void testCustomListeners() throws Exception {
        context.register(CustomListenerConfiguration.class,
                DataSourceAutoConfiguration.class,
                DataSourceDecoratorAutoConfiguration.class,
                PropertyPlaceholderAutoConfiguration.class);
        context.refresh();

        DataSource dataSource = context.getBean(DataSource.class);
        JdbcEventListenerFactory jdbcEventListenerFactory = context.getBean(JdbcEventListenerFactory.class);
        GetCountingListener getCountingListener = context.getBean(GetCountingListener.class);
        ClosingCountingListener closingCountingListener = context.getBean(ClosingCountingListener.class);
        P6DataSource p6DataSource = (P6DataSource) ((DecoratedDataSource) dataSource).getDecoratedDataSource();
        assertThat(p6DataSource).extracting("jdbcEventListenerFactory").containsOnly(jdbcEventListenerFactory);

        CompoundJdbcEventListener jdbcEventListener = (CompoundJdbcEventListener) jdbcEventListenerFactory.createJdbcEventListener();

        assertThat(jdbcEventListener.getEventListeners()).contains(getCountingListener, closingCountingListener);
        assertThat(getCountingListener.connectionCount).isEqualTo(0);

        Connection connection1 = p6DataSource.getConnection();

        assertThat(getCountingListener.connectionCount).isEqualTo(1);
        assertThat(closingCountingListener.connectionCount).isEqualTo(0);

        Connection connection2 = p6DataSource.getConnection();

        assertThat(getCountingListener.connectionCount).isEqualTo(2);

        connection1.close();

        assertThat(closingCountingListener.connectionCount).isEqualTo(1);

        connection2.close();

        assertThat(closingCountingListener.connectionCount).isEqualTo(2);
    }

    @Configuration
    static class CustomListenerConfiguration {

        @Bean
        public GetCountingListener wrappingCountingListener() {
            return new GetCountingListener();
        }

        @Bean
        public ClosingCountingListener closingCountingListener() {
            return new ClosingCountingListener();
        }
    }

    static class GetCountingListener extends JdbcEventListener {

        int connectionCount = 0;

        @Override
        public void onAfterGetConnection(ConnectionInformation connectionInformation, SQLException e) {
            connectionCount++;
        }
    }

    static class ClosingCountingListener extends JdbcEventListener {

        int connectionCount = 0;

        @Override
        public void onAfterConnectionClose(ConnectionInformation connectionInformation, SQLException e) {
            connectionCount++;
        }
    }
}
