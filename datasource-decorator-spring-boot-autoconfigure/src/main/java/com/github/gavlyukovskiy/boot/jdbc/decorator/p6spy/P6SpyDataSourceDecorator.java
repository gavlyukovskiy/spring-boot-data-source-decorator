package com.github.gavlyukovskiy.boot.jdbc.decorator.p6spy;

import com.github.gavlyukovskiy.boot.jdbc.decorator.DataSourceDecorator;
import com.p6spy.engine.event.JdbcEventListener;
import com.p6spy.engine.logging.P6LogFactory;
import com.p6spy.engine.spy.P6DataSource;
import com.p6spy.engine.spy.P6SpyFactory;
import org.springframework.core.Ordered;

import javax.sql.DataSource;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class P6SpyDataSourceDecorator implements DataSourceDecorator, Ordered {

    private static final String DEFAULT_P6SPY_MODULES = Stream.of(P6SpyFactory.class, P6LogFactory.class)
            .map(Class::getName)
            .collect(Collectors.joining(","));

    private List<JdbcEventListener> listeners;

    P6SpyDataSourceDecorator(List<JdbcEventListener> listeners) {
        this.listeners = listeners;
    }

    @Override
    public DataSource decorate(String beanName, DataSource dataSource) {
        System.setProperty("p6spy.config.logMessageFormat", "com.p6spy.engine.spy.appender.MultiLineFormat");
        if (listeners != null) {
            System.setProperty("p6spy.config.modulelist", DEFAULT_P6SPY_MODULES + "," + RuntimeListenerSupportFactory.class.getName());
        }
        return new P6DataSource(dataSource);
    }

    @Override
    public int getOrder() {
        return 30;
    }
}
