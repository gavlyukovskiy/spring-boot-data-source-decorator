package com.github.gavlyukovskiy.boot.jdbc.decorator.dsproxy;

import com.github.gavlyukovskiy.boot.jdbc.decorator.DataSourceDecoratorProperties;
import com.github.gavlyukovskiy.boot.jdbc.decorator.DataSourceDecorator;
import net.ttddyy.dsproxy.listener.QueryExecutionListener;
import net.ttddyy.dsproxy.support.ProxyDataSource;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import net.ttddyy.dsproxy.transform.ParameterTransformer;
import net.ttddyy.dsproxy.transform.QueryTransformer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import java.util.List;

/**
 * Configuration for datasource-proxy, allows to use define custom {@link QueryExecutionListener}s,
 * {@link ParameterTransformer} and {@link QueryTransformer}.
 *
 * @author Arthur Gavlyukovskiy
 */
@ConditionalOnClass(ProxyDataSource.class)
public class DataSourceProxyConfiguration {

    @Autowired
    private DataSourceDecoratorProperties dataSourceDecoratorProperties;

    @Autowired(required = false)
    private List<QueryExecutionListener> listeners;

    @Autowired(required = false)
    private ParameterTransformer parameterTransformer;

    @Autowired(required = false)
    private QueryTransformer queryTransformer;

    @Bean
    @ConditionalOnMissingBean
    public ProxyDataSourceBuilder proxyDataSourceBuilder() {
        ProxyDataSourceBuilder proxyDataSourceBuilder = ProxyDataSourceBuilder.create();
        dataSourceDecoratorProperties.getDataSourceProxy().configure(proxyDataSourceBuilder);
        if (listeners != null) {
            listeners.forEach(proxyDataSourceBuilder::listener);
        }
        if (parameterTransformer != null) {
            proxyDataSourceBuilder.parameterTransformer(parameterTransformer);
        }
        if (queryTransformer != null) {
            proxyDataSourceBuilder.queryTransformer(queryTransformer);
        }
        return proxyDataSourceBuilder;
    }

    @Bean
    public DataSourceDecorator proxyDataSourceDecorator(ProxyDataSourceBuilder proxyDataSourceBuilder) {
        return new ProxyDataSourceDecorator(proxyDataSourceBuilder);
    }
}
