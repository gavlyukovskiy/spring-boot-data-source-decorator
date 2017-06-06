package com.github.gavlyukovskiy.boot.jdbc.decorator.p6spy;

import com.github.gavlyukovskiy.boot.jdbc.decorator.DataSourceDecorator;
import com.p6spy.engine.spy.P6DataSource;
import org.springframework.core.Ordered;

import javax.sql.DataSource;

class P6SpyDataSourceDecorator implements DataSourceDecorator, Ordered {

    @Override
    public DataSource decorate(String beanName, DataSource dataSource) {
        return new P6DataSource(dataSource);
    }

    @Override
    public int getOrder() {
        return 30;
    }
}
