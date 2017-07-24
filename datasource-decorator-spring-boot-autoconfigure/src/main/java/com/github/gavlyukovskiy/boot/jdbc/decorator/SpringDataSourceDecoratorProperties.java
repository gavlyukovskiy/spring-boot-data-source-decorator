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

package com.github.gavlyukovskiy.boot.jdbc.decorator;

import com.github.gavlyukovskiy.boot.jdbc.decorator.dsproxy.DataSourceProxyProperties.DataSourceProxyLogging;
import com.github.gavlyukovskiy.boot.jdbc.decorator.p6spy.P6SpyProperties.P6SpyLogging;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Collection;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Deprecated in favor to not use spring-boot reserved namespace, please use properties without 'spring.' prefix.
 *
 * @author Arthur Gavlyukovskiy
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "spring.datasource.decorator")
@Deprecated
public class SpringDataSourceDecoratorProperties {

    private static final Logger log = getLogger(SpringDataSourceDecoratorProperties.class);

    private Boolean enabled;
    private Collection<String> excludeBeans;

    @Deprecated
    private SpringDataSourceProxyProperties datasourceProxy = new SpringDataSourceProxyProperties();

    @Deprecated
    private SpringP6SpyProperties p6spy = new SpringP6SpyProperties();

    @Deprecated
    private SpringFlexyPoolProperties flexyPool = new SpringFlexyPoolProperties();

    void replacePropertiesAndWarn(DataSourceDecoratorProperties dataSourceDecoratorProperties) {
        if (enabled != null) {
            logDeprecatedProperty("datasource.decorator.enabled");
            dataSourceDecoratorProperties.setEnabled(enabled);
        }
        if (excludeBeans != null) {
            logDeprecatedProperty("datasource.decorator.exclude-beans");
            dataSourceDecoratorProperties.setExcludeBeans(excludeBeans);
        }

        if (p6spy.getEnableRuntimeListeners() != null) {
            logDeprecatedProperty("datasource.decorator.p6spy.enable-runtime-listeners");
            dataSourceDecoratorProperties.getP6spy().setEnableRuntimeListeners(!p6spy.getEnableRuntimeListeners());
        }
        if (p6spy.getMultiline() != null) {
            logDeprecatedProperty("datasource.decorator.p6spy.multiline");
            dataSourceDecoratorProperties.getP6spy().setMultiline(!p6spy.getMultiline());
        }
        if (p6spy.getLogging() != null) {
            logDeprecatedProperty("datasource.decorator.p6spy.logging");
            dataSourceDecoratorProperties.getP6spy().setLogging(p6spy.getLogging());
        }
        if (p6spy.getLogFile() != null) {
            logDeprecatedProperty("datasource.decorator.p6spy.log-file");
            dataSourceDecoratorProperties.getP6spy().setLogFile(p6spy.getLogFile());
        }

        if (datasourceProxy.getLogging() != null) {
            logDeprecatedProperty("datasource.decorator.datasource-proxy.logging");
            dataSourceDecoratorProperties.getDatasourceProxy().setLogging(datasourceProxy.getLogging());
        }
        if (datasourceProxy.getQuery().getEnableLogging() != null) {
            logDeprecatedProperty("datasource.decorator.datasource-proxy.query.enable-logging");
            dataSourceDecoratorProperties.getDatasourceProxy().getQuery().setEnableLogging(datasourceProxy.getQuery().getEnableLogging());
        }
        if (datasourceProxy.getQuery().getLoggerName() != null) {
            logDeprecatedProperty("datasource.decorator.datasource-proxy.query.logger-name");
            dataSourceDecoratorProperties.getDatasourceProxy().getQuery().setLoggerName(datasourceProxy.getQuery().getLoggerName());
        }
        if (datasourceProxy.getQuery().getLogLevel() != null) {
            logDeprecatedProperty("datasource.decorator.datasource-proxy.query.log-level");
            dataSourceDecoratorProperties.getDatasourceProxy().getQuery().setLogLevel(datasourceProxy.getQuery().getLogLevel());
        }
        if (datasourceProxy.getSlowQuery().getEnableLogging() != null) {
            logDeprecatedProperty("datasource.decorator.datasource-proxy.slow-query.enable-logging");
            dataSourceDecoratorProperties.getDatasourceProxy().getSlowQuery().setEnableLogging(datasourceProxy.getSlowQuery().getEnableLogging());
        }
        if (datasourceProxy.getSlowQuery().getLoggerName() != null) {
            logDeprecatedProperty("datasource.decorator.datasource-proxy.slow-query.logger-name");
            dataSourceDecoratorProperties.getDatasourceProxy().getSlowQuery().setLoggerName(datasourceProxy.getSlowQuery().getLoggerName());
        }
        if (datasourceProxy.getSlowQuery().getLogLevel() != null) {
            logDeprecatedProperty("datasource.decorator.datasource-proxy.slow-query.log-level");
            dataSourceDecoratorProperties.getDatasourceProxy().getSlowQuery().setLogLevel(datasourceProxy.getSlowQuery().getLogLevel());
        }
        if (datasourceProxy.getSlowQuery().getThreshold() != null) {
            logDeprecatedProperty("datasource.decorator.datasource-proxy.slow-query.threshold");
            dataSourceDecoratorProperties.getDatasourceProxy().getSlowQuery().setThreshold(datasourceProxy.getSlowQuery().getThreshold());
        }

        if (flexyPool.getAcquiringStrategy().getRetry().getAttempts() != null) {
            logDeprecatedProperty("datasource.decorator.flexy-pool.acquiring-strategy.retry.attempts");
            dataSourceDecoratorProperties.getFlexyPool().getAcquiringStrategy().getRetry().setAttempts(flexyPool.getAcquiringStrategy().getRetry().getAttempts());
        }
        if (flexyPool.getAcquiringStrategy().getIncrementPool().getMaxOverflowPoolSize() != null) {
            logDeprecatedProperty("datasource.decorator.flexy-pool.acquiring-strategy.increment-pool.max-overflow-pool-size");
            dataSourceDecoratorProperties.getFlexyPool().getAcquiringStrategy().getIncrementPool().setMaxOverflowPoolSize(flexyPool.getAcquiringStrategy().getIncrementPool().getMaxOverflowPoolSize());
        }
        if (flexyPool.getAcquiringStrategy().getIncrementPool().getTimeoutMillis() != null) {
            logDeprecatedProperty("datasource.decorator.flexy-pool.acquiring-strategy.increment-pool.timeout-millis");
            dataSourceDecoratorProperties.getFlexyPool().getAcquiringStrategy().getIncrementPool().setTimeoutMillis(flexyPool.getAcquiringStrategy().getIncrementPool().getTimeoutMillis());
        }
        if (flexyPool.getMetrics().getReporter().getLog().getMillis() != null) {
            logDeprecatedProperty("datasource.decorator.flexy-pool.metrics.reporter.log.millis");
            dataSourceDecoratorProperties.getFlexyPool().getMetrics().getReporter().getLog().setMillis(flexyPool.getMetrics().getReporter().getLog().getMillis());
        }
        if (flexyPool.getMetrics().getReporter().getJmx().getEnabled() != null) {
            logDeprecatedProperty("datasource.decorator.flexy-pool.metrics.reporter.jmx.enabled");
            dataSourceDecoratorProperties.getFlexyPool().getMetrics().getReporter().getJmx().setEnabled(flexyPool.getMetrics().getReporter().getJmx().getEnabled());
        }
        if (flexyPool.getMetrics().getReporter().getJmx().getAutoStart() != null) {
            logDeprecatedProperty("datasource.decorator.flexy-pool.metrics.reporter.jmx.auto-start");
            dataSourceDecoratorProperties.getFlexyPool().getMetrics().getReporter().getJmx().setAutoStart(flexyPool.getMetrics().getReporter().getJmx().getAutoStart());
        }
        if (flexyPool.getThreshold().getConnection().getAcquire() != null) {
            logDeprecatedProperty("datasource.decorator.flexy-pool.threshold.connection.acquire");
            dataSourceDecoratorProperties.getFlexyPool().getThreshold().getConnection().setAcquire(flexyPool.getThreshold().getConnection().getAcquire());
        }
        if (flexyPool.getThreshold().getConnection().getLease() != null) {
            logDeprecatedProperty("datasource.decorator.flexy-pool.threshold.connection.lease");
            dataSourceDecoratorProperties.getFlexyPool().getThreshold().getConnection().setLease(flexyPool.getThreshold().getConnection().getLease());
        }
    }

