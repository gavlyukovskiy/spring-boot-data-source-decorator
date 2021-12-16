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

package com.github.gavlyukovskiy.boot.jdbc.decorator.p6spy;

import com.github.gavlyukovskiy.boot.jdbc.decorator.DataSourceDecoratorProperties;
import com.p6spy.engine.event.JdbcEventListener;
import com.p6spy.engine.logging.P6LogFactory;
import com.p6spy.engine.spy.DefaultJdbcEventListenerFactory;
import com.p6spy.engine.spy.JdbcEventListenerFactory;
import com.p6spy.engine.spy.P6DataSource;
import com.p6spy.engine.spy.P6ModuleManager;
import com.p6spy.engine.spy.P6SpyFactory;
import com.p6spy.engine.spy.option.EnvironmentVariables;
import com.p6spy.engine.spy.option.P6OptionsSource;
import com.p6spy.engine.spy.option.SpyDotProperties;
import com.p6spy.engine.spy.option.SystemProperties;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Configuration for integration with p6spy, allows to define custom {@link JdbcEventListener}.
 *
 * @author Arthur Gavlyukovskiy
 */
// P6Spy is automatically configured by Spring Cloud Sleuth if exists
@ConditionalOnMissingBean(type = "org.springframework.cloud.sleuth.autoconfig.instrument.jdbc.P6SpyConfiguration")
@ConditionalOnClass(P6DataSource.class)
public class P6SpyConfiguration {

    private static final Logger log = getLogger(P6SpyConfiguration.class);

    @Autowired
    private DataSourceDecoratorProperties dataSourceDecoratorProperties;

    @Autowired(required = false)
    private List<JdbcEventListener> listeners;

    private Map<String, String> initialP6SpyOptions;

    @PostConstruct
    public void init() {
        P6SpyProperties p6spy = dataSourceDecoratorProperties.getP6spy();
        initialP6SpyOptions = findDefinedOptions();
        String customModuleList = initialP6SpyOptions.get("modulelist");
        if (customModuleList != null) {
            log.info("P6Spy modulelist is overridden, some p6spy configuration features will not be applied");
        }
        else {
            List<String> moduleList = new ArrayList<>();
            // default factory, holds P6Spy configuration
            moduleList.add(P6SpyFactory.class.getName());
            if (p6spy.isEnableLogging()) {
                moduleList.add(P6LogFactory.class.getName());
            }
            System.setProperty("p6spy.config.modulelist", String.join(",", moduleList));
        }
        if (!initialP6SpyOptions.containsKey("logMessageFormat")) {
            if (p6spy.getLogFormat() != null) {
                System.setProperty("p6spy.config.logMessageFormat", "com.p6spy.engine.spy.appender.CustomLineFormat");
                System.setProperty("p6spy.config.customLogMessageFormat", p6spy.getLogFormat());
            }
            else if (p6spy.isMultiline()) {
                System.setProperty("p6spy.config.logMessageFormat", "com.p6spy.engine.spy.appender.MultiLineFormat");
            }
        }
        if (p6spy.isEnableLogging() && !initialP6SpyOptions.containsKey("appender")) {
            switch (p6spy.getLogging()) {
                case SYSOUT:
                    System.setProperty("p6spy.config.appender", "com.p6spy.engine.spy.appender.StdoutLogger");
                    break;
                case SLF4J:
                    System.setProperty("p6spy.config.appender", "com.p6spy.engine.spy.appender.Slf4JLogger");
                    break;
                case FILE:
                    System.setProperty("p6spy.config.appender", "com.p6spy.engine.spy.appender.FileLogger");
                    break;
                case CUSTOM:
                    System.setProperty("p6spy.config.appender", p6spy.getCustomAppenderClass());
                    break;
            }
        }
        if (!initialP6SpyOptions.containsKey("logfile")) {
            System.setProperty("p6spy.config.logfile", p6spy.getLogFile());
        }
        if (p6spy.getLogFilter().getPattern() != null) {
            System.setProperty("p6spy.config.filter", "true");
            System.setProperty("p6spy.config.sqlexpression", p6spy.getLogFilter().getPattern().pattern());
        }
        // If factories were loaded before this method is initialized changing properties will not be applied
        // Changes done in this method could not override anything user specified, therefore it is safe to call reload
        P6ModuleManager.getInstance().reload();
    }

    @PreDestroy
    public void destroy() {
        P6SpyProperties p6spy = dataSourceDecoratorProperties.getP6spy();
        if (!initialP6SpyOptions.containsKey("modulelist")) {
            System.clearProperty("p6spy.config.modulelist");
        }
        if (!initialP6SpyOptions.containsKey("logMessageFormat")) {
            if (p6spy.getLogFormat() != null) {
                System.clearProperty("p6spy.config.logMessageFormat");
                System.clearProperty("p6spy.config.customLogMessageFormat");
            }
            else if (p6spy.isMultiline()) {
                System.clearProperty("p6spy.config.logMessageFormat");
            }
        }
        if (!initialP6SpyOptions.containsKey("appender")) {
            System.clearProperty("p6spy.config.appender");
        }
        if (!initialP6SpyOptions.containsKey("logfile")) {
            System.clearProperty("p6spy.config.logfile");
        }
        P6ModuleManager.getInstance().reload();
    }

    private Map<String, String> findDefinedOptions() {
        SpyDotProperties spyDotProperties = null;
        try {
            spyDotProperties = new SpyDotProperties();
        }
        catch (IOException ignored) {
        }
        return Stream.of(spyDotProperties, new EnvironmentVariables(), new SystemProperties())
                .filter(Objects::nonNull)
                .map(P6OptionsSource::getOptions)
                .filter(Objects::nonNull)
                .flatMap(options -> options.entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        // always using value from the first P6OptionsSource
                        (value1, value2) -> value1));
    }

    @Bean
    @ConditionalOnMissingBean
    public JdbcEventListenerFactory jdbcEventListenerFactory() {
        JdbcEventListenerFactory jdbcEventListenerFactory = new DefaultJdbcEventListenerFactory();
        return listeners != null ? new ContextJdbcEventListenerFactory(jdbcEventListenerFactory, listeners) : jdbcEventListenerFactory;
    }

    @Bean
    public P6SpyDataSourceDecorator p6SpyDataSourceDecorator(JdbcEventListenerFactory jdbcEventListenerFactory) {
        return new P6SpyDataSourceDecorator(jdbcEventListenerFactory);
    }
}
