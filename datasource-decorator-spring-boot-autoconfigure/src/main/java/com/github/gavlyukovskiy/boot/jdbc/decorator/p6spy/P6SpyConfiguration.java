package com.github.gavlyukovskiy.boot.jdbc.decorator.p6spy;

import com.github.gavlyukovskiy.boot.jdbc.decorator.DataSourceDecorator;
import com.p6spy.engine.event.JdbcEventListener;
import com.p6spy.engine.spy.P6DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;

import javax.annotation.PostConstruct;

import java.util.List;

@ConditionalOnClass(P6DataSource.class)
public class P6SpyConfiguration {

    @Autowired(required = false)
    private List<JdbcEventListener> listeners;

    @PostConstruct
    public void initRuntimeListenerHolder() {
        if (listeners != null) {
            RuntimeListenerHolder.setListeners(listeners);
        }
    }

    @Bean
    public DataSourceDecorator p6SpyDataSourceDecorator() {
        return new P6SpyDataSourceDecorator(listeners);
    }
}
