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
import net.ttddyy.dsproxy.ExecutionInfo;

import javax.sql.CommonDataSource;
import javax.sql.DataSource;

import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@link DataSource} url resolver based on p6spy's {@link ConnectionInformation} and datasource-proxy's {@link ExecutionInfo}.
 *
 * @author Arthur Gavlyukovskiy
 * @since 1.2
 */
public class DataSourceLookupUtil {

    private Map<CommonDataSource, String> resolvedP6SpyNames = new ConcurrentHashMap<>();
    private Map<String, String> resolvedDataSourceProxyNames = new ConcurrentHashMap<>();

    public String dataSourceUrl(ConnectionInformation connectionInformation) {
        return resolvedP6SpyNames.computeIfAbsent(connectionInformation.getDataSource(), dataSource -> {
            try {
                return connectionInformation.getConnection().getMetaData().getURL();
            }
            catch (SQLException e) {
                return "jdbc:";
            }
        });
    }

    public String dataSourceUrl(ExecutionInfo executionInfo) {
        return resolvedDataSourceProxyNames.computeIfAbsent(executionInfo.getDataSourceName(), dataSourceName -> {
            try {
                return executionInfo.getStatement().getConnection().getMetaData().getURL();
            }
            catch (SQLException e) {
                return "jdbc:";
            }
        });
    }
}
