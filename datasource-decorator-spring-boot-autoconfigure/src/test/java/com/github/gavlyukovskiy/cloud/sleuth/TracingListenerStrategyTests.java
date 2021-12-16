/*
 * Copyright 2021 the original author or authors.
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

import brave.handler.MutableSpan;
import brave.test.TestSpanHandler;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SuppressWarnings("deprecation")
abstract class TracingListenerStrategyTests {

    protected final ApplicationContextRunner contextRunner;

    protected TracingListenerStrategyTests(ApplicationContextRunner contextRunner) {
        this.contextRunner = contextRunner;
    }

    @Test
    void testShouldAddSpanForConnection() {
        contextRunner.run(context -> {
            DataSource dataSource = context.getBean(DataSource.class);
            TestSpanHandler spanReporter = context.getBean(TestSpanHandler.class);

            Connection connection = dataSource.getConnection();
            connection.commit();
            connection.rollback();
            connection.close();

            assertThat(spanReporter.spans()).hasSize(1);
            MutableSpan connectionSpan = spanReporter.spans().get(0);
            assertThat(connectionSpan.name()).isEqualTo("jdbc:/test/connection");
            assertThat(connectionSpan.remoteServiceName()).isEqualTo("test");
            assertThat(connectionSpan.annotations()).extracting("value").contains("commit");
            assertThat(connectionSpan.annotations()).extracting("value").contains("rollback");
        });
    }

    @Test
    void testShouldAddSpanForPreparedStatementExecute() {
        contextRunner.run(context -> {
            DataSource dataSource = context.getBean(DataSource.class);
            TestSpanHandler spanReporter = context.getBean(TestSpanHandler.class);

            Connection connection = dataSource.getConnection();
            connection.prepareStatement("SELECT NOW()").execute();
            connection.close();

            assertThat(spanReporter.spans()).hasSize(2);
            MutableSpan connectionSpan = spanReporter.spans().get(1);
            MutableSpan statementSpan = spanReporter.spans().get(0);
            assertThat(connectionSpan.name()).isEqualTo("jdbc:/test/connection");
            assertThat(statementSpan.name()).isEqualTo("jdbc:/test/query");
            assertThat(statementSpan.remoteServiceName()).isEqualTo("test");
            assertThat(statementSpan.tags()).containsEntry(SleuthListenerAutoConfiguration.SPAN_SQL_QUERY_TAG_NAME, "SELECT NOW()");
        });
    }

    @Test
    void testShouldAddSpanForPreparedStatementExecuteUpdate() {
        contextRunner.run(context -> {
            DataSource dataSource = context.getBean(DataSource.class);
            TestSpanHandler spanReporter = context.getBean(TestSpanHandler.class);

            Connection connection = dataSource.getConnection();
            connection.prepareStatement("UPDATE INFORMATION_SCHEMA.TABLES SET table_Name = '' WHERE 0 = 1").executeUpdate();
            connection.close();

            assertThat(spanReporter.spans()).hasSize(2);
            MutableSpan connectionSpan = spanReporter.spans().get(1);
            MutableSpan statementSpan = spanReporter.spans().get(0);
            assertThat(connectionSpan.name()).isEqualTo("jdbc:/test/connection");
            assertThat(statementSpan.name()).isEqualTo("jdbc:/test/query");
            assertThat(statementSpan.tags()).containsEntry(SleuthListenerAutoConfiguration.SPAN_SQL_QUERY_TAG_NAME,
                    "UPDATE INFORMATION_SCHEMA.TABLES SET table_Name = '' WHERE 0 = 1");
            assertThat(statementSpan.tags()).containsEntry(SleuthListenerAutoConfiguration.SPAN_ROW_COUNT_TAG_NAME, "0");
        });
    }

    @Test
    void testShouldAddSpanForStatementExecuteUpdate() {
        contextRunner.run(context -> {
            DataSource dataSource = context.getBean(DataSource.class);
            TestSpanHandler spanReporter = context.getBean(TestSpanHandler.class);

            Connection connection = dataSource.getConnection();
            connection.createStatement().executeUpdate("UPDATE INFORMATION_SCHEMA.TABLES SET table_Name = '' WHERE 0 = 1");
            connection.close();

            assertThat(spanReporter.spans()).hasSize(2);
            MutableSpan connectionSpan = spanReporter.spans().get(1);
            MutableSpan statementSpan = spanReporter.spans().get(0);
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
            TestSpanHandler spanReporter = context.getBean(TestSpanHandler.class);

            Connection connection = dataSource.getConnection();
            ResultSet resultSet = connection.prepareStatement("SELECT NOW() UNION ALL SELECT NOW()").executeQuery();
            resultSet.next();
            resultSet.next();
            resultSet.close();
            connection.close();

            assertThat(spanReporter.spans()).hasSize(3);
            MutableSpan connectionSpan = spanReporter.spans().get(2);
            MutableSpan resultSetSpan = spanReporter.spans().get(1);
            MutableSpan statementSpan = spanReporter.spans().get(0);
            assertThat(connectionSpan.name()).isEqualTo("jdbc:/test/connection");
            assertThat(statementSpan.name()).isEqualTo("jdbc:/test/query");
            assertThat(statementSpan.tags()).containsEntry(SleuthListenerAutoConfiguration.SPAN_SQL_QUERY_TAG_NAME, "SELECT NOW() UNION ALL SELECT NOW()");
            assertThat(resultSetSpan.name()).isEqualTo("jdbc:/test/fetch");
            assertThat(resultSetSpan.remoteServiceName()).isEqualTo("test");
            if (isP6Spy(context)) {
                assertThat(resultSetSpan.tags()).containsEntry(SleuthListenerAutoConfiguration.SPAN_ROW_COUNT_TAG_NAME, "2");
            }
        });
    }

    @Test
    void testShouldAddSpanForStatementAndResultSet() {
        contextRunner.run(context -> {
            DataSource dataSource = context.getBean(DataSource.class);
            TestSpanHandler spanReporter = context.getBean(TestSpanHandler.class);

            Connection connection = dataSource.getConnection();
            ResultSet resultSet = connection.createStatement().executeQuery("SELECT NOW()");
            resultSet.next();
            Thread.sleep(200L);
            resultSet.close();
            connection.close();

            assertThat(spanReporter.spans()).hasSize(3);
            MutableSpan connectionSpan = spanReporter.spans().get(2);
            MutableSpan resultSetSpan = spanReporter.spans().get(1);
            MutableSpan statementSpan = spanReporter.spans().get(0);
            assertThat(connectionSpan.name()).isEqualTo("jdbc:/test/connection");
            assertThat(statementSpan.name()).isEqualTo("jdbc:/test/query");
            assertThat(statementSpan.tags()).containsEntry(SleuthListenerAutoConfiguration.SPAN_SQL_QUERY_TAG_NAME, "SELECT NOW()");
            assertThat(resultSetSpan.name()).isEqualTo("jdbc:/test/fetch");
            if (isP6Spy(context)) {
                assertThat(resultSetSpan.tags()).containsEntry(SleuthListenerAutoConfiguration.SPAN_ROW_COUNT_TAG_NAME, "1");
            }
        });
    }

    @Test
    void testShouldNotFailWhenStatementIsClosedWihoutResultSet() {
        contextRunner.run(context -> {
            DataSource dataSource = context.getBean(DataSource.class);
            TestSpanHandler spanReporter = context.getBean(TestSpanHandler.class);

            Connection connection = dataSource.getConnection();
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT NOW()");
            resultSet.next();
            statement.close();
            connection.close();

            assertThat(spanReporter.spans()).hasSize(3);
            MutableSpan connectionSpan = spanReporter.spans().get(2);
            MutableSpan resultSetSpan = spanReporter.spans().get(1);
            MutableSpan statementSpan = spanReporter.spans().get(0);
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
            TestSpanHandler spanReporter = context.getBean(TestSpanHandler.class);

            Connection connection = dataSource.getConnection();
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT NOW()");
            resultSet.next();
            connection.close();

            assertThat(spanReporter.spans()).hasSize(3);
            MutableSpan connectionSpan = spanReporter.spans().get(2);
            MutableSpan resultSetSpan = spanReporter.spans().get(1);
            MutableSpan statementSpan = spanReporter.spans().get(0);
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
            TestSpanHandler spanReporter = context.getBean(TestSpanHandler.class);

            Connection connection = dataSource.getConnection();
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT NOW()");
            resultSet.next();
            resultSet.close();
            statement.close();
            connection.close();

            assertThat(spanReporter.spans()).hasSize(3);
            MutableSpan connectionSpan = spanReporter.spans().get(2);
            MutableSpan resultSetSpan = spanReporter.spans().get(1);
            MutableSpan statementSpan = spanReporter.spans().get(0);
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
            TestSpanHandler spanReporter = context.getBean(TestSpanHandler.class);

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

            assertThat(spanReporter.spans()).hasSize(3);
            MutableSpan connectionSpan = spanReporter.spans().get(2);
            MutableSpan resultSetSpan = spanReporter.spans().get(1);
            MutableSpan statementSpan = spanReporter.spans().get(0);
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
            TestSpanHandler spanReporter = context.getBean(TestSpanHandler.class);

            Connection connection = dataSource.getConnection();
            assertThrows(SQLException.class, () -> {
                connection.close();
                connection.prepareStatement("SELECT NOW()");
            });

            assertThat(spanReporter.spans()).hasSize(1);
            MutableSpan connectionSpan = spanReporter.spans().get(0);
            assertThat(connectionSpan.name()).isEqualTo("jdbc:/test/connection");
        });
    }

    @Test
    void testShouldNotFailWhenResourceIsAlreadyClosed3() {
        contextRunner.run(context -> {
            DataSource dataSource = context.getBean(DataSource.class);

            Connection connection = dataSource.getConnection();
            Statement statement = connection.createStatement();
            assertThrows(SQLException.class, () -> {
                statement.close();
                statement.executeQuery("SELECT NOW()");
            });
            connection.close();
        });
    }

    @Test
    void testShouldNotFailWhenResourceIsAlreadyClosed4() {
        contextRunner.run(context -> {
            DataSource dataSource = context.getBean(DataSource.class);

            Connection connection = dataSource.getConnection();
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT NOW()");
            assertThrows(SQLException.class, () -> {
                resultSet.close();
                resultSet.next();
            });
            statement.close();
            connection.close();
        });
    }

    @Test
    void testShouldNotFailToCloseSpanForTwoConsecutiveConnections() {
        contextRunner.run(context -> {
            DataSource dataSource = context.getBean(DataSource.class);
            TestSpanHandler spanReporter = context.getBean(TestSpanHandler.class);

            Connection connection1 = dataSource.getConnection();
            Connection connection2 = dataSource.getConnection();
            connection1.close();
            connection2.close();

            assertThat(spanReporter.spans()).hasSize(2);
            MutableSpan connection1Span = spanReporter.spans().get(0);
            MutableSpan connection2Span = spanReporter.spans().get(1);
            assertThat(connection1Span.name()).isEqualTo("jdbc:/test/connection");
            assertThat(connection2Span.name()).isEqualTo("jdbc:/test/connection");
        });
    }

    @Test
    void testShouldNotFailWhenClosedInReversedOrder() {
        contextRunner.run(context -> {
            DataSource dataSource = context.getBean(DataSource.class);
            TestSpanHandler spanReporter = context.getBean(TestSpanHandler.class);

            Connection connection = dataSource.getConnection();
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT NOW()");
            resultSet.next();
            connection.close();
            statement.close();
            resultSet.close();

            assertThat(spanReporter.spans()).hasSize(3);
            MutableSpan connectionSpan = spanReporter.spans().get(2);
            MutableSpan resultSetSpan = spanReporter.spans().get(1);
            MutableSpan statementSpan = spanReporter.spans().get(0);
            assertThat(connectionSpan.name()).isEqualTo("jdbc:/test/connection");
            assertThat(statementSpan.name()).isEqualTo("jdbc:/test/query");
            assertThat(resultSetSpan.name()).isEqualTo("jdbc:/test/fetch");
            assertThat(statementSpan.tags()).containsEntry(SleuthListenerAutoConfiguration.SPAN_SQL_QUERY_TAG_NAME, "SELECT NOW()");
        });
    }

    @Test
    @SuppressWarnings("unchecked")
    void testShouldNotCauseMemoryLeakOnTomcatPool() {
        contextRunner.withPropertyValues("spring.datasource.type:org.apache.tomcat.jdbc.pool.DataSource").run(context -> {
            DataSource dataSource = context.getBean(DataSource.class);
            Object listener = isP6Spy(context)
                    ? context.getBean(TracingJdbcEventListener.class)
                    : context.getBean(TracingQueryExecutionListener.class);

            Connection connection = dataSource.getConnection();
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT 1 FROM dual");
            resultSet.next();
            resultSet.close();
            statement.close();
            connection.close();

            assertThat(listener)
                    .extracting("strategy")
                    .extracting("openConnections")
                    .isInstanceOfSatisfying(Map.class, map -> assertThat(map).isEmpty());
        });
    }

    private boolean isP6Spy(AssertableApplicationContext context) {
        if (context.getBeansOfType(TracingJdbcEventListener.class).size() == 1) {
            return true;
        } else if (context.getBeansOfType(TracingQueryExecutionListener.class).size() == 1) {
            return false;
        } else {
            throw new IllegalStateException("Expected exactly 1 tracing listener bean in the context.");
        }
    }

    @Test
    void testSingleConnectionAcrossMultipleThreads() {
        contextRunner.run(context -> {
            DataSource dataSource = context.getBean(DataSource.class);
            TestSpanHandler spanReporter = context.getBean(TestSpanHandler.class);

            Connection connection = dataSource.getConnection();
            IntStream.range(0, 5)
                    .mapToObj(i -> CompletableFuture.runAsync(() -> {
                        try {
                            Statement statement = connection.createStatement();
                            ResultSet resultSet = statement.executeQuery("SELECT NOW()");
                            resultSet.next();
                            statement.close();
                            resultSet.close();
                        } catch (SQLException e) {
                            throw new IllegalStateException(e);
                        }
                    }))
                    .collect(Collectors.toList())
                    .forEach(CompletableFuture::join);
            connection.close();

            assertThat(spanReporter.spans()).hasSize(1 + 2 * 5);
            assertThat(spanReporter.spans()).extracting("name")
                    .contains("jdbc:/test/query", "jdbc:/test/fetch", "jdbc:/test/connection");
        });
    }

    @Test
    void testShouldIncludeOnlyConnectionTraces() {
        contextRunner.withPropertyValues("decorator.datasource.sleuth.include: connection").run(context -> {
            DataSource dataSource = context.getBean(DataSource.class);
            TestSpanHandler spanReporter = context.getBean(TestSpanHandler.class);

            Connection connection = dataSource.getConnection();
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT 1 FROM dual");
            resultSet.next();
            resultSet.close();
            statement.close();
            connection.close();

            assertThat(spanReporter.spans()).hasSize(1);
            MutableSpan connectionSpan = spanReporter.spans().get(0);
            assertThat(connectionSpan.name()).isEqualTo("jdbc:/test/connection");
        });
    }

    @Test
    void testShouldIncludeOnlyQueryTraces() {
        contextRunner.withPropertyValues("decorator.datasource.sleuth.include: query").run(context -> {
            DataSource dataSource = context.getBean(DataSource.class);
            TestSpanHandler spanReporter = context.getBean(TestSpanHandler.class);

            Connection connection = dataSource.getConnection();
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT 1 FROM dual");
            resultSet.next();
            resultSet.close();
            statement.close();
            connection.close();

            assertThat(spanReporter.spans()).hasSize(1);
            MutableSpan statementSpan = spanReporter.spans().get(0);
            assertThat(statementSpan.name()).isEqualTo("jdbc:/test/query");
        });
    }

    @Test
    void testShouldIncludeOnlyFetchTraces() {
        contextRunner.withPropertyValues("decorator.datasource.sleuth.include: fetch").run(context -> {
            DataSource dataSource = context.getBean(DataSource.class);
            TestSpanHandler spanReporter = context.getBean(TestSpanHandler.class);

            Connection connection = dataSource.getConnection();
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT 1 FROM dual");
            resultSet.next();
            resultSet.close();
            statement.close();
            connection.close();

            assertThat(spanReporter.spans()).hasSize(1);
            MutableSpan resultSetSpan = spanReporter.spans().get(0);
            assertThat(resultSetSpan.name()).isEqualTo("jdbc:/test/fetch");
        });
    }

    @Test
    void testShouldIncludeOnlyConnectionAndQueryTraces() {
        contextRunner.withPropertyValues("decorator.datasource.sleuth.include: connection, query").run(context -> {
            DataSource dataSource = context.getBean(DataSource.class);
            TestSpanHandler spanReporter = context.getBean(TestSpanHandler.class);

            Connection connection = dataSource.getConnection();
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT 1 FROM dual");
            resultSet.next();
            resultSet.close();
            statement.close();
            connection.close();

            assertThat(spanReporter.spans()).hasSize(2);
            MutableSpan connectionSpan = spanReporter.spans().get(1);
            MutableSpan statementSpan = spanReporter.spans().get(0);
            assertThat(connectionSpan.name()).isEqualTo("jdbc:/test/connection");
            assertThat(statementSpan.name()).isEqualTo("jdbc:/test/query");
        });
    }

    @Test
    void testShouldIncludeOnlyConnectionAndFetchTraces() {
        contextRunner.withPropertyValues("decorator.datasource.sleuth.include: connection, fetch").run(context -> {
            DataSource dataSource = context.getBean(DataSource.class);
            TestSpanHandler spanReporter = context.getBean(TestSpanHandler.class);

            Connection connection = dataSource.getConnection();
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT 1 FROM dual");
            resultSet.next();
            resultSet.close();
            statement.close();
            connection.close();

            assertThat(spanReporter.spans()).hasSize(2);
            MutableSpan connectionSpan = spanReporter.spans().get(1);
            MutableSpan resultSetSpan = spanReporter.spans().get(0);
            assertThat(connectionSpan.name()).isEqualTo("jdbc:/test/connection");
            assertThat(resultSetSpan.name()).isEqualTo("jdbc:/test/fetch");
        });
    }

    @Test
    void testShouldIncludeOnlyQueryAndFetchTraces() {
        contextRunner.withPropertyValues("decorator.datasource.sleuth.include: query, fetch").run(context -> {
            DataSource dataSource = context.getBean(DataSource.class);
            TestSpanHandler spanReporter = context.getBean(TestSpanHandler.class);

            Connection connection = dataSource.getConnection();
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT 1 FROM dual");
            resultSet.next();
            resultSet.close();
            statement.close();
            connection.close();

            assertThat(spanReporter.spans()).hasSize(2);
            MutableSpan resultSetSpan = spanReporter.spans().get(1);
            MutableSpan statementSpan = spanReporter.spans().get(0);
            assertThat(statementSpan.name()).isEqualTo("jdbc:/test/query");
            assertThat(resultSetSpan.name()).isEqualTo("jdbc:/test/fetch");
        });
    }

    @Test
    void testShouldNotOverrideExceptionWhenConnectionWasClosedBeforeExecutingQuery() {
        contextRunner.run(context -> {
            DataSource dataSource = context.getBean(DataSource.class);
            TestSpanHandler spanReporter = context.getBean(TestSpanHandler.class);

            Connection connection = dataSource.getConnection();
            PreparedStatement statement = connection.prepareStatement("SELECT NOW()");
            connection.close();
            assertThrows(SQLException.class, statement::executeQuery);

            assertThat(spanReporter.spans()).hasSize(1);
            MutableSpan connectionSpan = spanReporter.spans().get(0);
            assertThat(connectionSpan.name()).isEqualTo("jdbc:/test/connection");
        });
    }

    @Test
    void testShouldNotOverrideExceptionWhenStatementWasClosedBeforeExecutingQuery() {
        contextRunner.run(context -> {
            DataSource dataSource = context.getBean(DataSource.class);
            TestSpanHandler spanReporter = context.getBean(TestSpanHandler.class);

            Connection connection = dataSource.getConnection();
            PreparedStatement statement = connection.prepareStatement("SELECT NOW()");
            statement.close();
            assertThrows(SQLException.class, statement::executeQuery);
            connection.close();

            assertThat(spanReporter.spans()).hasSize(2);
            MutableSpan connectionSpan = spanReporter.spans().get(1);
            MutableSpan statementSpan = spanReporter.spans().get(0);
            assertThat(connectionSpan.name()).isEqualTo("jdbc:/test/connection");
            assertThat(statementSpan.name()).isEqualTo("jdbc:/test/query");
        });
    }

    @Test
    void testShouldNotOverrideExceptionWhenResultSetWasClosedBeforeNext() {
        contextRunner.run(context -> {
            DataSource dataSource = context.getBean(DataSource.class);
            TestSpanHandler spanReporter = context.getBean(TestSpanHandler.class);

            Connection connection = dataSource.getConnection();
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT NOW()");
            resultSet.close();
            assertThrows(SQLException.class, resultSet::next);
            statement.close();
            connection.close();

            assertThat(spanReporter.spans()).hasSize(3);
            MutableSpan connectionSpan = spanReporter.spans().get(2);
            MutableSpan resultSetSpan = spanReporter.spans().get(1);
            MutableSpan statementSpan = spanReporter.spans().get(0);
            assertThat(connectionSpan.name()).isEqualTo("jdbc:/test/connection");
            assertThat(statementSpan.name()).isEqualTo("jdbc:/test/query");
            assertThat(resultSetSpan.name()).isEqualTo("jdbc:/test/fetch");
            assertThat(statementSpan.tags()).containsEntry(SleuthListenerAutoConfiguration.SPAN_SQL_QUERY_TAG_NAME, "SELECT NOW()");
        });
    }

    @Test
    void testShouldNotFailWhenClosingConnectionFromDifferentDataSource() {
        ApplicationContextRunner contextRunner = this.contextRunner.withUserConfiguration(MultiDataSourceConfiguration.class);

        contextRunner.run(context -> {
            DataSource dataSource1 = context.getBean("test1", DataSource.class);
            DataSource dataSource2 = context.getBean("test2", DataSource.class);

            dataSource1.getConnection().close();
            dataSource2.getConnection().close();

            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    Connection connection1 = dataSource1.getConnection();
                    PreparedStatement statement = connection1.prepareStatement("SELECT NOW()");
                    ResultSet resultSet = statement.executeQuery();
                    Thread.sleep(200);
                    resultSet.close();
                    statement.close();
                    connection1.close();
                } catch (SQLException | InterruptedException e) {
                    throw new IllegalStateException(e);
                }
            });
            Thread.sleep(100);
            Connection connection2 = dataSource2.getConnection();
            Thread.sleep(300);
            connection2.close();

            future.join();
        });
    }

    private static class MultiDataSourceConfiguration {
        @Bean
        public HikariDataSource test1() {
            HikariDataSource dataSource = new HikariDataSource();
            dataSource.setJdbcUrl("jdbc:h2:mem:testdb-1-" + ThreadLocalRandom.current().nextInt());
            dataSource.setPoolName("test1");
            return dataSource;
        }

        @Bean
        public HikariDataSource test2() {
            HikariDataSource dataSource = new HikariDataSource();
            dataSource.setJdbcUrl("jdbc:h2:mem:testdb-2-" + ThreadLocalRandom.current().nextInt());
            dataSource.setPoolName("test2");
            return dataSource;
        }
    }
}
