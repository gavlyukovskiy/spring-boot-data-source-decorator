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

import net.ttddyy.dsproxy.ExecutionInfo;

import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Span name resolver for a {@link ExecutionInfo}.
 *
 * @author Arthur Gavlyukovskiy
 * @since 1.2.1
 */
public class DataSourceProxySpanNameResolver {

    private Map<String, String> resolvedDataSourceProxyNames = new ConcurrentHashMap<>();

    public String querySpanName(ExecutionInfo executionInfo) {
        return spanPrefix(executionInfo) + SleuthListenerConfiguration.SPAN_QUERY_POSTFIX;
    }

    private String spanPrefix(ExecutionInfo executionInfo) {
        String resolvedDataSourceName = resolvedDataSourceProxyNames.computeIfAbsent(executionInfo.getDataSourceName(), dataSource -> {
            try {
                String dataSourceUrl = executionInfo.getStatement().getConnection().getMetaData().getURL();
                return DataSourceUrlCutter.shorten(dataSourceUrl);
            }
            catch (SQLException e) {
                return null;
            }
        });
        return resolvedDataSourceName != null ? resolvedDataSourceName : "jdbc:";
    }
}
