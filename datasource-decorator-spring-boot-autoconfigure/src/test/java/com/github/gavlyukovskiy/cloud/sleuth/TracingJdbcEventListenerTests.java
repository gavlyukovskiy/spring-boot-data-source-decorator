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
import com.github.gavlyukovskiy.boot.jdbc.decorator.DataSourceDecoratorAutoConfiguration;
import com.github.gavlyukovskiy.boot.jdbc.decorator.HidePackagesClassLoader;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.sleuth.autoconfig.brave.BraveAutoConfiguration;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;

class TracingJdbcEventListenerTests extends TracingListenerStrategyTests{

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    DataSourceAutoConfiguration.class,
                    DataSourceDecoratorAutoConfiguration.class,
                    BraveAutoConfiguration.class,
                    SleuthListenerAutoConfiguration.class,
                    TestSpanHandlerConfiguration.class,
                    PropertyPlaceholderAutoConfiguration.class
            ))
            .withPropertyValues("spring.datasource.initialization-mode=never",
                    "spring.datasource.url:jdbc:h2:mem:testdb-" + ThreadLocalRandom.current().nextInt(),
                    "spring.datasource.hikari.pool-name=test")
            .withClassLoader(new HidePackagesClassLoader("com.vladmihalcea.flexypool", "net.ttddyy.dsproxy"));

    protected TracingJdbcEventListenerTests() {
        super(new ApplicationContextRunner()
                      .withConfiguration(AutoConfigurations.of(
                              DataSourceAutoConfiguration.class,
                              DataSourceDecoratorAutoConfiguration.class,
                              BraveAutoConfiguration.class,
                              SleuthListenerAutoConfiguration.class,
                              TestSpanHandlerConfiguration.class,
                              PropertyPlaceholderAutoConfiguration.class
                      ))
                      .withPropertyValues("spring.datasource.initialization-mode=never",
                                          "spring.datasource.url:jdbc:h2:mem:testdb-" + ThreadLocalRandom.current().nextInt(),
                                          "spring.datasource.hikari.pool-name=test")
                      .withClassLoader(new HidePackagesClassLoader("com.vladmihalcea.flexypool", "net.ttddyy.dsproxy")));
    }

    @Test
    void testShouldUsePlaceholderInSqlTagOfSpansForPreparedStatementIfIncludeParameterValuesIsSetToFalse() {
        contextRunner.withPropertyValues("decorator.datasource.p6spy.tracing.include-parameter-values=false")
                .run(context -> {
                    DataSource dataSource = context.getBean(DataSource.class);
                    TestSpanHandler spanReporter = context.getBean(TestSpanHandler.class);

                    Connection connection = dataSource.getConnection();
                    PreparedStatement preparedStatement = connection.prepareStatement("UPDATE INFORMATION_SCHEMA.TABLES SET table_Name = ? WHERE 0 = ?");
                    preparedStatement.setString(1, "");
                    preparedStatement.setInt(2, 1);
                    preparedStatement.executeUpdate();
                    connection.close();

                    assertThat(spanReporter.spans()).hasSize(2);
                    MutableSpan connectionSpan = spanReporter.spans().get(1);
                    MutableSpan statementSpan = spanReporter.spans().get(0);
                    assertThat(connectionSpan.name()).isEqualTo("jdbc:/test/connection");
                    assertThat(statementSpan.name()).isEqualTo("jdbc:/test/query");
                    assertThat(statementSpan.tags()).containsEntry(SleuthListenerAutoConfiguration.SPAN_SQL_QUERY_TAG_NAME, "UPDATE INFORMATION_SCHEMA.TABLES SET table_Name = ? WHERE 0 = ?");
                    assertThat(statementSpan.tags()).containsEntry(SleuthListenerAutoConfiguration.SPAN_ROW_COUNT_TAG_NAME, "0");
                });
    }
}
