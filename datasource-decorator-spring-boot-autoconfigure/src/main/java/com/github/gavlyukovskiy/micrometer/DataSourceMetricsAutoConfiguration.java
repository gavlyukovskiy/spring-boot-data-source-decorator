package com.github.gavlyukovskiy.micrometer;

import com.github.gavlyukovskiy.boot.jdbc.decorator.dsproxy.ProxyDataSourceDecorator;
import com.github.gavlyukovskiy.boot.jdbc.decorator.p6spy.P6SpyDataSourceDecorator;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.spring.autoconfigure.MetricsAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jdbc.metadata.DataSourcePoolMetadataProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
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
@ConditionalOnBean(MeterRegistry.class)
@AutoConfigureAfter(name = {
    "io.micrometer.spring.autoconfigure.MetricsAutoConfiguration",
    "org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration"
})
public class DataSourceMetricsAutoConfiguration {


    @Configuration
    @ConditionalOnBean(P6SpyDataSourceDecorator.class)
    static class P6SpyConfiguration {

        @Bean
        public DataSourceMetricsJdbcEventListener dataSourceMetricsJdbcEventListener(ApplicationContext applicationContext,
                MeterRegistry meterRegistry, Collection<DataSourcePoolMetadataProvider> metadataProviders) {
            return new DataSourceMetricsJdbcEventListener(applicationContext, meterRegistry, metadataProviders);
        }
    }

    @Configuration
    @ConditionalOnBean(ProxyDataSourceDecorator.class)
    @ConditionalOnMissingBean(P6SpyConfiguration.class)
    static class ProxyDataSourceConfiguration {

        @Bean
        public DataSourceMetricsMethodExecutionListener dataSourceMetricsMethodExecutionListener(MeterRegistry meterRegistry, Collection<DataSourcePoolMetadataProvider> metadataProviders) {
            return new DataSourceMetricsMethodExecutionListener(meterRegistry, metadataProviders);
        }
    }
}