    private void logDeprecatedProperty(String propertyName) {
        log.warn("Property spring." + propertyName + " is deprecated, please use " + propertyName + " instead");
    }

    @Getter
    @Setter
    @Deprecated
    public static class SpringP6SpyProperties {

        private Boolean enableRuntimeListeners;
        private Boolean multiline;
        private P6SpyLogging logging;
        private String logFile;
    }

    @Getter
    @Setter
    @Deprecated
    public static class SpringDataSourceProxyProperties {

        private DataSourceProxyLogging logging;

        private Query query = new Query();
        private SlowQuery slowQuery = new SlowQuery();

        private Boolean multiline;
        private Boolean jsonFormat;
        private Boolean countQuery;

        @Getter
        @Setter
        @Deprecated
        public static class Query {
            /**
             * Deprecated.
             */
            private Boolean enableLogging;
            /**
             * Deprecated.
             */
            private String loggerName;
            /**
             * Deprecated.
             */
            private String logLevel;
        }

        @Getter
        @Setter
        @Deprecated
        public static class SlowQuery {
            /**
             * Deprecated.
             */
            private Boolean enableLogging;
            /**
             * Deprecated.
             */
            private String loggerName;
            /**
             * Deprecated.
             */
            private String logLevel;
            /**
             * Deprecated.
             */
            private Long threshold;
        }
    }

    @Getter
    @Setter
    @Deprecated
    public static class SpringFlexyPoolProperties {

        private AcquiringStrategy acquiringStrategy = new AcquiringStrategy();

        private Metrics metrics = new Metrics();
        private Threshold threshold = new Threshold();

        @Getter
        @Setter
        @Deprecated
        public static class AcquiringStrategy {
            private Retry retry = new Retry();
            private IncrementPool incrementPool = new IncrementPool();

            @Getter
            @Setter
            @Deprecated
            public static class Retry {
                private Integer attempts;
            }

            @Getter
            @Setter
            @Deprecated
            public static class IncrementPool {
                private Integer maxOverflowPoolSize;
                private Integer timeoutMillis;
            }
        }

        @Getter
        @Setter
        @Deprecated
        public static class Metrics {
            private Reporter reporter = new Reporter();

            @Getter
            @Setter
            @Deprecated
            public static class Reporter {
                private Log log = new Log();
                private Jmx jmx = new Jmx();

                @Getter
                @Setter
                @Deprecated
                public static class Log {
                    private Long millis;
                }

                @Getter
                @Setter
                @Deprecated
                public static class Jmx {
                    private Boolean enabled;
                    private Boolean autoStart;
                }
            }
        }

        @Getter
        @Setter
        @Deprecated
        public static class Threshold {
            private Connection connection = new Connection();

            @Getter
            @Setter
            public static class Connection {
                private Long acquire;
                private Long lease;
            }
        }
    }
}
