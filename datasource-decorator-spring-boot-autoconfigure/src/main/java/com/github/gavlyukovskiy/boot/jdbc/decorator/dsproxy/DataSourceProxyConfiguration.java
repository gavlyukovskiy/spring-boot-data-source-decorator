/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.gavlyukovskiy.boot.jdbc.decorator.dsproxy;

import com.github.gavlyukovskiy.boot.jdbc.decorator.DataSourceDecoratorProperties;
import net.ttddyy.dsproxy.listener.MethodExecutionListener;
import net.ttddyy.dsproxy.listener.QueryCountStrategy;
import net.ttddyy.dsproxy.listener.QueryExecutionListener;
import net.ttddyy.dsproxy.listener.SingleQueryCountHolder;
import net.ttddyy.dsproxy.support.ProxyDataSource;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import net.ttddyy.dsproxy.transform.ParameterTransformer;
import net.ttddyy.dsproxy.transform.QueryTransformer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import java.util.List;

/**
 * Configuration for integration with datasource-proxy, allows to use define custom {@link QueryExecutionListener},
 * {@link ParameterTransformer} and {@link QueryTransformer}.
 *
 * @author Arthur Gavlyukovskiy
 */
@ConditionalOnClass(ProxyDataSource.class)
public class DataSourceProxyConfiguration {

    @Autowired
    private DataSourceDecoratorProperties dataSourceDecoratorProperties;

    @Autowired(required = false)
    private QueryCountStrategy queryCountStrategy;

    @Autowired(required = false)
    private List<QueryExecutionListener> listeners;

    @Autowired(required = false)
    private List<MethodExecutionListener> methodExecutionListeners;

    @Autowired(required = false)
    private ParameterTransformer parameterTransformer;

    @Autowired(required = false)
    private QueryTransformer queryTransformer;

    @Bean
    @ConditionalOnMissingBean
    public ProxyDataSourceBuilder proxyDataSourceBuilder() {
        ProxyDataSourceBuilder proxyDataSourceBuilder = ProxyDataSourceBuilder.create();
        DataSourceProxyProperties datasourceProxy = dataSourceDecoratorProperties.getDatasourceProxy();
        datasourceProxy.configure(proxyDataSourceBuilder);
        if (datasourceProxy.isCountQuery()) {
            proxyDataSourceBuilder.countQuery(queryCountStrategy);
        }
        if (listeners != null) {
            listeners.forEach(proxyDataSourceBuilder::listener);
        }
        if (methodExecutionListeners != null) {
            methodExecutionListeners.forEach(proxyDataSourceBuilder::methodListener);
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
    public ProxyDataSourceDecorator proxyDataSourceDecorator(ProxyDataSourceBuilder proxyDataSourceBuilder) {
        return new ProxyDataSourceDecorator(proxyDataSourceBuilder);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(value = "decorator.datasource.datasource-proxy.count-query", havingValue = "true")
    public QueryCountStrategy queryCountStrategy() {
        return new SingleQueryCountHolder();
    }
}
