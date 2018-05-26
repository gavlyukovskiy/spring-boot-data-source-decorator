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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.cloud.sleuth.Sampler;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.SpanReporter;
import org.springframework.cloud.sleuth.autoconfig.TraceAutoConfiguration;
import org.springframework.cloud.sleuth.log.SleuthLogAutoConfiguration;
import org.springframework.cloud.sleuth.sampler.AlwaysSampler;
import org.springframework.cloud.sleuth.util.ExceptionUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class TracingJdbcEventListenerTests {

    private final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    private String dbUrl;

    private DataSource dataSource;
    private CollectingSpanReporter spanReporter;

    @Before
    public void init() {
        dbUrl = "h2:mem:testdb-" + new Random().nextInt();
        EnvironmentTestUtils.addEnvironment(context,
                "spring.datasource.initialize:false",
                "spring.datasource.url:jdbc:" + dbUrl);
        context.setClassLoader(new HidePackagesClassLoader("com.vladmihalcea.flexypool", "net.ttddyy.dsproxy"));
        context.register(DataSourceAutoConfiguration.class,
                DataSourceDecoratorAutoConfiguration.class,
                TraceAutoConfiguration.class,
                SleuthLogAutoConfiguration.class,
                SleuthListenerAutoConfiguration.class,
                SavingSpanReporterConfiguration.class,
                PropertyPlaceholderAutoConfiguration.class);
        context.refresh();

        dataSource = context.getBean(DataSource.class);
        spanReporter = context.getBean(CollectingSpanReporter.class);
    }

    @After
    public void restore() {
        context.close();
    }

    @Test
    public void testShouldAddSpanForConnection() throws Exception {
        Connection connection = dataSource.getConnection();
        connection.commit();
        connection.rollback();
        connection.close();

        assertThat(ExceptionUtils.getLastException()).isNull();

        assertThat(spanReporter.getSpans()).hasSize(1);
        Span connectionSpan = spanReporter.getSpans().get(0);
        assertThat(connectionSpan.getName()).isEqualTo("jdbc:/dataSource/connection");
        assertThat(connectionSpan.logs()).extracting("event").contains("commit");
        assertThat(connectionSpan.logs()).extracting("event").contains("rollback");
    }

    @Test
    public void testShouldAddSpanForPrepatedStatementExecute() throws Exception {
        Connection connection = dataSource.getConnection();
        connection.prepareStatement("SELECT NOW()").execute();
        connection.close();

        assertThat(ExceptionUtils.getLastException()).isNull();

        assertThat(spanReporter.getSpans()).hasSize(2);
        Span connectionSpan = spanReporter.getSpans().get(0);
        Span statementSpan = spanReporter.getSpans().get(1);
        assertThat(connectionSpan.getName()).isEqualTo("jdbc:/dataSource/connection");
        assertThat(statementSpan.getName()).isEqualTo("jdbc:/dataSource/query");
        assertThat(statementSpan.tags()).containsEntry(SleuthListenerAutoConfiguration.SPAN_SQL_QUERY_TAG_NAME, "SELECT NOW()");
    }

    @Test
    public void testShouldAddSpanForPrepatedStatementExecuteUpdate() throws Exception {
        Connection connection = dataSource.getConnection();
        connection.prepareStatement("UPDATE INFORMATION_SCHEMA.TABLES SET table_Name = '' WHERE 0 = 1").executeUpdate();
        connection.close();

        assertThat(ExceptionUtils.getLastException()).isNull();

        assertThat(spanReporter.getSpans()).hasSize(2);
        Span connectionSpan = spanReporter.getSpans().get(0);
        Span statementSpan = spanReporter.getSpans().get(1);
        assertThat(connectionSpan.getName()).isEqualTo("jdbc:/dataSource/connection");
        assertThat(statementSpan.getName()).isEqualTo("jdbc:/dataSource/query");
        assertThat(statementSpan.tags()).containsEntry(SleuthListenerAutoConfiguration.SPAN_SQL_QUERY_TAG_NAME, "UPDATE INFORMATION_SCHEMA.TABLES SET table_Name = '' WHERE 0 = 1");
        assertThat(statementSpan.tags()).containsEntry(SleuthListenerAutoConfiguration.SPAN_ROW_COUNT_TAG_NAME, "0");
    }

    @Test
    public void testShouldAddSpanForStatementExecuteUpdate() throws Exception {
        Connection connection = dataSource.getConnection();
        connection.createStatement().executeUpdate("UPDATE INFORMATION_SCHEMA.TABLES SET table_Name = '' WHERE 0 = 1");
        connection.close();

        assertThat(ExceptionUtils.getLastException()).isNull();

        assertThat(spanReporter.getSpans()).hasSize(2);
        Span connectionSpan = spanReporter.getSpans().get(0);
        Span statementSpan = spanReporter.getSpans().get(1);
        assertThat(connectionSpan.getName()).isEqualTo("jdbc:/dataSource/connection");
        assertThat(statementSpan.getName()).isEqualTo("jdbc:/dataSource/query");
        assertThat(statementSpan.tags()).containsEntry(SleuthListenerAutoConfiguration.SPAN_SQL_QUERY_TAG_NAME, "UPDATE INFORMATION_SCHEMA.TABLES SET table_Name = '' WHERE 0 = 1");
        assertThat(statementSpan.tags()).containsEntry(SleuthListenerAutoConfiguration.SPAN_ROW_COUNT_TAG_NAME, "0");
    }

    @Test
    public void testShouldAddSpanForPreparedStatementExecuteQueryIncludingTimeToCloseResultSet() throws Exception {
        Connection connection = dataSource.getConnection();
        ResultSet resultSet = connection.prepareStatement("SELECT NOW()").executeQuery();
        resultSet.next();
        resultSet.next();
        resultSet.close();
        connection.close();

        assertThat(ExceptionUtils.getLastException()).isNull();

        assertThat(spanReporter.getSpans()).hasSize(3);
        Span connectionSpan = spanReporter.getSpans().get(0);
        Span resultSetSpan = spanReporter.getSpans().get(1);
        Span statementSpan = spanReporter.getSpans().get(2);
        assertThat(connectionSpan.getName()).isEqualTo("jdbc:/dataSource/connection");
        assertThat(statementSpan.getName()).isEqualTo("jdbc:/dataSource/query");
        assertThat(resultSetSpan.getName()).isEqualTo("jdbc:/dataSource/fetch");
        assertThat(statementSpan.tags()).containsEntry(SleuthListenerAutoConfiguration.SPAN_SQL_QUERY_TAG_NAME, "SELECT NOW()");
        assertThat(resultSetSpan.tags()).containsEntry(SleuthListenerAutoConfiguration.SPAN_ROW_COUNT_TAG_NAME, "1");
    }

    @Test
    public void testShouldAddSpanForStatementAndResultSet() throws Exception {
        Connection connection = dataSource.getConnection();
        ResultSet resultSet = connection.createStatement().executeQuery("SELECT NOW()");
        resultSet.next();
        Thread.sleep(200L);
        resultSet.close();
        connection.close();

        assertThat(ExceptionUtils.getLastException()).isNull();

        assertThat(spanReporter.getSpans()).hasSize(3);
        Span connectionSpan = spanReporter.getSpans().get(0);
        Span resultSetSpan = spanReporter.getSpans().get(1);
        Span statementSpan = spanReporter.getSpans().get(2);
        assertThat(connectionSpan.getName()).isEqualTo("jdbc:/dataSource/connection");
        assertThat(statementSpan.getName()).isEqualTo("jdbc:/dataSource/query");
        assertThat(resultSetSpan.getName()).isEqualTo("jdbc:/dataSource/fetch");
        assertThat(statementSpan.tags()).containsEntry(SleuthListenerAutoConfiguration.SPAN_SQL_QUERY_TAG_NAME, "SELECT NOW()");
        assertThat(resultSetSpan.tags()).containsEntry(SleuthListenerAutoConfiguration.SPAN_ROW_COUNT_TAG_NAME, "1");
    }

    @Test
    public void testShouldNotFailWhenStatementIsClosedWihoutResultSet() throws Exception {
        Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery("SELECT NOW()");
        resultSet.next();
        statement.close();
        connection.close();

        assertThat(ExceptionUtils.getLastException()).isNull();

        assertThat(spanReporter.getSpans()).hasSize(3);
        Span connectionSpan = spanReporter.getSpans().get(0);
        Span resultSetSpan = spanReporter.getSpans().get(1);
        Span statementSpan = spanReporter.getSpans().get(2);
        assertThat(connectionSpan.getName()).isEqualTo("jdbc:/dataSource/connection");
        assertThat(statementSpan.getName()).isEqualTo("jdbc:/dataSource/query");
        assertThat(resultSetSpan.getName()).isEqualTo("jdbc:/dataSource/fetch");
        assertThat(statementSpan.tags()).containsEntry(SleuthListenerAutoConfiguration.SPAN_SQL_QUERY_TAG_NAME, "SELECT NOW()");
    }

    @Test
    public void testShouldNotFailWhenConnectionIsClosedWihoutResultSet() throws Exception {
        Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery("SELECT NOW()");
        resultSet.next();
        connection.close();

        assertThat(ExceptionUtils.getLastException()).isNull();

        assertThat(spanReporter.getSpans()).hasSize(3);
        Span connectionSpan = spanReporter.getSpans().get(0);
        Span resultSetSpan = spanReporter.getSpans().get(1);
        Span statementSpan = spanReporter.getSpans().get(2);
        assertThat(connectionSpan.getName()).isEqualTo("jdbc:/dataSource/connection");
        assertThat(statementSpan.getName()).isEqualTo("jdbc:/dataSource/query");
        assertThat(resultSetSpan.getName()).isEqualTo("jdbc:/dataSource/fetch");
        assertThat(statementSpan.tags()).containsEntry(SleuthListenerAutoConfiguration.SPAN_SQL_QUERY_TAG_NAME, "SELECT NOW()");
    }

    @Test
    public void testShouldNotFailWhenResultSetNextWasNotCalled() throws Exception {
        Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery("SELECT NOW()");
        resultSet.close();
        statement.close();
        connection.close();

        assertThat(ExceptionUtils.getLastException()).isNull();

        assertThat(spanReporter.getSpans()).hasSize(3);
        Span connectionSpan = spanReporter.getSpans().get(0);
        Span resultSetSpan = spanReporter.getSpans().get(1);
        Span statementSpan = spanReporter.getSpans().get(2);
        assertThat(connectionSpan.getName()).isEqualTo("jdbc:/dataSource/connection");
        assertThat(statementSpan.getName()).isEqualTo("jdbc:/dataSource/query");
        assertThat(resultSetSpan.getName()).isEqualTo("jdbc:/dataSource/fetch");
        assertThat(statementSpan.tags()).containsEntry(SleuthListenerAutoConfiguration.SPAN_SQL_QUERY_TAG_NAME, "SELECT NOW()");
    }

    @Test
    public void testShouldNotFailWhenResourceIsAlreadyClosed() throws Exception {
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

        assertThat(spanReporter.getSpans()).hasSize(3);
        Span connectionSpan = spanReporter.getSpans().get(0);
        Span resultSetSpan = spanReporter.getSpans().get(1);
        Span statementSpan = spanReporter.getSpans().get(2);
        assertThat(connectionSpan.getName()).isEqualTo("jdbc:/dataSource/connection");
        assertThat(statementSpan.getName()).isEqualTo("jdbc:/dataSource/query");
        assertThat(resultSetSpan.getName()).isEqualTo("jdbc:/dataSource/fetch");
        assertThat(statementSpan.tags()).containsEntry(SleuthListenerAutoConfiguration.SPAN_SQL_QUERY_TAG_NAME, "SELECT NOW()");
    }

    @Test
    public void testShouldNotFailWhenResourceIsAlreadyClosed2() throws Exception {
        Connection connection = dataSource.getConnection();
        try {
            connection.close();
            connection.prepareStatement("SELECT NOW()");
            fail("should fail due to closed connection");
        }
        catch (SQLException expected) {
        }

        assertThat(ExceptionUtils.getLastException()).isNull();

        assertThat(spanReporter.getSpans()).hasSize(1);
        Span connectionSpan = spanReporter.getSpans().get(0);
        assertThat(connectionSpan.getName()).isEqualTo("jdbc:/dataSource/connection");
    }

    @Test
    public void testShouldNotFailToCloseSpanForTwoConsecutiveConnections() throws Exception {
        Connection connection1 = dataSource.getConnection();
        Connection connection2 = dataSource.getConnection();
        connection1.close();
        connection2.close();

        assertThat(ExceptionUtils.getLastException()).isNull();

        assertThat(spanReporter.getSpans()).hasSize(2);
        Span connectionSpan = spanReporter.getSpans().get(0);
        Span statementSpan = spanReporter.getSpans().get(1);
        assertThat(connectionSpan.getName()).isEqualTo("jdbc:/dataSource/connection");
        assertThat(statementSpan.getName()).isEqualTo("jdbc:/dataSource/connection");
    }

    @Configuration
    static class SavingSpanReporterConfiguration {

        @Bean
        public CollectingSpanReporter spanReporter() {
            return new CollectingSpanReporter();
        }

        @Bean
        public Sampler sampler() {
            return new AlwaysSampler();
        }
    }

    static class CollectingSpanReporter implements SpanReporter {
        private List<Span> spans = new ArrayList<>();
        @Override
        public void report(Span span) {
            spans.add(0, span);
        }

        public List<Span> getSpans() {
            return spans;
        }
    }
}
