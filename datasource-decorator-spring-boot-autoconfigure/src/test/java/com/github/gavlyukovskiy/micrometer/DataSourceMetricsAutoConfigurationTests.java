package com.github.gavlyukovskiy.micrometer;

import com.github.gavlyukovskiy.boot.jdbc.decorator.DataSourceDecoratorAutoConfiguration;
import com.github.gavlyukovskiy.boot.jdbc.decorator.HidePackagesClassLoader;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.simple.SimpleMetricsExportAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class DataSourceMetricsAutoConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    MetricsAutoConfiguration.class,
                    SimpleMetricsExportAutoConfiguration.class,
                    DataSourceAutoConfiguration.class,
                    DataSourceDecoratorAutoConfiguration.class,
                    DataSourceMetricsAutoConfiguration.class,
                    PropertyPlaceholderAutoConfiguration.class
            ))
            .withPropertyValues("spring.datasource.initialization-mode=never",
                    "spring.datasource.url:jdbc:h2:mem:testdb-" + new Random().nextInt(),
                    "spring.datasource.hikari.pool-name=test")
            .withClassLoader(new HidePackagesClassLoader("com.vladmihalcea.flexypool", "com.p6spy", "net.ttddyy.dsproxy"));

    @Test
    void shouldNotUseMetricsIfHikariDataSourceIsPresent() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(MeterRegistry.class);
            assertThat(context).doesNotHaveBean(DataSourceMetricsAutoConfiguration.class);
        });
    }
}