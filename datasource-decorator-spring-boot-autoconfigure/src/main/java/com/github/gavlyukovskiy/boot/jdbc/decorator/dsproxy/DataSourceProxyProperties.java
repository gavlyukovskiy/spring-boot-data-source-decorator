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

import net.ttddyy.dsproxy.QueryCountHolder;
import net.ttddyy.dsproxy.listener.logging.CommonsLogLevel;
import net.ttddyy.dsproxy.listener.logging.SLF4JLogLevel;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Properties for datasource-proxy
 *
 * @author Arthur Gavlyukovskiy
 * @since 1.1
 */
public class DataSourceProxyProperties {

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

    public DataSourceProxyLogging getLogging() {
        return this.logging;
    }

    public Query getQuery() {
        return this.query;
    }

    public SlowQuery getSlowQuery() {
        return this.slowQuery;
    }

    public boolean isMultiline() {
        return this.multiline;
    }

    public boolean isJsonFormat() {
        return this.jsonFormat;
    }

    public boolean isCountQuery() {
        return this.countQuery;
    }

    public void setLogging(DataSourceProxyLogging logging) {
        this.logging = logging;
    }

    public void setQuery(Query query) {
        this.query = query;
    }

    public void setSlowQuery(SlowQuery slowQuery) {
        this.slowQuery = slowQuery;
    }

    public void setMultiline(boolean multiline) {
        this.multiline = multiline;
    }

    public void setJsonFormat(boolean jsonFormat) {
        this.jsonFormat = jsonFormat;
    }

    public void setCountQuery(boolean countQuery) {
        this.countQuery = countQuery;
    }

    /**
     * Properties to configure query logging listener.
     *
     * @see ProxyDataSourceBuilder#logQueryToSysOut()
     * @see ProxyDataSourceBuilder#logQueryBySlf4j(SLF4JLogLevel, String)
     * @see ProxyDataSourceBuilder#logQueryByCommons(CommonsLogLevel, String)
     * @see ProxyDataSourceBuilder#logQueryByJUL(Level, String)
     */
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

        public boolean isEnableLogging() {
            return this.enableLogging;
        }

        public String getLoggerName() {
            return this.loggerName;
        }

        public String getLogLevel() {
            return this.logLevel;
        }

        public void setEnableLogging(boolean enableLogging) {
            this.enableLogging = enableLogging;
        }

        public void setLoggerName(String loggerName) {
            this.loggerName = loggerName;
        }

        public void setLogLevel(String logLevel) {
            this.logLevel = logLevel;
        }
    }

    /**
     * Properties to configure slow query logging listener.
     *
     * @see ProxyDataSourceBuilder#logSlowQueryToSysOut(long, TimeUnit)
     * @see ProxyDataSourceBuilder#logSlowQueryBySlf4j(long, TimeUnit)
     * @see ProxyDataSourceBuilder#logSlowQueryByCommons(long, TimeUnit)
     * @see ProxyDataSourceBuilder#logSlowQueryByJUL(long, TimeUnit)
     */
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

        public boolean isEnableLogging() {
            return this.enableLogging;
        }

        public String getLoggerName() {
            return this.loggerName;
        }

        public String getLogLevel() {
            return this.logLevel;
        }

        public long getThreshold() {
            return this.threshold;
        }

        public void setEnableLogging(boolean enableLogging) {
            this.enableLogging = enableLogging;
        }

        public void setLoggerName(String loggerName) {
            this.loggerName = loggerName;
        }

        public void setLogLevel(String logLevel) {
            this.logLevel = logLevel;
        }

        public void setThreshold(long threshold) {
            this.threshold = threshold;
        }
    }

    public enum DataSourceProxyLogging {
        SYSOUT,
        SLF4J,
        COMMONS,
        JUL
    }
}
