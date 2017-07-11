package com.github.gavlyukovskiy.boot.jdbc.decorator.flexypool;

import com.vladmihalcea.flexypool.config.Configuration;

public interface FlexyPoolConfigurationBuilderCustomizer {

    void customize(String beanName, Configuration.Builder<?> builder, Class<?> dataSourceClass);
}
