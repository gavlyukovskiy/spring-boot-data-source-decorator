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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Trims database name to a shortest name trims 'jdbc:' and port number if name is longer than a span name limit.
 *
 * @author Arthur Gavlyukovskiy
 * @since 1.2.1
 */
class TrimmingDataSourceSpanNameResolver implements DataSourceSpanNameResolver {

    private static final Pattern PORT_PATTERN = Pattern.compile("(:\\d+)");
    private static final int MAX_SPAN_NAME = 50;

    @Override
    public String resolveName(Connection connection) {
        try {
            String dataSourceUrl = connection.getMetaData().getURL();
            // remove jdbc:
            dataSourceUrl = dataSourceUrl.substring(5);
            // if name is too long remove db port
            if (isDataSourceNameLong(dataSourceUrl)) {
                int portStartIndex = dataSourceUrl.lastIndexOf(":");
                Matcher matcher = PORT_PATTERN.matcher(dataSourceUrl);
                matcher.region(portStartIndex, dataSourceUrl.length());
                if (matcher.find()) {
                    String port = matcher.group(1);
                    dataSourceUrl = dataSourceUrl.replace(port, "");
                }
            }
            return dataSourceUrl;
        }
        catch (SQLException e) {
            return null;
        }
    }

    private boolean isDataSourceNameLong(String dataSourceUrl) {
        return dataSourceUrl.length() + SleuthListenerConfiguration.SPAN_CONNECTION_POSTFIX.length() > MAX_SPAN_NAME;
    }
}
