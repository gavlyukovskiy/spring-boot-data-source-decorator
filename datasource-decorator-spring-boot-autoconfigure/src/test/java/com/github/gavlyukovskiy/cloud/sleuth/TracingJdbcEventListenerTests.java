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
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.sleuth.autoconfig.TraceAutoConfiguration;
import org.springframework.cloud.sleuth.log.SleuthLogAutoConfiguration;
import org.springframework.cloud.sleuth.util.ArrayListSpanReporter;
import zipkin2.Span;

import javax.sql.DataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
                    "spring.datasource.url:jdbc:h2:mem:testdb-" + ThreadLocalRandom.current().nextInt(),
                    "spring.datasource.hikari.pool-name=test")
            .withClassLoader(new HidePackagesClassLoader("com.vladmihalcea.flexypool", "net.ttddyy.dsproxy"));

    @Test
    void testShouldAddSpanForConnection() {
        contextRunner.run(context -> {
            DataSource dataSource = context.getBean(DataSource.class);
            ArrayListSpanReporter spanReporter = context.getBean(ArrayListSpanReporter.class);

            Connection connection = dataSource.getConnection();
            connection.commit();
            connection.rollback();
            connection.close();

            assertThat(spanReporter.getSpans()).hasSize(1);
            Span connectionSpan = spanReporter.getSpans().get(0);
            assertThat(connectionSpan.name()).isEqualTo("jdbc:/test/connection");
            assertThat(connectionSpan.annotations()).extracting("value").contains("commit");
            assertThat(connectionSpan.annotations()).extracting("value").contains("rollback");
        });
    }

    @Test
    void testShouldAddSpanForPreparedStatementExecute() {
        contextRunner.run(context -> {
            DataSource dataSource = context.getBean(DataSource.class);
            ArrayListSpanReporter spanReporter = context.getBean(ArrayListSpanReporter.class);

            Connection connection = dataSource.getConnection();
            connection.prepareStatement("SELECT NOW()").execute();
            connection.close();

            assertThat(spanReporter.getSpans()).hasSize(2);
            Span connectionSpan = spanReporter.getSpans().get(1);
            Span statementSpan = spanReporter.getSpans().get(0);
            assertThat(connectionSpan.name()).isEqualTo("jdbc:/test/connection");
            assertThat(statementSpan.name()).isEqualTo("jdbc:/test/query");
            assertThat(statementSpan.tags()).containsEntry(SleuthListenerAutoConfiguration.SPAN_SQL_QUERY_TAG_NAME, "SELECT NOW()");
        });
    }

    @Test
    void testShouldAddSpanForPreparedStatementExecuteUpdate() {
        contextRunner.run(context -> {
            DataSource dataSource = context.getBean(DataSource.class);
            ArrayListSpanReporter spanReporter = context.getBean(ArrayListSpanReporter.class);

            Connection connection = dataSource.getConnection();
            connection.prepareStatement("UPDATE INFORMATION_SCHEMA.TABLES SET table_Name = '' WHERE 0 = 1").executeUpdate();
            connection.close();

            assertThat(spanReporter.getSpans()).hasSize(2);
            Span connectionSpan = spanReporter.getSpans().get(1);
            Span statementSpan = spanReporter.getSpans().get(0);
            assertThat(connectionSpan.name()).isEqualTo("jdbc:/test/connection");
            assertThat(statementSpan.name()).isEqualTo("jdbc:/test/query");
            assertThat(statementSpan.tags()).containsEntry(SleuthListenerAutoConfiguration.SPAN_SQL_QUERY_TAG_NAME,
                    "UPDATE INFORMATION_SCHEMA.TABLES SET table_Name = '' WHERE 0 = 1");
            assertThat(statementSpan.tags()).containsEntry(SleuthListenerAutoConfiguration.SPAN_ROW_COUNT_TAG_NAME, "0");
        });
    }

    @Test
    void testShouldUsePlaceholderInSqlTagOfSpansForPreparedStatementIfIncludeParameterValuesIsSetToFalse() {
        contextRunner.withPropertyValues("decorator.datasource.p6spy.include-parameter-values=false")
                .run(context -> {
                    DataSource dataSource = context.getBean(DataSource.class);
                    ArrayListSpanReporter spanReporter = context.getBean(ArrayListSpanReporter.class);

                    Connection connection = dataSource.getConnection();
                    PreparedStatement preparedStatement = connection.prepareStatement("UPDATE INFORMATION_SCHEMA.TABLES SET table_Name = ? WHERE 0 = ?");
                    preparedStatement.setString(1, "");
                    preparedStatement.setInt(2, 1);
                    preparedStatement.executeUpdate();
                    connection.close();

                    assertThat(spanReporter.getSpans()).hasSize(2);
                    Span connectionSpan = spanReporter.getSpans().get(1);
                    Span statementSpan = spanReporter.getSpans().get(0);
                    assertThat(connectionSpan.name()).isEqualTo("jdbc:/test/connection");
                    assertThat(statementSpan.name()).isEqualTo("jdbc:/test/query");
                    assertThat(statementSpan.tags()).containsEntry(SleuthListenerAutoConfiguration.SPAN_SQL_QUERY_TAG_NAME,
                            "UPDATE INFORMATION_SCHEMA.TABLES SET table_Name = ? WHERE 0 = ?");
                    assertThat(statementSpan.tags()).containsEntry(SleuthListenerAutoConfiguration.SPAN_ROW_COUNT_TAG_NAME, "0");
                });
    }

    @Test
    void testShouldAddSpanForStatementExecuteUpdate() {
        contextRunner.run(context -> {
            DataSource dataSource = context.getBean(DataSource.class);
            ArrayListSpanReporter spanReporter = context.getBean(ArrayListSpanReporter.class);

            Connection connection = dataSource.getConnection();
            connection.createStatement().executeUpdate("UPDATE INFORMATION_SCHEMA.TABLES SET table_Name = '' WHERE 0 = 1");
            connection.close();

            assertThat(spanReporter.getSpans()).hasSize(2);
            Span connectionSpan = spanReporter.getSpans().get(1);
            Span statementSpan = spanReporter.getSpans().get(0);
            assertThat(connectionSpan.name()).isEqualTo("jdbc:/test/connection");
            assertThat(statementSpan.name()).isEqualTo("jdbc:/test/query");
            assertThat(statementSpan.tags()).containsEntry(SleuthListenerAutoConfiguration.SPAN_SQL_QUERY_TAG_NAME,
                    "UPDATE INFORMATION_SCHEMA.TABLES SET table_Name = '' WHERE 0 = 1");
            assertThat(statementSpan.tags()).containsEntry(SleuthListenerAutoConfiguration.SPAN_ROW_COUNT_TAG_NAME, "0");
        });
    }

    @Test
    void testShouldAddSpanForPreparedStatementExecuteQueryIncludingTimeToCloseResultSet() {
        contextRunner.run(context -> {
            DataSource dataSource = context.getBean(DataSource.class);
            ArrayListSpanReporter spanReporter = context.getBean(ArrayListSpanReporter.class);

            Connection connection = dataSource.getConnection();
            ResultSet resultSet = connection.prepareStatement("SELECT NOW() UNION ALL SELECT NOW()").executeQuery();
            resultSet.next();
            resultSet.next();
            resultSet.close();
            connection.close();

            assertThat(spanReporter.getSpans()).hasSize(3);
            Span connectionSpan = spanReporter.getSpans().get(2);
            Span resultSetSpan = spanReporter.getSpans().get(1);
            Span statementSpan = spanReporter.getSpans().get(0);
            assertThat(connectionSpan.name()).isEqualTo("jdbc:/test/connection");
            assertThat(statementSpan.name()).isEqualTo("jdbc:/test/query");
            assertThat(resultSetSpan.name()).isEqualTo("jdbc:/test/fetch");
            assertThat(statementSpan.tags()).containsEntry(SleuthListenerAutoConfiguration.SPAN_SQL_QUERY_TAG_NAME, "SELECT NOW() UNION ALL SELECT NOW()");
            assertThat(resultSetSpan.tags()).containsEntry(SleuthListenerAutoConfiguration.SPAN_ROW_COUNT_TAG_NAME, "2");
        });
    }

    @Test
    void testShouldAddSpanForStatementAndResultSet() {
        contextRunner.run(context -> {
            DataSource dataSource = context.getBean(DataSource.class);
            ArrayListSpanReporter spanReporter = context.getBean(ArrayListSpanReporter.class);

            Connection connection = dataSource.getConnection();
            ResultSet resultSet = connection.createStatement().executeQuery("SELECT NOW()");
            resultSet.next();
            Thread.sleep(200L);
            resultSet.close();
            connection.close();

            assertThat(spanReporter.getSpans()).hasSize(3);
            Span connectionSpan = spanReporter.getSpans().get(2);
            Span resultSetSpan = spanReporter.getSpans().get(1);
            Span statementSpan = spanReporter.getSpans().get(0);
            assertThat(connectionSpan.name()).isEqualTo("jdbc:/test/connection");
            assertThat(statementSpan.name()).isEqualTo("jdbc:/test/query");
            assertThat(resultSetSpan.name()).isEqualTo("jdbc:/test/fetch");
            assertThat(statementSpan.tags()).containsEntry(SleuthListenerAutoConfiguration.SPAN_SQL_QUERY_TAG_NAME, "SELECT NOW()");
            assertThat(resultSetSpan.tags()).containsEntry(SleuthListenerAutoConfiguration.SPAN_ROW_COUNT_TAG_NAME, "1");
        });
    }

    @Test
    void testShouldNotFailWhenStatementIsClosedWihoutResultSet() {
        contextRunner.run(context -> {
            DataSource dataSource = context.getBean(DataSource.class);
            ArrayListSpanReporter spanReporter = context.getBean(ArrayListSpanReporter.class);

            Connection connection = dataSource.getConnection();
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT NOW()");
            resultSet.next();
            statement.close();
            connection.close();

            assertThat(spanReporter.getSpans()).hasSize(3);
            Span connectionSpan = spanReporter.getSpans().get(2);
            Span resultSetSpan = spanReporter.getSpans().get(1);
            Span statementSpan = spanReporter.getSpans().get(0);
            assertThat(connectionSpan.name()).isEqualTo("jdbc:/test/connection");
            assertThat(statementSpan.name()).isEqualTo("jdbc:/test/query");
            assertThat(resultSetSpan.name()).isEqualTo("jdbc:/test/fetch");
            assertThat(statementSpan.tags()).containsEntry(SleuthListenerAutoConfiguration.SPAN_SQL_QUERY_TAG_NAME, "SELECT NOW()");
        });
    }

    @Test
    void testShouldNotFailWhenConnectionIsClosedWihoutResultSet() {
        contextRunner.run(context -> {
            DataSource dataSource = context.getBean(DataSource.class);
            ArrayListSpanReporter spanReporter = context.getBean(ArrayListSpanReporter.class);

            Connection connection = dataSource.getConnection();
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT NOW()");
            resultSet.next();
            connection.close();

            assertThat(spanReporter.getSpans()).hasSize(3);
            Span connectionSpan = spanReporter.getSpans().get(2);
            Span resultSetSpan = spanReporter.getSpans().get(1);
            Span statementSpan = spanReporter.getSpans().get(0);
            assertThat(connectionSpan.name()).isEqualTo("jdbc:/test/connection");
            assertThat(statementSpan.name()).isEqualTo("jdbc:/test/query");
            assertThat(resultSetSpan.name()).isEqualTo("jdbc:/test/fetch");
            assertThat(statementSpan.tags()).containsEntry(SleuthListenerAutoConfiguration.SPAN_SQL_QUERY_TAG_NAME, "SELECT NOW()");
        });
    }

    @Test
    void testShouldNotFailWhenResultSetNextWasNotCalled() {
        contextRunner.run(context -> {
            DataSource dataSource = context.getBean(DataSource.class);
            ArrayListSpanReporter spanReporter = context.getBean(ArrayListSpanReporter.class);

            Connection connection = dataSource.getConnection();
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT NOW()");
            resultSet.next();
            resultSet.close();
            statement.close();
            connection.close();

            assertThat(spanReporter.getSpans()).hasSize(3);
            Span connectionSpan = spanReporter.getSpans().get(2);
            Span resultSetSpan = spanReporter.getSpans().get(1);
            Span statementSpan = spanReporter.getSpans().get(0);
            assertThat(connectionSpan.name()).isEqualTo("jdbc:/test/connection");
            assertThat(statementSpan.name()).isEqualTo("jdbc:/test/query");
            assertThat(resultSetSpan.name()).isEqualTo("jdbc:/test/fetch");
            assertThat(statementSpan.tags()).containsEntry(SleuthListenerAutoConfiguration.SPAN_SQL_QUERY_TAG_NAME, "SELECT NOW()");
        });
    }

    @Test
    void testShouldNotFailWhenResourceIsAlreadyClosed() {
        contextRunner.run(context -> {
            DataSource dataSource = context.getBean(DataSource.class);
            ArrayListSpanReporter spanReporter = context.getBean(ArrayListSpanReporter.class);

            Connection connection = dataSource.getConnection();
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT NOW()");
            resultSet.next();
            resultSet.close();
            resultSet.close();
            statement.close();
            statement.close();
            connection.close();
            connection.close();

            assertThat(spanReporter.getSpans()).hasSize(3);
            Span connectionSpan = spanReporter.getSpans().get(2);
            Span resultSetSpan = spanReporter.getSpans().get(1);
            Span statementSpan = spanReporter.getSpans().get(0);
            assertThat(connectionSpan.name()).isEqualTo("jdbc:/test/connection");
            assertThat(statementSpan.name()).isEqualTo("jdbc:/test/query");
            assertThat(resultSetSpan.name()).isEqualTo("jdbc:/test/fetch");
            assertThat(statementSpan.tags()).containsEntry(SleuthListenerAutoConfiguration.SPAN_SQL_QUERY_TAG_NAME, "SELECT NOW()");
        });
    }

    @Test
    void testShouldNotFailWhenResourceIsAlreadyClosed2() {
        contextRunner.run(context -> {
            DataSource dataSource = context.getBean(DataSource.class);
            ArrayListSpanReporter spanReporter = context.getBean(ArrayListSpanReporter.class);

            Connection connection = dataSource.getConnection();
            try {
                connection.close();
                connection.prepareStatement("SELECT NOW()");
                fail("should fail due to closed connection");
            }
            catch (SQLException expected) {
            }

            assertThat(spanReporter.getSpans()).hasSize(1);
            Span connectionSpan = spanReporter.getSpans().get(0);
            assertThat(connectionSpan.name()).isEqualTo("jdbc:/test/connection");
        });
    }

    @Test
    void testShouldNotFailToCloseSpanForTwoConsecutiveConnections() {
        contextRunner.run(context -> {
            DataSource dataSource = context.getBean(DataSource.class);
            ArrayListSpanReporter spanReporter = context.getBean(ArrayListSpanReporter.class);

            Connection connection1 = dataSource.getConnection();
            Connection connection2 = dataSource.getConnection();
            connection1.close();
            connection2.close();

            assertThat(spanReporter.getSpans()).hasSize(2);
            Span connection1Span = spanReporter.getSpans().get(0);
            Span connection2Span = spanReporter.getSpans().get(1);
            assertThat(connection1Span.name()).isEqualTo("jdbc:/test/connection");
            assertThat(connection2Span.name()).isEqualTo("jdbc:/test/connection");
        });
    }

    @Test
    void testShouldNotFailWhenClosedInReversedOrder() {
        contextRunner.run(context -> {
            DataSource dataSource = context.getBean(DataSource.class);
            ArrayListSpanReporter spanReporter = context.getBean(ArrayListSpanReporter.class);

            Connection connection = dataSource.getConnection();
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT NOW()");
            resultSet.next();
            connection.close();
            statement.close();
            resultSet.close();

            assertThat(spanReporter.getSpans()).hasSize(3);
            Span connectionSpan = spanReporter.getSpans().get(2);
            Span resultSetSpan = spanReporter.getSpans().get(1);
            Span statementSpan = spanReporter.getSpans().get(0);
            assertThat(connectionSpan.name()).isEqualTo("jdbc:/test/connection");
            assertThat(statementSpan.name()).isEqualTo("jdbc:/test/query");
            assertThat(resultSetSpan.name()).isEqualTo("jdbc:/test/fetch");
            assertThat(statementSpan.tags()).containsEntry(SleuthListenerAutoConfiguration.SPAN_SQL_QUERY_TAG_NAME, "SELECT NOW()");
        });
    }

    @Test
    void testSingleConnectionAcrossMultipleThreads() {
        contextRunner.run(context -> {
            DataSource dataSource = context.getBean(DataSource.class);
            ArrayListSpanReporter spanReporter = context.getBean(ArrayListSpanReporter.class);

            Connection connection = dataSource.getConnection();
            IntStream.range(0, 5)
                    .mapToObj(i -> CompletableFuture.runAsync(() -> {
                        try {
                            Statement statement = connection.createStatement();
                            ResultSet resultSet = statement.executeQuery("SELECT NOW()");
                            resultSet.next();
                            statement.close();
                            resultSet.close();
                        }
                        catch (SQLException e) {
                            throw new IllegalStateException(e);
                        }
                    }))
                    .collect(Collectors.toList())
                    .forEach(CompletableFuture::join);
            connection.close();

            assertThat(spanReporter.getSpans()).hasSize(1 + 2 * 5);
            assertThat(spanReporter.getSpans()).extracting("name")
                    .contains("jdbc:/test/query", "jdbc:/test/fetch", "jdbc:/test/connection");
        });
    }

    @Test
    void testShouldIncludeOnlyConnectionTraces() {
        contextRunner.withPropertyValues("decorator.datasource.sleuth.include: connection").run(context -> {
            DataSource dataSource = context.getBean(DataSource.class);
            ArrayListSpanReporter spanReporter = context.getBean(ArrayListSpanReporter.class);

            Connection connection = dataSource.getConnection();
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT 1 FROM dual");
            resultSet.next();
            resultSet.close();
            statement.close();
            connection.close();

            assertThat(spanReporter.getSpans()).hasSize(1);
            Span connectionSpan = spanReporter.getSpans().get(0);
            assertThat(connectionSpan.name()).isEqualTo("jdbc:/test/connection");
        });
    }

    @Test
    void testShouldIncludeOnlyQueryTraces() {
        contextRunner.withPropertyValues("decorator.datasource.sleuth.include: query").run(context -> {
            DataSource dataSource = context.getBean(DataSource.class);
            ArrayListSpanReporter spanReporter = context.getBean(ArrayListSpanReporter.class);

            Connection connection = dataSource.getConnection();
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT 1 FROM dual");
            resultSet.next();
            resultSet.close();
            statement.close();
            connection.close();

            assertThat(spanReporter.getSpans()).hasSize(1);
            Span statementSpan = spanReporter.getSpans().get(0);
            assertThat(statementSpan.name()).isEqualTo("jdbc:/test/query");
        });
    }

    @Test
    void testShouldIncludeOnlyFetchTraces() {
        contextRunner.withPropertyValues("decorator.datasource.sleuth.include: fetch").run(context -> {
            DataSource dataSource = context.getBean(DataSource.class);
            ArrayListSpanReporter spanReporter = context.getBean(ArrayListSpanReporter.class);

            Connection connection = dataSource.getConnection();
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT 1 FROM dual");
            resultSet.next();
            resultSet.close();
            statement.close();
            connection.close();

            assertThat(spanReporter.getSpans()).hasSize(1);
            Span resultSetSpan = spanReporter.getSpans().get(0);
            assertThat(resultSetSpan.name()).isEqualTo("jdbc:/test/fetch");
        });
    }

    @Test
    void testShouldIncludeOnlyConnectionAndQueryTraces() {
        contextRunner.withPropertyValues("decorator.datasource.sleuth.include: connection, query").run(context -> {
            DataSource dataSource = context.getBean(DataSource.class);
            ArrayListSpanReporter spanReporter = context.getBean(ArrayListSpanReporter.class);

            Connection connection = dataSource.getConnection();
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT 1 FROM dual");
            resultSet.next();
            resultSet.close();
            statement.close();
            connection.close();

            assertThat(spanReporter.getSpans()).hasSize(2);
            Span connectionSpan = spanReporter.getSpans().get(1);
            Span statementSpan = spanReporter.getSpans().get(0);
            assertThat(connectionSpan.name()).isEqualTo("jdbc:/test/connection");
            assertThat(statementSpan.name()).isEqualTo("jdbc:/test/query");
        });
    }

    @Test
    void testShouldIncludeOnlyConnectionAndFetchTraces() {
        contextRunner.withPropertyValues("decorator.datasource.sleuth.include: connection, fetch").run(context -> {
            DataSource dataSource = context.getBean(DataSource.class);
            ArrayListSpanReporter spanReporter = context.getBean(ArrayListSpanReporter.class);

            Connection connection = dataSource.getConnection();
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT 1 FROM dual");
            resultSet.next();
            resultSet.close();
            statement.close();
            connection.close();

            assertThat(spanReporter.getSpans()).hasSize(2);
            Span connectionSpan = spanReporter.getSpans().get(1);
            Span resultSetSpan = spanReporter.getSpans().get(0);
            assertThat(connectionSpan.name()).isEqualTo("jdbc:/test/connection");
            assertThat(resultSetSpan.name()).isEqualTo("jdbc:/test/fetch");
        });
    }

    @Test
    void testShouldIncludeOnlyQueryAndFetchTraces() {
        contextRunner.withPropertyValues("decorator.datasource.sleuth.include: query, fetch").run(context -> {
            DataSource dataSource = context.getBean(DataSource.class);
            ArrayListSpanReporter spanReporter = context.getBean(ArrayListSpanReporter.class);

            Connection connection = dataSource.getConnection();
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT 1 FROM dual");
            resultSet.next();
            resultSet.close();
            statement.close();
            connection.close();

            assertThat(spanReporter.getSpans()).hasSize(2);
            Span resultSetSpan = spanReporter.getSpans().get(1);
            Span statementSpan = spanReporter.getSpans().get(0);
            assertThat(statementSpan.name()).isEqualTo("jdbc:/test/query");
            assertThat(resultSetSpan.name()).isEqualTo("jdbc:/test/fetch");
        });
    }

    @Test
    void testShouldNotOverrideExceptionWhenConnectionWasClosedBeforeExecutingQuery() {
        contextRunner.run(context -> {
            DataSource dataSource = context.getBean(DataSource.class);
            ArrayListSpanReporter spanReporter = context.getBean(ArrayListSpanReporter.class);

            Connection connection = dataSource.getConnection();
            PreparedStatement statement = connection.prepareStatement("SELECT NOW()");
            connection.close();
            try {
                statement.executeQuery();
                fail("should throw SQLException");
            }
            catch (SQLException expected) {
            }

            assertThat(spanReporter.getSpans()).hasSize(1);
            Span connectionSpan = spanReporter.getSpans().get(0);
            assertThat(connectionSpan.name()).isEqualTo("jdbc:/test/connection");
        });
    }

    @Test
    void testShouldNotOverrideExceptionWhenStatementWasClosedBeforeExecutingQuery() {
        contextRunner.run(context -> {
            DataSource dataSource = context.getBean(DataSource.class);
            ArrayListSpanReporter spanReporter = context.getBean(ArrayListSpanReporter.class);

            Connection connection = dataSource.getConnection();
            PreparedStatement statement = connection.prepareStatement("SELECT NOW()");
            statement.close();
            try {
                statement.executeQuery();
                fail("should throw SQLException");
            }
            catch (SQLException expected) {
            }
            connection.close();

            assertThat(spanReporter.getSpans()).hasSize(2);
            Span connectionSpan = spanReporter.getSpans().get(1);
            Span statementSpan = spanReporter.getSpans().get(0);
            assertThat(connectionSpan.name()).isEqualTo("jdbc:/test/connection");
            assertThat(statementSpan.name()).isEqualTo("jdbc:/test/query");
        });
    }

    @Test
    void testShouldNotOverrideExceptionWhenResultSetWasClosedBeforeNext() {
        contextRunner.run(context -> {
            DataSource dataSource = context.getBean(DataSource.class);
            ArrayListSpanReporter spanReporter = context.getBean(ArrayListSpanReporter.class);

            Connection connection = dataSource.getConnection();
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT NOW()");
            resultSet.close();
            try {
                resultSet.next();
                fail("should throw SQLException");
            }
            catch (SQLException expected) {
            }
            statement.close();
            connection.close();

            assertThat(spanReporter.getSpans()).hasSize(3);
            Span connectionSpan = spanReporter.getSpans().get(2);
            Span resultSetSpan = spanReporter.getSpans().get(1);
            Span statementSpan = spanReporter.getSpans().get(0);
            assertThat(connectionSpan.name()).isEqualTo("jdbc:/test/connection");
            assertThat(statementSpan.name()).isEqualTo("jdbc:/test/query");
            assertThat(resultSetSpan.name()).isEqualTo("jdbc:/test/fetch");
            assertThat(statementSpan.tags()).containsEntry(SleuthListenerAutoConfiguration.SPAN_SQL_QUERY_TAG_NAME, "SELECT NOW()");
        });
    }
}
