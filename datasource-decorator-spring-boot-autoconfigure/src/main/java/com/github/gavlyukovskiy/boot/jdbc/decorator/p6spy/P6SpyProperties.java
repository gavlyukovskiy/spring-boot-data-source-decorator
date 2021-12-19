/*
 * Copyright 2021 the original author or authors.
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

package com.github.gavlyukovskiy.boot.jdbc.decorator.p6spy;

import org.springframework.boot.context.properties.DeprecatedConfigurationProperty;

import java.util.regex.Pattern;

/**
 * Properties for configuring p6spy.
 *
 * @author Arthur Gavlyukovskiy
 */
public class P6SpyProperties {

    /**
     * Enables logging JDBC events.
     *
     * @see com.p6spy.engine.logging.P6LogFactory
     */
    private boolean enableLogging = true;
    /**
     * Enables multiline output.
     */
    private boolean multiline = true;
    /**
     * Logging to use for logging queries.
     */
    private P6SpyLogging logging = P6SpyLogging.SLF4J;
    /**
     * Name of log file to use (only with logging=file).
     */
    private String logFile = "spy.log";
    /**
     * Custom log format.
     */
    private String logFormat;

    /**
     * Tracing related properties
     */
    private P6SpyTracing tracing = new P6SpyTracing();

    /**
     * Class file to use (only with logging=custom).
     * The class must implement {@link com.p6spy.engine.spy.appender.FormattedLogger}
     */
    private String customAppenderClass;

    /**
     * Log filtering related properties.
     */
    private P6SpyLogFilter logFilter = new P6SpyLogFilter();

    public boolean isEnableLogging() {
        return this.enableLogging;
    }

    public boolean isMultiline() {
        return this.multiline;
    }

    public P6SpyLogging getLogging() {
        return this.logging;
    }

    public String getLogFile() {
        return this.logFile;
    }

    public String getLogFormat() {
        return this.logFormat;
    }

    public P6SpyTracing getTracing() {
        return this.tracing;
    }

    public String getCustomAppenderClass() {
        return this.customAppenderClass;
    }

    public P6SpyLogFilter getLogFilter() {
        return this.logFilter;
    }

    public void setEnableLogging(boolean enableLogging) {
        this.enableLogging = enableLogging;
    }

    public void setMultiline(boolean multiline) {
        this.multiline = multiline;
    }

    public void setLogging(P6SpyLogging logging) {
        this.logging = logging;
    }

    public void setLogFile(String logFile) {
        this.logFile = logFile;
    }

    public void setLogFormat(String logFormat) {
        this.logFormat = logFormat;
    }

    public void setTracing(P6SpyTracing tracing) {
        this.tracing = tracing;
    }

    public void setCustomAppenderClass(String customAppenderClass) {
        this.customAppenderClass = customAppenderClass;
    }

    public void setLogFilter(P6SpyLogFilter logFilter) {
        this.logFilter = logFilter;
    }

    public enum P6SpyLogging {
        SYSOUT,
        SLF4J,
        FILE,
        CUSTOM
    }

    @Deprecated
    public static class P6SpyTracing {
        /**
         * Report the effective sql string (with '?' replaced with real values) to tracing systems.
         * <p>
         * NOTE this setting does not affect the logging message.
         */
        @Deprecated
        private boolean includeParameterValues = true;

        @Deprecated
        @DeprecatedConfigurationProperty(
                reason = "JDBC instrumentation is provided in Spring Cloud Sleuth",
                replacement = "spring.sleuth.jdbc.p6spy.tracing.include-parameter-values"
        )
        public boolean isIncludeParameterValues() {
            return this.includeParameterValues;
        }

        @Deprecated
        public void setIncludeParameterValues(boolean includeParameterValues) {
            this.includeParameterValues = includeParameterValues;
        }
    }

    public static class P6SpyLogFilter {
        /**
         * Use regex pattern to filter log messages. Only matched messages will be logged.
         */
        private Pattern pattern;

        public Pattern getPattern() {
            return this.pattern;
        }

        public void setPattern(Pattern pattern) {
            this.pattern = pattern;
        }
    }
}
