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

import com.p6spy.engine.common.ConnectionInformation;
import com.p6spy.engine.common.StatementInformation;

import javax.sql.CommonDataSource;

import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Span name resolver for a {@link ConnectionInformation} and a {@link StatementInformation}
 *
 * @author Arthur Gavlyukovskiy
 * @since 1.2.1
 */
public class P6SpySpanNameResolver {

    private final Map<CommonDataSource, String> resolvedP6SpyNames = new ConcurrentHashMap<>();

    public String connectionSpanName(ConnectionInformation connectionInformation) {
        return spanPrefix(connectionInformation) + SleuthListenerConfiguration.SPAN_CONNECTION_POSTFIX;
    }

    public String querySpanName(StatementInformation statementInformation) {
        return spanPrefix(statementInformation.getConnectionInformation()) + SleuthListenerConfiguration.SPAN_QUERY_POSTFIX;
    }

    private String spanPrefix(ConnectionInformation connectionInformation) {
        String resolvedDataSourceName = resolvedP6SpyNames.computeIfAbsent(connectionInformation.getDataSource(), dataSource -> {
            try {
                String dataSourceUrl = connectionInformation.getConnection().getMetaData().getURL();
                return DataSourceUrlCutter.shorten(dataSourceUrl);
            } catch (SQLException e) {
                return null;
            }
        });
        return resolvedDataSourceName != null ? resolvedDataSourceName : "jdbc:";
    }
}
