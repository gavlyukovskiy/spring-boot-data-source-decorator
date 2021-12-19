package com.github.gavlyukovskiy.boot.jdbc.decorator;

import com.github.gavlyukovskiy.cloud.sleuth.SleuthListenerAutoConfiguration;
import com.github.gavlyukovskiy.cloud.sleuth.TracingJdbcEventListener;
import com.github.gavlyukovskiy.cloud.sleuth.TracingQueryExecutionListener;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.sleuth.autoconfig.brave.BraveAutoConfiguration;
import org.springframework.cloud.sleuth.autoconfig.instrument.jdbc.TraceJdbcAutoConfiguration;
import org.springframework.cloud.sleuth.instrument.jdbc.DataSourceWrapper;

import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("deprecation")
public class TraceJdbcAutoConfigurationCompatibilityTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withPropertyValues(
                    "spring.sql.init.mode=never",
                    "spring.datasource.url:jdbc:h2:mem:testdb-" + ThreadLocalRandom.current().nextInt()
            );

    private AutoConfigurations sleuth3_1_0() {
        return AutoConfigurations.of(
                DataSourceAutoConfiguration.class,
                TraceJdbcAutoConfiguration.class,
                DataSourceDecoratorAutoConfiguration.class,
                BraveAutoConfiguration.class,
                SleuthListenerAutoConfiguration.class,
                PropertyPlaceholderAutoConfiguration.class
        );
    }

    private AutoConfigurations sleuth3_0_0() {
        return AutoConfigurations.of(
                DataSourceAutoConfiguration.class,
                DataSourceDecoratorAutoConfiguration.class,
                BraveAutoConfiguration.class,
                SleuthListenerAutoConfiguration.class,
                PropertyPlaceholderAutoConfiguration.class
        );
    }

    @Test
    void shouldDelegateDecorationToSleuthWhenDoesNotUseFlexyPool() {
        ApplicationContextRunner contextRunner = this.contextRunner
                .withConfiguration(sleuth3_1_0())
                .withClassLoader(new HidePackagesClassLoader("com.vladmihalcea.flexypool"));

        contextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(DecoratedDataSource.class);
            assertThat(context).hasSingleBean(DataSourceWrapper.class);
            DataSourceWrapper sleuthDataSourceWrapper = context.getBean(DataSourceWrapper.class);
            assertThat(sleuthDataSourceWrapper.getOriginalDataSource()).isInstanceOf(HikariDataSource.class);
        });
    }

    @Test
    void shouldDecorateWhenSleuthJdbcIsNotAvailable() {
        ApplicationContextRunner contextRunner = this.contextRunner
                .withConfiguration(sleuth3_0_0())
                .withClassLoader(new HidePackagesClassLoader("com.vladmihalcea.flexypool"));

        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(DecoratedDataSource.class);
            assertThat(context).doesNotHaveBean(DataSourceWrapper.class);
        });
    }

    @Test
    void shouldDecorateWhenSleuthJdbcIsDisabled() {
        ApplicationContextRunner contextRunner = this.contextRunner
                .withConfiguration(sleuth3_1_0())
                .withPropertyValues("spring.sleuth.jdbc.enabled=false")
                .withClassLoader(new HidePackagesClassLoader("com.vladmihalcea.flexypool"));

        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(DecoratedDataSource.class);
            assertThat(context).doesNotHaveBean(DataSourceWrapper.class);
        });
    }

    @Test
    void shouldAdditionallyDecorateWhenFlexyPoolIsAvailable() {
        ApplicationContextRunner contextRunner = this.contextRunner
                .withConfiguration(sleuth3_1_0());

        /*
        Special case when FlexyPool is available it must be the first decorator on the actual data source pool,
        because it makes pool size adjustments using specific api.
        With Spring Cloud Sleuth the chain will look like this:

        DataSourceWrapper -> [P6SpyDataSource | ProxyDataSource] -> DecoratedDataSource -> FlexyPoolDataSource -> HikariDataSource
         */
        contextRunner.run(context -> {
            DataSourceWrapper sleuthDataSourceWrapper = context.getBean(DataSourceWrapper.class);
            assertThat(sleuthDataSourceWrapper.getOriginalDataSource()).isInstanceOf(DecoratedDataSource.class);
            DecoratedDataSource decoratedDataSource = (DecoratedDataSource) sleuthDataSourceWrapper.getOriginalDataSource();
            assertThat(decoratedDataSource.getRealDataSource()).isInstanceOf(HikariDataSource.class);
        });
    }

    @Test
    void shouldDelegateP6SpyTracingToSleuth() {
        ApplicationContextRunner contextRunner = this.contextRunner
                .withConfiguration(sleuth3_1_0())
                .withClassLoader(new HidePackagesClassLoader("com.vladmihalcea.flexypool", "net.ttddyy.dsproxy"));

        contextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(TracingJdbcEventListener.class);
            assertThat(context).doesNotHaveBean(TracingQueryExecutionListener.class);
        });
    }

    @Test
    void shouldRegisterP6SpyTracingWhenSleuthJdbcIsNotAvailable() {
        ApplicationContextRunner contextRunner = this.contextRunner
                .withConfiguration(sleuth3_0_0())
                .withClassLoader(new HidePackagesClassLoader("com.vladmihalcea.flexypool", "net.ttddyy.dsproxy"));

        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(TracingJdbcEventListener.class);
            assertThat(context).doesNotHaveBean(TracingQueryExecutionListener.class);
        });
    }

    @Test
    void shouldRegisterP6SpyTracingWhenSleuthJdbcIsDisabled() {
        ApplicationContextRunner contextRunner = this.contextRunner
                .withConfiguration(sleuth3_1_0())
                .withPropertyValues("spring.sleuth.jdbc.enabled=false")
                .withClassLoader(new HidePackagesClassLoader("com.vladmihalcea.flexypool", "net.ttddyy.dsproxy"));

        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(TracingJdbcEventListener.class);
            assertThat(context).doesNotHaveBean(TracingQueryExecutionListener.class);
        });
    }

    @Test
    void shouldDelegateDatasourceProxyTracingToSleuth() {
        ApplicationContextRunner contextRunner = this.contextRunner
                .withConfiguration(sleuth3_1_0())
                .withClassLoader(new HidePackagesClassLoader("com.vladmihalcea.flexypool", "com.p6spy"));

        contextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(TracingJdbcEventListener.class);
            assertThat(context).doesNotHaveBean(TracingQueryExecutionListener.class);
        });
    }

    @Test
    void shouldRegisterDatasourceProxyTracingWhenSleuthJdbcIsNotAvailable() {
        ApplicationContextRunner contextRunner = this.contextRunner
                .withConfiguration(sleuth3_0_0())
                .withClassLoader(new HidePackagesClassLoader("com.vladmihalcea.flexypool", "com.p6spy"));

        contextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(TracingJdbcEventListener.class);
            assertThat(context).hasSingleBean(TracingQueryExecutionListener.class);
        });
    }

    @Test
    void shouldRegisterDatasourceProxyTracingWhenSleuthJdbcIsDisabled() {
        ApplicationContextRunner contextRunner = this.contextRunner
                .withConfiguration(sleuth3_1_0())
                .withPropertyValues("spring.sleuth.jdbc.enabled=false")
                .withClassLoader(new HidePackagesClassLoader("com.vladmihalcea.flexypool", "com.p6spy"));

        contextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(TracingJdbcEventListener.class);
            assertThat(context).hasSingleBean(TracingQueryExecutionListener.class);
        });
    }
}
