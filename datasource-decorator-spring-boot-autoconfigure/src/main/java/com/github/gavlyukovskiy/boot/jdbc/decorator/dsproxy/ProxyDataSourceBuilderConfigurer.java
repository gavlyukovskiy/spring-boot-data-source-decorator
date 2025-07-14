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

import net.ttddyy.dsproxy.listener.MethodExecutionListener;
import net.ttddyy.dsproxy.listener.QueryCountStrategy;
import net.ttddyy.dsproxy.listener.QueryExecutionListener;
import net.ttddyy.dsproxy.listener.logging.CommonsLogLevel;
import net.ttddyy.dsproxy.listener.logging.LoggingFilter;
import net.ttddyy.dsproxy.listener.logging.SLF4JLogLevel;
import net.ttddyy.dsproxy.proxy.ResultSetProxyLogicFactory;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import net.ttddyy.dsproxy.transform.ParameterTransformer;
import net.ttddyy.dsproxy.transform.QueryTransformer;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Configurer for {@link ProxyDataSourceBuilder} based on the application context.
 *
 * @author Arthur Gavlyukovskiy
 * @see ProxyDataSourceBuilder
 * @since 1.3.1
 */
public class ProxyDataSourceBuilderConfigurer {

    private static final Logger log = getLogger(ProxyDataSourceBuilderConfigurer.class);

    @Autowired(required = false)
    private QueryCountStrategy queryCountStrategy;

    @Autowired(required = false)
    private List<QueryExecutionListener> listeners;

    @Autowired(required = false)
    private List<MethodExecutionListener> methodExecutionListeners;

    @Autowired(required = false)
    private ParameterTransformer parameterTransformer;

    @Autowired(required = false)
    private QueryTransformer queryTransformer;

    @Autowired(required = false)
    private ResultSetProxyLogicFactory resultSetProxyLogicFactory;

    @Autowired(required = false)
    private ConnectionIdManagerProvider connectionIdManagerProvider;

    @Autowired(required = false)
    ProxyDataSourceBuilder.FormatQueryCallback formatQueryCallback;

    @Autowired(required = false)
    private LoggingFilter loggingFilter;

    public void configure(ProxyDataSourceBuilder proxyDataSourceBuilder, DataSourceProxyProperties datasourceProxy) {
        var query = datasourceProxy.getQuery();
        var slowQuery = datasourceProxy.getSlowQuery();
        switch (datasourceProxy.getLogging()) {
            case SLF4J: {
                if (query.isEnableLogging()) {
                    proxyDataSourceBuilder.logQueryBySlf4j(toSlf4JLogLevel(query.getLogLevel()), query.getLoggerName());
                }
                if (slowQuery.isEnableLogging()) {
                    proxyDataSourceBuilder.logSlowQueryBySlf4j(
                            slowQuery.getThresholdDuration().toMillis(), TimeUnit.MILLISECONDS,
                            toSlf4JLogLevel(slowQuery.getLogLevel()), slowQuery.getLoggerName());
                }
                break;
            }
            case JUL: {
                if (query.isEnableLogging()) {
                    proxyDataSourceBuilder.logQueryByJUL(toJULLogLevel(query.getLogLevel()), query.getLoggerName());
                }
                if (slowQuery.isEnableLogging()) {
                    proxyDataSourceBuilder.logSlowQueryByJUL(
                            slowQuery.getThresholdDuration().toMillis(), TimeUnit.MILLISECONDS,
                            toJULLogLevel(slowQuery.getLogLevel()), slowQuery.getLoggerName());
                }
                break;
            }
            case COMMONS: {
                if (query.isEnableLogging()) {
                    proxyDataSourceBuilder.logQueryByCommons(toCommonsLogLevel(query.getLogLevel()), query.getLoggerName());
                }
                if (slowQuery.isEnableLogging()) {
                    proxyDataSourceBuilder.logSlowQueryByCommons(
                            slowQuery.getThresholdDuration().toMillis(), TimeUnit.MILLISECONDS,
                            toCommonsLogLevel(slowQuery.getLogLevel()), slowQuery.getLoggerName());
                }
                break;
            }
            case SYSOUT: {
                if (query.isEnableLogging()) {
                    proxyDataSourceBuilder.logQueryToSysOut();
                }
                if (slowQuery.isEnableLogging()) {
                    proxyDataSourceBuilder.logSlowQueryToSysOut(
                            slowQuery.getThresholdDuration().toMillis(), TimeUnit.MILLISECONDS);
                }
                break;
            }
        }

        if (datasourceProxy.isMultiline() && datasourceProxy.isJsonFormat()) {
            log.warn("Found opposite multiline and json format, multiline will be used (may depend on library version)");
        }
        if (datasourceProxy.isFormatSql() && datasourceProxy.isJsonFormat()) {
            log.warn("Found opposite format-sql and json format, json format will be used (may depend on library version)");
        }

        if (loggingFilter != null) {
            proxyDataSourceBuilder.loggingFilter(loggingFilter);
        }

        if (datasourceProxy.isMultiline()) {
            proxyDataSourceBuilder.multiline();
        }

        if (!datasourceProxy.isMultiline() && datasourceProxy.isJsonFormat()) {
            proxyDataSourceBuilder.asJson();
        }

        if (!datasourceProxy.isJsonFormat() && datasourceProxy.isFormatSql()) {
            if (formatQueryCallback != null) {
                proxyDataSourceBuilder.formatQuery(formatQueryCallback);
            } else {
                throw new IllegalStateException("'datasource-proxy.format-sql' was set to 'true', but cannot be enabled because no formatter is present in the classpath (neither 'org.hibernate:hibernate-core' nor 'com.github.vertical-blank:sql-formatter').");
            }
        }

        if (datasourceProxy.isCountQuery()) {
            proxyDataSourceBuilder.countQuery(queryCountStrategy);
        }
        if (listeners != null) {
            listeners.forEach(proxyDataSourceBuilder::listener);
        }
        if (methodExecutionListeners != null) {
            methodExecutionListeners.forEach(proxyDataSourceBuilder::methodListener);
        }
        if (parameterTransformer != null) {
            proxyDataSourceBuilder.parameterTransformer(parameterTransformer);
        }
        if (queryTransformer != null) {
            proxyDataSourceBuilder.queryTransformer(queryTransformer);
        }
        if (resultSetProxyLogicFactory != null) {
            proxyDataSourceBuilder.proxyResultSet(resultSetProxyLogicFactory);
        }
        if (connectionIdManagerProvider != null) {
            proxyDataSourceBuilder.connectionIdManager(connectionIdManagerProvider.get());
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
        } catch (IllegalArgumentException e) {
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
}
