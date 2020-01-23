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
import com.p6spy.engine.common.P6LogQuery;
import com.p6spy.engine.event.CompoundJdbcEventListener;
import com.p6spy.engine.event.JdbcEventListener;
import com.p6spy.engine.logging.LoggingEventListener;
import com.p6spy.engine.spy.JdbcEventListenerFactory;
import com.p6spy.engine.spy.P6DataSource;
import com.p6spy.engine.spy.appender.CustomLineFormat;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;

public class P6SpyConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    DataSourceAutoConfiguration.class,
                    DataSourceDecoratorAutoConfiguration.class,
                    PropertyPlaceholderAutoConfiguration.class
            ))
            .withPropertyValues("spring.datasource.initialization-mode=never",
                    "spring.datasource.url:jdbc:h2:mem:testdb-" + ThreadLocalRandom.current().nextInt())
            .withClassLoader(new HidePackagesClassLoader("com.vladmihalcea.flexypool", "net.ttddyy.dsproxy"));

    @Test
    void testCustomListeners() {
        ApplicationContextRunner contextRunner = this.contextRunner.withUserConfiguration(CustomListenerConfiguration.class);

        contextRunner.run(context -> {
            DataSource dataSource = context.getBean(DataSource.class);
            JdbcEventListenerFactory jdbcEventListenerFactory = context.getBean(JdbcEventListenerFactory.class);
            GetCountingListener getCountingListener = context.getBean(GetCountingListener.class);
            ClosingCountingListener closingCountingListener = context.getBean(ClosingCountingListener.class);
            P6DataSource p6DataSource = (P6DataSource) ((DecoratedDataSource) dataSource).getDecoratedDataSource();
            assertThat(p6DataSource).extracting("jdbcEventListenerFactory").isEqualTo(jdbcEventListenerFactory);

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
        });
    }

    @Test
    void testDoesNotRegisterLoggingListenerIfDisabled() {
        ApplicationContextRunner contextRunner = this.contextRunner.withPropertyValues("decorator.datasource.p6spy.enable-logging:false");

        contextRunner.run(context -> {
            JdbcEventListenerFactory jdbcEventListenerFactory = context.getBean(JdbcEventListenerFactory.class);
            CompoundJdbcEventListener jdbcEventListener = (CompoundJdbcEventListener) jdbcEventListenerFactory.createJdbcEventListener();

            assertThat(jdbcEventListener.getEventListeners()).extracting("class").doesNotContain(LoggingEventListener.class);
        });
    }

    @Test
    void testCanSetCustomLoggingFormat() {
        ApplicationContextRunner contextRunner = this.contextRunner.withPropertyValues("decorator.datasource.p6spy.log-format:test %{connectionId}");

        contextRunner.run(context -> {
            JdbcEventListenerFactory jdbcEventListenerFactory = context.getBean(JdbcEventListenerFactory.class);
            CompoundJdbcEventListener jdbcEventListener = (CompoundJdbcEventListener) jdbcEventListenerFactory.createJdbcEventListener();

            assertThat(jdbcEventListener.getEventListeners()).extracting("class").contains(LoggingEventListener.class);
            assertThat(P6LogQuery.getLogger()).extracting("strategy").extracting("class").isEqualTo(CustomLineFormat.class);
        });
    }

    @Test
    void testMultilineShouldNotOverrideCustomProperties() {
        System.setProperty("p6spy.config.logMessageFormat", "com.p6spy.engine.spy.appender.CustomLineFormat");
        System.setProperty("p6spy.config.excludecategories", "debug");
        ApplicationContextRunner contextRunner = this.contextRunner.withPropertyValues("decorator.datasource.p6spy.multiline:true");

        contextRunner.run(context -> {
            JdbcEventListenerFactory jdbcEventListenerFactory = context.getBean(JdbcEventListenerFactory.class);
            CompoundJdbcEventListener jdbcEventListener = (CompoundJdbcEventListener) jdbcEventListenerFactory.createJdbcEventListener();

            assertThat(jdbcEventListener.getEventListeners()).extracting("class").contains(LoggingEventListener.class);
            assertThat(P6LogQuery.getLogger()).extracting("strategy").extracting("class").isEqualTo(CustomLineFormat.class);
        });
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
