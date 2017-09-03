package com.github.gavlyukovskiy.cloud.sleuth;

import com.github.gavlyukovskiy.boot.jdbc.decorator.DataSourceDecoratorAutoConfiguration;
import com.github.gavlyukovskiy.boot.jdbc.decorator.HidePackagesClassLoader;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

public class TracingQueryExecutionListenerTests {

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
        context.setClassLoader(new HidePackagesClassLoader("com.vladmihalcea.flexypool", "com.p6spy"));
        context.register(DataSourceAutoConfiguration.class,
                DataSourceDecoratorAutoConfiguration.class,
                SavingSpanReporterConfiguration.class,
                TraceAutoConfiguration.class,
                SleuthLogAutoConfiguration.class,
                SleuthListenerAutoConfiguration.class,
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
    public void testShouldAddSpanForPreparedStatementExecute() throws Exception {
        Connection connection = dataSource.getConnection();
        connection.prepareStatement("SELECT NOW()").execute();
        connection.close();

        assertThat(ExceptionUtils.getLastException()).isNull();

        assertThat(spanReporter.getSpans()).hasSize(1);
        Span statementSpan = spanReporter.getSpans().get(0);
        assertThat(statementSpan.getName()).isEqualTo("jdbc:/dataSource/query");
        assertThat(statementSpan.tags()).containsEntry(SleuthListenerConfiguration.SPAN_SQL_QUERY_TAG_NAME, "SELECT NOW()");
    }

    @Test
    public void testShouldAddSpanForPreparedStatementExecuteUpdate() throws Exception {
        Connection connection = dataSource.getConnection();
        connection.prepareStatement("UPDATE INFORMATION_SCHEMA.TABLES SET table_Name = '' WHERE 0 = 1").executeUpdate();
        connection.close();

        assertThat(ExceptionUtils.getLastException()).isNull();

        assertThat(spanReporter.getSpans()).hasSize(1);
        Span statementSpan = spanReporter.getSpans().get(0);
        assertThat(statementSpan.getName()).isEqualTo("jdbc:/dataSource/query");
        assertThat(statementSpan.tags()).containsEntry(SleuthListenerConfiguration.SPAN_SQL_QUERY_TAG_NAME, "UPDATE INFORMATION_SCHEMA.TABLES SET table_Name = '' WHERE 0 = 1");
        assertThat(statementSpan.tags()).containsEntry(SleuthListenerConfiguration.SPAN_ROW_COUNT_TAG_NAME, "0");
    }

    @Test
    public void testShouldAddSpanForPreparedStatementExecuteQueryIncludingTimeToCloseResultSet() throws Exception {
        Connection connection = dataSource.getConnection();
        ResultSet resultSet = connection.prepareStatement("SELECT NOW()").executeQuery();
        Thread.sleep(200L);
        resultSet.close();
        connection.close();

        assertThat(ExceptionUtils.getLastException()).isNull();

        assertThat(spanReporter.getSpans()).hasSize(1);
        Span statementSpan = spanReporter.getSpans().get(0);
        assertThat(statementSpan.getName()).isEqualTo("jdbc:/dataSource/query");
        assertThat(statementSpan.tags()).containsEntry(SleuthListenerConfiguration.SPAN_SQL_QUERY_TAG_NAME, "SELECT NOW()");
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
