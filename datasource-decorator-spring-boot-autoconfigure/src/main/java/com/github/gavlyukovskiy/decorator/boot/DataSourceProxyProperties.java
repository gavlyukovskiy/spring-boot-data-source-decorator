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

package com.github.gavlyukovskiy.decorator.boot;

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
 * Configuration for datasource-proxy
 *
 * @see ProxyDataSourceBuilder
 *
 * @author Arthur Gavlyukovskiy
 * @since 1.1
 */
public class DataSourceProxyProperties {

    private static final Logger log = getLogger(DataSourceProxyProperties.class);

    /**
     * Logging to use for logging queries.
     */
    private DataSourceProxyLogging logging = DataSourceProxyLogging.SYSOUT;

    /**
     * Logging level appropriate to the selected logging type.
     *
     * @see ProxyDataSourceBuilder#logQueryToSysOut()
     * @see ProxyDataSourceBuilder#logQueryBySlf4j()
     * @see ProxyDataSourceBuilder#logQueryByCommons()
     * @see ProxyDataSourceBuilder#logQueryByJUL()
     */
    private String logLevel;

    /**
     * Name of the logger.
     *
     * @see ProxyDataSourceBuilder#logQueryToSysOut()
     * @see ProxyDataSourceBuilder#logQueryBySlf4j(SLF4JLogLevel, String)
     * @see ProxyDataSourceBuilder#logQueryByCommons(CommonsLogLevel, String)
     * @see ProxyDataSourceBuilder#logQueryByJUL(Level, String)
     */
    private String loggerName;

    /**
     * Log all queries to the log.
     *
     * @see ProxyDataSourceBuilder#logQueryToSysOut()
     * @see ProxyDataSourceBuilder#logQueryBySlf4j(SLF4JLogLevel, String)
     * @see ProxyDataSourceBuilder#logQueryByCommons(CommonsLogLevel, String)
     * @see ProxyDataSourceBuilder#logQueryByJUL(Level, String)
     */
    private boolean logQuery = true;

    /**
     * Log slow queries to the log.
     *
     * @see ProxyDataSourceBuilder#logSlowQueryToSysOut(long, TimeUnit)
     * @see ProxyDataSourceBuilder#logSlowQueryBySlf4j(long, TimeUnit)
     * @see ProxyDataSourceBuilder#logSlowQueryByCommons(long, TimeUnit)
     * @see ProxyDataSourceBuilder#logSlowQueryByJUL(long, TimeUnit)
     */
    private boolean logSlowQuery = true;

    /**
     * Number of seconds to consider query as slow.
     *
     * @see ProxyDataSourceBuilder#logSlowQueryToSysOut(long, TimeUnit)
     * @see ProxyDataSourceBuilder#logSlowQueryBySlf4j(long, TimeUnit)
     * @see ProxyDataSourceBuilder#logSlowQueryByCommons(long, TimeUnit)
     * @see ProxyDataSourceBuilder#logSlowQueryByJUL(long, TimeUnit)
     */
    private long slowQueryThreshold = 300;

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
            case SLF4j: {
                SLF4JLogLevel logLevel = toSlf4JLogLevel();
                if (logQuery) {
                    proxyDataSourceBuilder.logQueryBySlf4j(logLevel, loggerName);
                }
                if (logSlowQuery) {
                    proxyDataSourceBuilder.logSlowQueryBySlf4j(slowQueryThreshold, TimeUnit.SECONDS, logLevel, loggerName);
                }
                break;
            }
            case JUL: {
                Level logLevel = toJULLogLevel();
                if (logQuery) {
                    proxyDataSourceBuilder.logQueryByJUL(logLevel, loggerName);
                }
                if (logSlowQuery) {
                    proxyDataSourceBuilder.logSlowQueryByJUL(slowQueryThreshold, TimeUnit.SECONDS, logLevel, loggerName);
                }
                break;
            }
            case COMMONS: {
                CommonsLogLevel logLevel = toCommonsLogLevel();
                if (logQuery) {
                    proxyDataSourceBuilder.logQueryByCommons(logLevel, loggerName);
                }
                if (logSlowQuery) {
                    proxyDataSourceBuilder.logSlowQueryByCommons(slowQueryThreshold, TimeUnit.SECONDS, logLevel, loggerName);
                }
                break;
            }
            case SYSOUT: {
                if (logQuery) {
                    proxyDataSourceBuilder.logQueryToSysOut();
                }
                if (logSlowQuery) {
                    proxyDataSourceBuilder.logSlowQueryToSysOut(slowQueryThreshold, TimeUnit.SECONDS);
                }
                break;
            }
        }
        if (multiline && jsonFormat) {
            log.warn("Found opposite multiline and json format, multiline will be used (may be depend on library version)");
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

    private SLF4JLogLevel toSlf4JLogLevel() {
        if (logLevel == null) {
            return null;
        }
        for (SLF4JLogLevel slf4JLogLevel : SLF4JLogLevel.values()) {
            if (slf4JLogLevel.name().equals(logLevel)) {
                return slf4JLogLevel;
            }
        }
        throw new IllegalArgumentException("Unresolved log level " + logLevel + " for slf4j logger, " +
                "known levels: " + Arrays.toString(SLF4JLogLevel.values()));
    }

    private Level toJULLogLevel() {
        if (logLevel == null) {
            return null;
        }
        try {
            return Level.parse(logLevel);
        }
        catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unresolved log level " + logLevel + " for java.util.logging", e);
        }
    }

    private CommonsLogLevel toCommonsLogLevel() {
        if (logLevel == null) {
            return null;
        }
        for (CommonsLogLevel commonsLogLevel : CommonsLogLevel.values()) {
            if (commonsLogLevel.name().equals(logLevel)) {
                return commonsLogLevel;
            }
        }
        throw new IllegalArgumentException("Unresolved log level " + logLevel + " for apache commons logger, " +
                "known levels " + Arrays.toString(CommonsLogLevel.values()));
    }

    public DataSourceProxyLogging getLogging() {
        return logging;
    }

    public void setLogging(DataSourceProxyLogging logging) {
        this.logging = logging;
    }

    public String getLogLevel() {
        return logLevel;
    }

    public void setLogLevel(String logLevel) {
        this.logLevel = logLevel;
    }

    public String getLoggerName() {
        return loggerName;
    }

    public void setLoggerName(String loggerName) {
        this.loggerName = loggerName;
    }

    public boolean isLogQuery() {
        return logQuery;
    }

    public void setLogQuery(boolean logQuery) {
        this.logQuery = logQuery;
    }

    public boolean isLogSlowQuery() {
        return logSlowQuery;
    }

    public void setLogSlowQuery(boolean logSlowQuery) {
        this.logSlowQuery = logSlowQuery;
    }

    public long getSlowQueryThreshold() {
        return slowQueryThreshold;
    }

    public void setSlowQueryThreshold(long slowQueryThreshold) {
        this.slowQueryThreshold = slowQueryThreshold;
    }

    public boolean isMultiline() {
        return multiline;
    }

    public void setMultiline(boolean multiline) {
        this.multiline = multiline;
    }

    public boolean isJsonFormat() {
        return jsonFormat;
    }

    public void setJsonFormat(boolean jsonFormat) {
        this.jsonFormat = jsonFormat;
    }

    public boolean isCountQuery() {
        return countQuery;
    }

    public void setCountQuery(boolean countQuery) {
        this.countQuery = countQuery;
    }

    public enum DataSourceProxyLogging {
        SYSOUT,
        SLF4j,
        COMMONS,
        JUL
    }
}
