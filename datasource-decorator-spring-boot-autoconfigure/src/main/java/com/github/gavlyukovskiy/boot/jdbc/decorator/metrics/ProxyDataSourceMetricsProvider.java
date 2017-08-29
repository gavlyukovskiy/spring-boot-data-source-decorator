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

package com.github.gavlyukovskiy.boot.jdbc.decorator.metrics;

import com.github.gavlyukovskiy.boot.jdbc.decorator.DataSourceDecorationStage;
import com.github.gavlyukovskiy.boot.jdbc.decorator.DecoratedDataSource;
import net.ttddyy.dsproxy.QueryCount;
import net.ttddyy.dsproxy.listener.ChainListener;
import net.ttddyy.dsproxy.listener.DataSourceQueryCountListener;
import net.ttddyy.dsproxy.listener.QueryCountStrategy;
import net.ttddyy.dsproxy.listener.QueryExecutionListener;
import net.ttddyy.dsproxy.listener.SingleQueryCountHolder;
import net.ttddyy.dsproxy.support.ProxyDataSource;

import javax.sql.DataSource;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Provides metrics specific for datasource-proxy.
 *
 * @author Arthur Gavlyukovskiy
 * @since 1.2.2
 */
public class ProxyDataSourceMetricsProvider implements DecoratedDataSourceMetricsProvider {

    @Override
    public Map<String, Number> getMetrics(DataSource dataSource) {
        if (dataSource instanceof DecoratedDataSource) {
            DecoratedDataSource decoratedDataSource = (DecoratedDataSource) dataSource;
            QueryCount queryCount = findQueryCountForDataSource(decoratedDataSource);
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
    }

    private QueryCount findQueryCountForDataSource(DecoratedDataSource decoratedDataSource) {
        for (DataSourceDecorationStage dataSourceDecorationStage : decoratedDataSource.getDecoratingChain()) {
            if (dataSourceDecorationStage.getDataSource() instanceof ProxyDataSource) {
                ProxyDataSource proxyDataSource = (ProxyDataSource) dataSourceDecorationStage.getDataSource();
                List<QueryExecutionListener> listeners = ((ChainListener) proxyDataSource.getInterceptorHolder().getListener()).getListeners();
                for (QueryExecutionListener listener : listeners) {
                    if (listener instanceof DataSourceQueryCountListener) {
                        QueryCountStrategy queryCountStrategy = ((DataSourceQueryCountListener) listener).getQueryCountStrategy();
                        if (queryCountStrategy instanceof SingleQueryCountHolder) {
                            return ((SingleQueryCountHolder) queryCountStrategy).getQueryCountMap().get(proxyDataSource.getDataSourceName());
                        }
                    }
                }
            }
        }
        return null;
    }
}
