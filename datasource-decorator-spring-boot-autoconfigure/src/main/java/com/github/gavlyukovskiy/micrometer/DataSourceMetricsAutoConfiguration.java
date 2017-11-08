package com.github.gavlyukovskiy.micrometer;

import com.github.gavlyukovskiy.boot.jdbc.decorator.dsproxy.ProxyDataSourceDecorator;
import com.github.gavlyukovskiy.boot.jdbc.decorator.p6spy.P6SpyDataSourceDecorator;
import com.github.gavlyukovskiy.boot.jdbc.decorator.p6spy.P6SpyDataSourceNameResolver;
import com.github.gavlyukovskiy.micrometer.DataSourceMetricsAutoConfiguration.OnAnyListenableProxyProviderCondition;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jdbc.metadata.DataSourcePoolMetadataProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

import java.util.Collection;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for integration with micrometer.
 *
 * @author Arthur Gavlyukovskiy
 * @since 1.2.2
 */
@Configuration
@ConditionalOnClass(MeterRegistry.class)
@Conditional(OnAnyListenableProxyProviderCondition.class)
@AutoConfigureAfter(name = {
    "io.micrometer.spring.autoconfigure.MetricsAutoConfiguration",
    "org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration"
})
public class DataSourceMetricsAutoConfiguration {

    @Bean
    public DataSourceMetricsBinder dataSourceMetricsBinder(ApplicationContext applicationContext,
            Collection<DataSourcePoolMetadataProvider> metadataProviders) {
        return new DataSourceMetricsBinder(applicationContext, metadataProviders);
    }

    @Configuration
    @ConditionalOnBean(P6SpyDataSourceDecorator.class)
    static class P6SpyConfiguration {

        @Bean
        public DataSourceMetricsJdbcEventListener dataSourceMetricsJdbcEventListener(DataSourceMetricsBinder dataSourceMetricsBinder,
                P6SpyDataSourceNameResolver p6SpyDataSourceNameResolver) {
            return new DataSourceMetricsJdbcEventListener(dataSourceMetricsBinder, p6SpyDataSourceNameResolver);
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
