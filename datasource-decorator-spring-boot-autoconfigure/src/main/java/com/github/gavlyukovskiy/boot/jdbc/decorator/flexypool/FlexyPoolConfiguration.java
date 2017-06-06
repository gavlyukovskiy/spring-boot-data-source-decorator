package com.github.gavlyukovskiy.boot.jdbc.decorator.flexypool;

import com.github.gavlyukovskiy.boot.jdbc.decorator.DataSourceDecorator;
import com.vladmihalcea.flexypool.FlexyPoolDataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;

@ConditionalOnClass(FlexyPoolDataSource.class)
public class FlexyPoolConfiguration {

    @Bean
    public DataSourceDecorator flexyPoolDataSourceDecorator() {
        return new FlexyPoolDataSourceDecorator();
    }
}
