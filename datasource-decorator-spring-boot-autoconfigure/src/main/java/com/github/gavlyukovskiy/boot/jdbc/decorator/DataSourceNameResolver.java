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

package com.github.gavlyukovskiy.boot.jdbc.decorator;

import org.springframework.context.ApplicationContext;

import javax.sql.CommonDataSource;
import javax.sql.DataSource;

import java.util.Map;
import java.util.Map.Entry;

/**
 * {@link CommonDataSource} name resolver based on bean name.
 *
 * @author Arthur Gavlyukovskiy
 * @since 1.3.0
 */
public class DataSourceNameResolver {

    private final ApplicationContext applicationContext;
    private Map<String, DataSource> dataSources;

    public DataSourceNameResolver(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public String resolveDataSourceName(CommonDataSource dataSource) {
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

    private boolean matchesDataSource(DecoratedDataSource decoratedCandidate, CommonDataSource dataSource) {
        return decoratedCandidate.getRealDataSource() == dataSource
                || decoratedCandidate.getDecoratingChain().stream()
                .map(DataSourceDecorationStage::getDataSource)
                .anyMatch(candidate -> candidate == dataSource);
    }
}
