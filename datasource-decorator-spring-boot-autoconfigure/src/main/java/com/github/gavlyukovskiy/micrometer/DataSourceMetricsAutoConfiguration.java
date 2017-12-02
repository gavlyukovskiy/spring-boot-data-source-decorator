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

package com.github.gavlyukovskiy.micrometer;

import com.github.gavlyukovskiy.boot.jdbc.decorator.DataSourceNameResolver;
import com.github.gavlyukovskiy.boot.jdbc.decorator.dsproxy.ProxyDataSourceDecorator;
import com.github.gavlyukovskiy.boot.jdbc.decorator.p6spy.P6SpyDataSourceDecorator;
import com.github.gavlyukovskiy.micrometer.DataSourceMetricsAutoConfiguration.OnAnyListenableProxyProviderCondition;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jdbc.metadata.DataSourcePoolMetadataProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

import java.util.Collection;
import java.util.Map;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for integration with micrometer.
 *
 * @author Arthur Gavlyukovskiy
 * @since 1.3.0
 */
@Configuration
@ConditionalOnBean(MeterRegistry.class)
@Conditional(OnAnyListenableProxyProviderCondition.class)
@AutoConfigureAfter(name = {
    "io.micrometer.spring.autoconfigure.MetricsAutoConfiguration",
    "org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration"
})
public class DataSourceMetricsAutoConfiguration {

    @Bean
    public DataSourceMetricsBinder dataSourceMetricsBinder(Map<String, DataSource> dataSources,
            Collection<DataSourcePoolMetadataProvider> metadataProviders,
            MeterRegistry registry) {
        return new DataSourceMetricsBinder(dataSources, metadataProviders, registry);
    }

    @Configuration
    @ConditionalOnBean(P6SpyDataSourceDecorator.class)
    static class P6SpyConfiguration {

        @Bean
        public DataSourceMetricsJdbcEventListener dataSourceMetricsJdbcEventListener(DataSourceMetricsBinder dataSourceMetricsBinder,
                DataSourceNameResolver dataSourceNameResolver) {
            return new DataSourceMetricsJdbcEventListener(dataSourceMetricsBinder, dataSourceNameResolver);
        }
    }

    @Configuration
    @ConditionalOnBean(ProxyDataSourceDecorator.class)
    @ConditionalOnMissingBean(P6SpyConfiguration.class)
    static class ProxyDataSourceConfiguration {

        @Bean
        public DataSourceMetricsMethodExecutionListener dataSourceMetricsMethodExecutionListener(DataSourceMetricsBinder dataSourceMetricsBinder) {
            return new DataSourceMetricsMethodExecutionListener(dataSourceMetricsBinder);
        }
    }

    static class OnAnyListenableProxyProviderCondition extends AnyNestedCondition {

        OnAnyListenableProxyProviderCondition() {
            super(ConfigurationPhase.REGISTER_BEAN);
        }

        @ConditionalOnBean(P6SpyDataSourceDecorator.class)
        static class HasP6Spy {
        }

        @ConditionalOnBean(ProxyDataSourceDecorator.class)
        static class HasDatasourceProxy {
        }
    }
}
