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

package com.github.gavlyukovskiy.cloud.sleuth;

import com.github.gavlyukovskiy.boot.jdbc.decorator.DataSourceDecorationStage;
import com.github.gavlyukovskiy.boot.jdbc.decorator.DecoratedDataSource;
import com.p6spy.engine.common.ConnectionInformation;
import com.p6spy.engine.common.StatementInformation;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import javax.annotation.PostConstruct;
import javax.sql.CommonDataSource;
import javax.sql.DataSource;

import java.util.Map;
import java.util.Map.Entry;

/**
 * Span name resolver for a {@link ConnectionInformation} and a {@link StatementInformation}
 *
 * @author Arthur Gavlyukovskiy
 * @since 1.2.1
 */
public class P6SpySpanNameResolver implements ApplicationContextAware {

    private ApplicationContext applicationContext;
    private Map<String, DataSource> dataSources;

    public String connectionSpanName(ConnectionInformation connectionInformation) {
        String dataSourceName = resolveDataSourceName(connectionInformation.getDataSource());
        return "jdbc:/" + dataSourceName + SleuthListenerAutoConfiguration.SPAN_CONNECTION_POSTFIX;
    }

    public String querySpanName(StatementInformation statementInformation) {
        String dataSourceName = resolveDataSourceName(statementInformation.getConnectionInformation().getDataSource());
        return "jdbc:/" + dataSourceName + SleuthListenerAutoConfiguration.SPAN_QUERY_POSTFIX;
    }

    private String resolveDataSourceName(CommonDataSource dataSource) {
        if (dataSources == null) {
            this.dataSources = applicationContext.getBeansOfType(DataSource.class);
        }
        return dataSources.entrySet()
                .stream()
                .filter(entry -> {
                    DataSource candidate = entry.getValue();
                    if (candidate instanceof DecoratedDataSource) {
                        return matchesDataSource((DecoratedDataSource) candidate, dataSource);
                    }
                    return candidate == dataSource;
                })
                .findFirst()
                .map(Entry::getKey)
                .orElse("dataSource");
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    private boolean matchesDataSource(DecoratedDataSource decoratedCandidate, CommonDataSource dataSource) {
        return decoratedCandidate.getDecoratingChain().stream()
                .map(DataSourceDecorationStage::getDataSource)
                .anyMatch(candidate -> candidate == dataSource);
    }
}
