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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.sql.DataSource;

/**
 * Cuts ':jdbc' and optionally port from {@link DataSource} url if the resulting span
 * is going to be too long for sending to Zipkin.
 *
 * @author Arthur Gavlyukovskiy
 */
class DataSourceUrlCutter {

    private static final Pattern PORT_PATTERN = Pattern.compile("(:\\d+)");
    private static final int MAX_SPAN_NAME = 50;

    static String shorten(String url) {
        // remove jdbc:
        url = url.substring(5);
        // if name is too long remove db port
        if (isDataSourceNameLong(url)) {
            int portStartIndex = url.lastIndexOf(":");
            Matcher matcher = PORT_PATTERN.matcher(url);
            matcher.region(portStartIndex, url.length());
            if (matcher.find()) {
                String port = matcher.group(1);
                url = url.replace(port, "");
            }
        }
        return url;
    }

    private static boolean isDataSourceNameLong(String dataSourceUrl) {
        return dataSourceUrl.length() + SleuthListenerConfiguration.SPAN_CONNECTION_POSTFIX.length() > MAX_SPAN_NAME;
    }
}
