package com.github.gavlyukovskiy.boot.jdbc.decorator.flexypool;

import com.github.gavlyukovskiy.boot.jdbc.decorator.DataSourceDecorator;
import com.vladmihalcea.flexypool.FlexyPoolDataSource;
import org.springframework.core.Ordered;

import javax.sql.DataSource;

class FlexyPoolDataSourceDecorator implements DataSourceDecorator, Ordered {

    @Override
    public DataSource decorate(String beanName, DataSource dataSource) {
        try {
            return new FlexyPoolDataSource<>(dataSource);
        }
        catch (Exception e) {
            return dataSource;
        }
    }

    @Override
    public int getOrder() {
        return 10;
    }
}
