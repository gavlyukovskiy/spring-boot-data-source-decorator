package com.github.gavlyukovskiy.boot.jdbc.decorator.metadata;

import com.github.gavlyukovskiy.boot.jdbc.decorator.DecoratedDataSource;
import com.github.gavlyukovskiy.boot.jdbc.decorator.DataSourceDecorationStage;
import net.ttddyy.dsproxy.QueryCount;
import net.ttddyy.dsproxy.listener.QueryCountStrategy;
import net.ttddyy.dsproxy.listener.SingleQueryCountHolder;
import net.ttddyy.dsproxy.support.ProxyDataSource;
import org.springframework.boot.actuate.endpoint.DataSourcePublicMetrics;
import org.springframework.boot.actuate.endpoint.PublicMetrics;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
@ConditionalOnClass(PublicMetrics.class)
public class DecoratedDataSourceMetadataProvidersConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(DataSourcePublicMetrics.class)
    public DecoratedDataSourcePublicMetrics decoratedDataSourcePublicMetrics() {
        return new DecoratedDataSourcePublicMetrics();
    }

    @Configuration
    @ConditionalOnClass(ProxyDataSource.class)
    class ProxyDataSourceMetadataProviderConfiguration {

        @Bean
        public DecoratedDataSourceMetadataProvider proxyDataSourceMetadataProvider(QueryCountStrategy queryCountStrategy) {
            return dataSource -> {
                if (dataSource instanceof DecoratedDataSource && queryCountStrategy instanceof SingleQueryCountHolder) {
                    DecoratedDataSource decoratedDataSource = (DecoratedDataSource) dataSource;
                    DataSourceDecorationStage initialStage = decoratedDataSource.getDecoratingChain().get(decoratedDataSource.getDecoratingChain().size() - 1);
                    QueryCount queryCount = ((SingleQueryCountHolder) queryCountStrategy).getQueryCountMap().get(initialStage.getBeanName());
                    if (queryCount != null) {
                        Map<String, Number> metrics = new LinkedHashMap<>();
                        metrics.put("datasource-proxy.query.count.select", queryCount.getSelect());
                        metrics.put("datasource-proxy.query.count.insert", queryCount.getInsert());
                        metrics.put("datasource-proxy.query.count.update", queryCount.getUpdate());
                        metrics.put("datasource-proxy.query.count.delete", queryCount.getDelete());
                        metrics.put("datasource-proxy.query.count.other", queryCount.getOther());
                        metrics.put("datasource-proxy.query.count.success", queryCount.getSuccess());
                        metrics.put("datasource-proxy.query.count.failure", queryCount.getFailure());
                        metrics.put("datasource-proxy.query.count.total", queryCount.getTotal());
                        metrics.put("datasource-proxy.query.time", queryCount.getTime());
                        return metrics;
                    }
                }
                return null;
            };
        }
    }
}
