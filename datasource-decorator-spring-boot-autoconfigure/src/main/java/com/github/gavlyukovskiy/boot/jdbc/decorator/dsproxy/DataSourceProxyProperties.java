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

package com.github.gavlyukovskiy.boot.jdbc.decorator.dsproxy;

import lombok.Getter;
import lombok.Setter;
import net.ttddyy.dsproxy.QueryCountHolder;
import net.ttddyy.dsproxy.listener.logging.CommonsLogLevel;
import net.ttddyy.dsproxy.listener.logging.SLF4JLogLevel;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.slf4j.Logger;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Properties for datasource-proxy
 *
 * @author Arthur Gavlyukovskiy
 * @since 1.1
 */
@Getter
@Setter
public class DataSourceProxyProperties {

    private static final Logger log = getLogger(DataSourceProxyProperties.class);

    /**
     * Logging to use for logging queries.
     */
    private DataSourceProxyLogging logging = DataSourceProxyLogging.SLF4J;

    private Query query = new Query();
    private SlowQuery slowQuery = new SlowQuery();

    /**
     * Use multiline output for logging query.
     *
     * @see ProxyDataSourceBuilder#multiline()
     */
    private boolean multiline = true;
    /**
     * Use json output for logging query.
     *
     * @see ProxyDataSourceBuilder#asJson()
     */
    private boolean jsonFormat = false;
    /**
     * Creates listener to count queries.
     *
     * @see ProxyDataSourceBuilder#countQuery()
     * @see QueryCountHolder
     */
    private boolean countQuery = false;

    /**
     * Properties to configure query logging listener.
     *
     * @see ProxyDataSourceBuilder#logQueryToSysOut()
     * @see ProxyDataSourceBuilder#logQueryBySlf4j(SLF4JLogLevel, String)
     * @see ProxyDataSourceBuilder#logQueryByCommons(CommonsLogLevel, String)
     * @see ProxyDataSourceBuilder#logQueryByJUL(Level, String)
     */
    @Getter
    @Setter
    public static class Query {
        /**
         * Enable logging all queries to the log.
         */
        private boolean enableLogging = true;
        /**
         * Name of query logger.
         */
        private String loggerName;
        /**
         * Severity of query logger.
         */
        private String logLevel = "DEBUG";
    }

    /**
     * Properties to configure slow query logging listener.
     *
     * @see ProxyDataSourceBuilder#logSlowQueryToSysOut(long, TimeUnit)
     * @see ProxyDataSourceBuilder#logSlowQueryBySlf4j(long, TimeUnit)
     * @see ProxyDataSourceBuilder#logSlowQueryByCommons(long, TimeUnit)
     * @see ProxyDataSourceBuilder#logSlowQueryByJUL(long, TimeUnit)
     */
    @Getter
    @Setter
    public static class SlowQuery {
        /**
         * Enable logging slow queries to the log.
         */
        private boolean enableLogging = true;
        /**
         * Name of slow query logger.
         */
        private String loggerName;
        /**
         * Severity of slow query logger.
         */
        private String logLevel = "WARN";
        /**
         * Number of seconds to consider query as slow.
         */
        private long threshold = 300;
    }

    public enum DataSourceProxyLogging {
        SYSOUT,
        SLF4J,
        COMMONS,
        JUL
    }
}
