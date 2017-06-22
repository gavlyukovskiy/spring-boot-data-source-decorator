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

import com.github.gavlyukovskiy.boot.jdbc.decorator.DataSourceDecorator;
import com.github.gavlyukovskiy.boot.jdbc.decorator.DataSourceDecoratorProperties;
import com.p6spy.engine.event.JdbcEventListener;
import com.p6spy.engine.logging.P6LogFactory;
import com.p6spy.engine.spy.P6DataSource;
import com.p6spy.engine.spy.P6SpyFactory;
import com.p6spy.engine.spy.option.EnvironmentVariables;
import com.p6spy.engine.spy.option.P6OptionsSource;
import com.p6spy.engine.spy.option.SpyDotProperties;
import com.p6spy.engine.spy.option.SystemProperties;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;

import javax.annotation.PostConstruct;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.slf4j.LoggerFactory.getLogger;

@ConditionalOnClass(P6DataSource.class)
public class P6SpyConfiguration {

    private static final Logger log = getLogger(P6SpyConfiguration.class);

    private static final String DEFAULT_P6SPY_MODULES = Stream.of(P6SpyFactory.class, P6LogFactory.class)
            .map(Class::getName)
            .collect(Collectors.joining(","));

    @Autowired
    private DataSourceDecoratorProperties dataSourceDecoratorProperties;

    @Autowired(required = false)
    private List<JdbcEventListener> listeners;

    @PostConstruct
    public void init() {
        P6SpyProperties p6spy = dataSourceDecoratorProperties.getP6spy();
        Set<String> definedOptions = findDefinedOptions();
        if (listeners != null) {
            if (p6spy.isEnableRuntimeListeners() && !definedOptions.contains("modulelist")) {
                RuntimeListenerSupportFactory.setListeners(listeners);
                System.setProperty("p6spy.config.modulelist", DEFAULT_P6SPY_MODULES + "," + RuntimeListenerSupportFactory.class.getName());
            }
            else {
                log.info("JdbcEventListener(s) " + listeners + " will not be applied since enable-runtime-listeners is false or modulelist is overridden");
            }
        }
        if (p6spy.isMultiline() && !definedOptions.contains("logMessageFormat")) {
            System.setProperty("p6spy.config.logMessageFormat", "com.p6spy.engine.spy.appender.MultiLineFormat");
        }
        if (!definedOptions.contains("appender")) {
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
            }
        }
        if (!definedOptions.contains("logfile")) {
            System.setProperty("p6spy.config.logfile", p6spy.getLogFile());
        }
    }

    private Set<String> findDefinedOptions() {
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
                .flatMap(options -> options.keySet().stream())
                .collect(Collectors.toSet());
    }

    @Bean
    public DataSourceDecorator p6SpyDataSourceDecorator() {
        return new P6SpyDataSourceDecorator();
    }
}
