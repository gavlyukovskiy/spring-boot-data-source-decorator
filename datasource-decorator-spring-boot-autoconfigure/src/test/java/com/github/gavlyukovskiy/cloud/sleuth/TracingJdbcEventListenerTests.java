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
import com.github.gavlyukovskiy.boot.jdbc.decorator.HidePackagesClassLoader;
import com.github.gavlyukovskiy.cloud.sleuth.SavingSpanReporterConfiguration.CollectingSpanReporter;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.autoconfig.TraceAutoConfiguration;
import org.springframework.cloud.sleuth.log.SleuthLogAutoConfiguration;
import org.springframework.cloud.sleuth.util.ExceptionUtils;

import javax.sql.DataSource;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

class TracingJdbcEventListenerTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    DataSourceAutoConfiguration.class,
                    DataSourceDecoratorAutoConfiguration.class,
                    TraceAutoConfiguration.class,
                    SleuthLogAutoConfiguration.class,
                    SleuthListenerAutoConfiguration.class,
                    SavingSpanReporterConfiguration.class,
                    PropertyPlaceholderAutoConfiguration.class
            ))
            .withPropertyValues("spring.datasource.initialization-mode=never",
                    "spring.datasource.url:jdbc:h2:mem:testdb-" + new Random().nextInt(),
                    "spring.datasource.hikari.pool-name=test")
            .withClassLoader(new HidePackagesClassLoader("com.vladmihalcea.flexypool", "net.ttddyy.dsproxy"));
    
    @Test
    void testShouldAddSpanForConnection() {
        contextRunner.run(context -> {
            DataSource dataSource = context.getBean(DataSource.class);
            CollectingSpanReporter spanReporter = context.getBean(CollectingSpanReporter.class);

            Connection connection = dataSource.getConnection();
            connection.commit();
            connection.rollback();
            connection.close();

            assertThat(ExceptionUtils.getLastException()).isNull();

            Assertions.assertThat(spanReporter.getSpans()).hasSize(1);
            Span connectionSpan = spanReporter.getSpans().get(0);
            assertThat(connectionSpan.getName()).isEqualTo("jdbc:/test/connection");
            assertThat(connectionSpan.logs()).extracting("event").contains("commit");
            assertThat(connectionSpan.logs()).extracting("event").contains("rollback");
        });
    }

    @Test
    void testShouldAddSpanForPreparedStatementExecute() throws Exception {
        contextRunner.run(context -> {
            DataSource dataSource = context.getBean(DataSource.class);
            CollectingSpanReporter spanReporter = context.getBean(CollectingSpanReporter.class);

            Connection connection = dataSource.getConnection();
            connection.prepareStatement("SELECT NOW()").execute();
            connection.close();

            assertThat(ExceptionUtils.getLastException()).isNull();

            Assertions.assertThat(spanReporter.getSpans()).hasSize(2);
            Span connectionSpan = spanReporter.getSpans().get(0);
            Span statementSpan = spanReporter.getSpans().get(1);
            assertThat(connectionSpan.getName()).isEqualTo("jdbc:/test/connection");
            assertThat(statementSpan.getName()).isEqualTo("jdbc:/test/query");
            assertThat(statementSpan.tags()).containsEntry(SleuthListenerAutoConfiguration.SPAN_SQL_QUERY_TAG_NAME, "SELECT NOW()");
        });
    }

    @Test
    void testShouldAddSpanForPreparedStatementExecuteUpdate() throws Exception {
        contextRunner.run(context -> {
            DataSource dataSource = context.getBean(DataSource.class);
            CollectingSpanReporter spanReporter = context.getBean(CollectingSpanReporter.class);

            Connection connection = dataSource.getConnection();
            connection.prepareStatement("UPDATE INFORMATION_SCHEMA.TABLES SET table_Name = '' WHERE 0 = 1").executeUpdate();
            connection.close();

            assertThat(ExceptionUtils.getLastException()).isNull();

            Assertions.assertThat(spanReporter.getSpans()).hasSize(2);
            Span connectionSpan = spanReporter.getSpans().get(0);
            Span statementSpan = spanReporter.getSpans().get(1);
            assertThat(connectionSpan.getName()).isEqualTo("jdbc:/test/connection");
            assertThat(statementSpan.getName()).isEqualTo("jdbc:/test/query");
            assertThat(statementSpan.tags()).containsEntry(SleuthListenerAutoConfiguration.SPAN_SQL_QUERY_TAG_NAME,
                    "UPDATE INFORMATION_SCHEMA.TABLES SET table_Name = '' WHERE 0 = 1");
            assertThat(statementSpan.tags()).containsEntry(SleuthListenerAutoConfiguration.SPAN_ROW_COUNT_TAG_NAME, "0");
        });
    }

    @Test
    void testShouldAddSpanForStatementExecuteUpdate() throws Exception {
        contextRunner.run(context -> {
            DataSource dataSource = context.getBean(DataSource.class);
            CollectingSpanReporter spanReporter = context.getBean(CollectingSpanReporter.class);

            Connection connection = dataSource.getConnection();
            connection.createStatement().executeUpdate("UPDATE INFORMATION_SCHEMA.TABLES SET table_Name = '' WHERE 0 = 1");
            connection.close();

            assertThat(ExceptionUtils.getLastException()).isNull();

            Assertions.assertThat(spanReporter.getSpans()).hasSize(2);
            Span connectionSpan = spanReporter.getSpans().get(0);
            Span statementSpan = spanReporter.getSpans().get(1);
            assertThat(connectionSpan.getName()).isEqualTo("jdbc:/test/connection");
            assertThat(statementSpan.getName()).isEqualTo("jdbc:/test/query");
            assertThat(statementSpan.tags()).containsEntry(SleuthListenerAutoConfiguration.SPAN_SQL_QUERY_TAG_NAME,
                    "UPDATE INFORMATION_SCHEMA.TABLES SET table_Name = '' WHERE 0 = 1");
            assertThat(statementSpan.tags()).containsEntry(SleuthListenerAutoConfiguration.SPAN_ROW_COUNT_TAG_NAME, "0");
        });
    }

    @Test
    void testShouldAddSpanForPreparedStatementExecuteQueryIncludingTimeToCloseResultSet() throws Exception {
        contextRunner.run(context -> {
            DataSource dataSource = context.getBean(DataSource.class);
            CollectingSpanReporter spanReporter = context.getBean(CollectingSpanReporter.class);

            Connection connection = dataSource.getConnection();
            ResultSet resultSet = connection.prepareStatement("SELECT NOW()").executeQuery();
            resultSet.next();
            resultSet.next();
            resultSet.close();
            connection.close();

            assertThat(ExceptionUtils.getLastException()).isNull();

            Assertions.assertThat(spanReporter.getSpans()).hasSize(3);
            Span connectionSpan = spanReporter.getSpans().get(0);
            Span resultSetSpan = spanReporter.getSpans().get(1);
            Span statementSpan = spanReporter.getSpans().get(2);
            assertThat(connectionSpan.getName()).isEqualTo("jdbc:/test/connection");
            assertThat(statementSpan.getName()).isEqualTo("jdbc:/test/query");
            assertThat(resultSetSpan.getName()).isEqualTo("jdbc:/test/fetch");
            assertThat(statementSpan.tags()).containsEntry(SleuthListenerAutoConfiguration.SPAN_SQL_QUERY_TAG_NAME, "SELECT NOW()");
            assertThat(resultSetSpan.tags()).containsEntry(SleuthListenerAutoConfiguration.SPAN_ROW_COUNT_TAG_NAME, "1");
        });
    }

    @Test
    void testShouldAddSpanForStatementAndResultSet() throws Exception {
        contextRunner.run(context -> {
            DataSource dataSource = context.getBean(DataSource.class);
            CollectingSpanReporter spanReporter = context.getBean(CollectingSpanReporter.class);

            Connection connection = dataSource.getConnection();
            ResultSet resultSet = connection.createStatement().executeQuery("SELECT NOW()");
            resultSet.next();
            Thread.sleep(200L);
            resultSet.close();
            connection.close();

            assertThat(ExceptionUtils.getLastException()).isNull();

            Assertions.assertThat(spanReporter.getSpans()).hasSize(3);
            Span connectionSpan = spanReporter.getSpans().get(0);
            Span resultSetSpan = spanReporter.getSpans().get(1);
            Span statementSpan = spanReporter.getSpans().get(2);
            assertThat(connectionSpan.getName()).isEqualTo("jdbc:/test/connection");
            assertThat(statementSpan.getName()).isEqualTo("jdbc:/test/query");
            assertThat(resultSetSpan.getName()).isEqualTo("jdbc:/test/fetch");
            assertThat(statementSpan.tags()).containsEntry(SleuthListenerAutoConfiguration.SPAN_SQL_QUERY_TAG_NAME, "SELECT NOW()");
            assertThat(resultSetSpan.tags()).containsEntry(SleuthListenerAutoConfiguration.SPAN_ROW_COUNT_TAG_NAME, "1");
        });
    }

    @Test
    void testShouldNotFailWhenStatementIsClosedWihoutResultSet() throws Exception {
        contextRunner.run(context -> {
            DataSource dataSource = context.getBean(DataSource.class);
            CollectingSpanReporter spanReporter = context.getBean(CollectingSpanReporter.class);

            Connection connection = dataSource.getConnection();
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT NOW()");
            resultSet.next();
            statement.close();
            connection.close();

            assertThat(ExceptionUtils.getLastException()).isNull();

            Assertions.assertThat(spanReporter.getSpans()).hasSize(3);
            Span connectionSpan = spanReporter.getSpans().get(0);
            Span resultSetSpan = spanReporter.getSpans().get(1);
            Span statementSpan = spanReporter.getSpans().get(2);
            assertThat(connectionSpan.getName()).isEqualTo("jdbc:/test/connection");
            assertThat(statementSpan.getName()).isEqualTo("jdbc:/test/query");
            assertThat(resultSetSpan.getName()).isEqualTo("jdbc:/test/fetch");
            assertThat(statementSpan.tags()).containsEntry(SleuthListenerAutoConfiguration.SPAN_SQL_QUERY_TAG_NAME, "SELECT NOW()");
        });
    }

    @Test
    void testShouldNotFailWhenConnectionIsClosedWihoutResultSet() throws Exception {
        contextRunner.run(context -> {
            DataSource dataSource = context.getBean(DataSource.class);
            CollectingSpanReporter spanReporter = context.getBean(CollectingSpanReporter.class);

            Connection connection = dataSource.getConnection();
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT NOW()");
            resultSet.next();
            connection.close();

            assertThat(ExceptionUtils.getLastException()).isNull();

            Assertions.assertThat(spanReporter.getSpans()).hasSize(3);
            Span connectionSpan = spanReporter.getSpans().get(0);
            Span resultSetSpan = spanReporter.getSpans().get(1);
            Span statementSpan = spanReporter.getSpans().get(2);
            assertThat(connectionSpan.getName()).isEqualTo("jdbc:/test/connection");
            assertThat(statementSpan.getName()).isEqualTo("jdbc:/test/query");
            assertThat(resultSetSpan.getName()).isEqualTo("jdbc:/test/fetch");
            assertThat(statementSpan.tags()).containsEntry(SleuthListenerAutoConfiguration.SPAN_SQL_QUERY_TAG_NAME, "SELECT NOW()");
        });
    }

    @Test
    void testShouldNotFailWhenResultSetNextWasNotCalled() throws Exception {
        contextRunner.run(context -> {
            DataSource dataSource = context.getBean(DataSource.class);
            CollectingSpanReporter spanReporter = context.getBean(CollectingSpanReporter.class);

            Connection connection = dataSource.getConnection();
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT NOW()");
            resultSet.close();
            statement.close();
            connection.close();

            assertThat(ExceptionUtils.getLastException()).isNull();

            Assertions.assertThat(spanReporter.getSpans()).hasSize(3);
            Span connectionSpan = spanReporter.getSpans().get(0);
            Span resultSetSpan = spanReporter.getSpans().get(1);
            Span statementSpan = spanReporter.getSpans().get(2);
            assertThat(connectionSpan.getName()).isEqualTo("jdbc:/test/connection");
            assertThat(statementSpan.getName()).isEqualTo("jdbc:/test/query");
            assertThat(resultSetSpan.getName()).isEqualTo("jdbc:/test/fetch");
            assertThat(statementSpan.tags()).containsEntry(SleuthListenerAutoConfiguration.SPAN_SQL_QUERY_TAG_NAME, "SELECT NOW()");
        });
    }

    @Test
    void testShouldNotFailWhenResourceIsAlreadyClosed() throws Exception {
        contextRunner.run(context -> {
            DataSource dataSource = context.getBean(DataSource.class);
            CollectingSpanReporter spanReporter = context.getBean(CollectingSpanReporter.class);

            Connection connection = dataSource.getConnection();
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT NOW()");
            resultSet.close();
            resultSet.close();
            statement.close();
            statement.close();
            connection.close();
            connection.close();

            assertThat(ExceptionUtils.getLastException()).isNull();

            Assertions.assertThat(spanReporter.getSpans()).hasSize(3);
            Span connectionSpan = spanReporter.getSpans().get(0);
            Span resultSetSpan = spanReporter.getSpans().get(1);
            Span statementSpan = spanReporter.getSpans().get(2);
            assertThat(connectionSpan.getName()).isEqualTo("jdbc:/test/connection");
            assertThat(statementSpan.getName()).isEqualTo("jdbc:/test/query");
            assertThat(resultSetSpan.getName()).isEqualTo("jdbc:/test/fetch");
            assertThat(statementSpan.tags()).containsEntry(SleuthListenerAutoConfiguration.SPAN_SQL_QUERY_TAG_NAME, "SELECT NOW()");
        });
    }

    @Test
    void testShouldNotFailWhenResourceIsAlreadyClosed2() {
        contextRunner.run(context -> {
            DataSource dataSource = context.getBean(DataSource.class);
            CollectingSpanReporter spanReporter = context.getBean(CollectingSpanReporter.class);

            Connection connection = dataSource.getConnection();
            try {
                connection.close();
                connection.prepareStatement("SELECT NOW()");
                fail("should fail due to closed connection");
            }
            catch (SQLException expected) {
            }

            assertThat(ExceptionUtils.getLastException()).isNull();

            Assertions.assertThat(spanReporter.getSpans()).hasSize(1);
            Span connectionSpan = spanReporter.getSpans().get(0);
            assertThat(connectionSpan.getName()).isEqualTo("jdbc:/test/connection");
        });
    }
}
