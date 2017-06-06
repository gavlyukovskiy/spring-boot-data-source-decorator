package com.github.gavlyukovskiy.boot.jdbc.decorator.p6spy;

import com.github.gavlyukovskiy.boot.jdbc.decorator.DataSourceDecorator;
import com.github.gavlyukovskiy.boot.jdbc.decorator.DataSourceDecoratorProperties;
import com.p6spy.engine.event.JdbcEventListener;
import com.p6spy.engine.logging.P6LogFactory;
import com.p6spy.engine.spy.P6DataSource;
import com.p6spy.engine.spy.P6SpyFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;

import javax.annotation.PostConstruct;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.slf4j.LoggerFactory.getLogger;

@ConditionalOnClass(P6DataSource.class)
public class P6SpyConfiguration {

    private static final Logger log = getLogger(P6SpyConfiguration.class);

    private static final String DEFAULT_P6SPY_MODULES = Stream.of(P6SpyFactory.class, P6LogFactory.class)
            .map(Class::getName)
            .collect(Collectors.joining(","));

    @Autowired
    private DataSourceDecoratorProperties dataSourceDecoratorProperties;

    @Autowired(required = false)
    private List<JdbcEventListener> listeners;

    @PostConstruct
    public void init() {
        if (listeners != null) {
            if (dataSourceDecoratorProperties.getP6spy().isEnableDynamicListeners()) {
                RuntimeListenerHolder.setListeners(listeners);
                System.setProperty("p6spy.config.modulelist", DEFAULT_P6SPY_MODULES + "," + RuntimeListenerSupportFactory.class.getName());
            }
            else {
                log.info("JdbcEventListener(s) " + listeners + " will not be applied since enable-dynamic-listeners is false");
            }
        }
        if (dataSourceDecoratorProperties.getP6spy().isMultiline()) {
            System.setProperty("p6spy.config.logMessageFormat", "com.p6spy.engine.spy.appender.MultiLineFormat");
        }
    }

    @Bean
    public DataSourceDecorator p6SpyDataSourceDecorator() {
        return new P6SpyDataSourceDecorator();
    }
}
