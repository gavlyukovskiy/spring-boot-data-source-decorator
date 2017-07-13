package com.github.gavlyukovskiy.boot.jdbc.decorator.flexypool;

import com.vladmihalcea.flexypool.config.Configuration;

import javax.sql.DataSource;

/**
 * Customizer for each {@link Configuration.Builder} when real {@link DataSource} is decorated using {@link FlexyPoolDataSourceDecorator}.
 *
 * @author Arthur Gavlyukovskiy
 * @since 1.1
 */
public interface FlexyPoolConfigurationBuilderCustomizer {

    void customize(String beanName, Configuration.Builder<?> builder, Class<?> dataSourceClass);
}
