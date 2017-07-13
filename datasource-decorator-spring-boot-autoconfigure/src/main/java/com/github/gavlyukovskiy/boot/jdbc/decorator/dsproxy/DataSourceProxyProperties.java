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

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Properties for datasource-proxy
 *
 * @see ProxyDataSourceBuilder
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

    void configure(ProxyDataSourceBuilder proxyDataSourceBuilder) {
        switch (logging) {
            case SLF4J: {
                if (query.isEnableLogging()) {
                    proxyDataSourceBuilder.logQueryBySlf4j(toSlf4JLogLevel(query.getLogLevel()), query.getLoggerName());
                }
                if (slowQuery.isEnableLogging()) {
                    proxyDataSourceBuilder.logSlowQueryBySlf4j(slowQuery.getThreshold(), TimeUnit.SECONDS,
                            toSlf4JLogLevel(slowQuery.getLogLevel()), slowQuery.getLoggerName());
                }
                break;
            }
            case JUL: {
                if (query.isEnableLogging()) {
                    proxyDataSourceBuilder.logQueryByJUL(toJULLogLevel(query.getLogLevel()), query.getLoggerName());
                }
                if (slowQuery.isEnableLogging()) {
                    proxyDataSourceBuilder.logSlowQueryByJUL(slowQuery.getThreshold(), TimeUnit.SECONDS,
                            toJULLogLevel(slowQuery.getLogLevel()), slowQuery.getLoggerName());
                }
                break;
            }
            case COMMONS: {
                if (query.isEnableLogging()) {
                    proxyDataSourceBuilder.logQueryByCommons(toCommonsLogLevel(query.getLogLevel()), query.getLoggerName());
                }
                if (slowQuery.isEnableLogging()) {
                    proxyDataSourceBuilder.logSlowQueryByCommons(slowQuery.getThreshold(), TimeUnit.SECONDS,
                            toCommonsLogLevel(slowQuery.getLogLevel()), slowQuery.getLoggerName());
                }
                break;
            }
            case SYSOUT: {
                if (query.isEnableLogging()) {
                    proxyDataSourceBuilder.logQueryToSysOut();
                }
                if (slowQuery.isEnableLogging()) {
                    proxyDataSourceBuilder.logSlowQueryToSysOut(slowQuery.getThreshold(), TimeUnit.SECONDS);
                }
                break;
            }
        }
        if (multiline && jsonFormat) {
            log.warn("Found opposite multiline and json format, multiline will be used (may depend on library version)");
        }
        if (multiline) {
            proxyDataSourceBuilder.multiline();
        }
        if (countQuery) {
            proxyDataSourceBuilder.countQuery();
        }
        if (jsonFormat) {
            proxyDataSourceBuilder.asJson();
        }
    }

    private SLF4JLogLevel toSlf4JLogLevel(String logLevel) {
        if (logLevel == null) {
            return null;
        }
        for (SLF4JLogLevel slf4JLogLevel : SLF4JLogLevel.values()) {
            if (slf4JLogLevel.name().equalsIgnoreCase(logLevel)) {
                return slf4JLogLevel;
            }
        }
        throw new IllegalArgumentException("Unresolved log level " + logLevel + " for slf4j logger, " +
                "known levels: " + Arrays.toString(SLF4JLogLevel.values()));
    }

    private Level toJULLogLevel(String logLevel) {
        if (logLevel == null) {
            return null;
        }
        try {
            return Level.parse(logLevel);
        }
        catch (IllegalArgumentException e) {
            if (logLevel.equalsIgnoreCase("DEBUG")) {
                return Level.FINE;
            }
            if (logLevel.equalsIgnoreCase("WARN")) {
                return Level.WARNING;
            }
            throw new IllegalArgumentException("Unresolved log level " + logLevel + " for java.util.logging", e);
        }
    }

    private CommonsLogLevel toCommonsLogLevel(String logLevel) {
        if (logLevel == null) {
            return null;
        }
        for (CommonsLogLevel commonsLogLevel : CommonsLogLevel.values()) {
            if (commonsLogLevel.name().equalsIgnoreCase(logLevel)) {
                return commonsLogLevel;
            }
        }
        throw new IllegalArgumentException("Unresolved log level " + logLevel + " for apache commons logger, " +
                "known levels " + Arrays.toString(CommonsLogLevel.values()));
    }

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
