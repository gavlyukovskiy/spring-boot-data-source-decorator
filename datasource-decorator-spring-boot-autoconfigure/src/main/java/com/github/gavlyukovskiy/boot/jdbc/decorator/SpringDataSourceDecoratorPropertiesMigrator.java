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
import org.slf4j.Logger;
import org.springframework.core.env.Environment;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Migrates deprecated properties from spring-boot's reserved namespace to 'decorator.datasource.*'.
 *
 * @author Arthur Gavlyukovskiy
 */
class SpringDataSourceDecoratorPropertiesMigrator {

    private static final Logger log = getLogger(SpringDataSourceDecoratorPropertiesMigrator.class);

    private final Environment environment;

    SpringDataSourceDecoratorPropertiesMigrator(Environment environment) {
        this.environment = environment;
    }

    @SuppressWarnings("unchecked")
    void replacePropertiesAndWarn(DataSourceDecoratorProperties properties) {
        migrateProperty("enabled", Boolean.class, properties::setEnabled);
        migrateProperty("exclude-beans", Collection.class, properties::setExcludeBeans);

        migrateProperty("p6spy.enable-runtime-listeners", Boolean.class, properties.getP6spy()::setEnableRuntimeListeners);
        migrateProperty("p6spy.multiline", Boolean.class, properties.getP6spy()::setMultiline);
        migrateProperty("p6spy.logging", P6SpyLogging.class, properties.getP6spy()::setLogging);
        migrateProperty("p6spy.log-file", String.class, properties.getP6spy()::setLogFile);

        migrateProperty("datasource-proxy.logging", DataSourceProxyLogging.class, properties.getDatasourceProxy()::setLogging);
        migrateProperty("datasource-proxy.query.enable-logging", Boolean.class, properties.getDatasourceProxy().getQuery()::setEnableLogging);
        migrateProperty("datasource-proxy.query.logger-name", String.class, properties.getDatasourceProxy().getQuery()::setLoggerName);
        migrateProperty("datasource-proxy.query.log-level", String.class, properties.getDatasourceProxy().getQuery()::setLogLevel);
        migrateProperty("datasource-proxy.slow-query.enable-logging", Boolean.class, properties.getDatasourceProxy().getSlowQuery()::setEnableLogging);
        migrateProperty("datasource-proxy.slow-query.logger-name", String.class, properties.getDatasourceProxy().getSlowQuery()::setLoggerName);
        migrateProperty("datasource-proxy.slow-query.log-level", String.class, properties.getDatasourceProxy().getSlowQuery()::setLogLevel);
        migrateProperty("datasource-proxy.slow-query.threshold", Long.class, properties.getDatasourceProxy().getSlowQuery()::setThreshold);

        migrateProperty("flexy-pool.acquiring-strategy.retry.attempts", Integer.class, properties.getFlexyPool().getAcquiringStrategy().getRetry()::setAttempts);
        migrateProperty("flexy-pool.acquiring-strategy.increment-pool.max-overflow-pool-size", Integer.class, properties.getFlexyPool().getAcquiringStrategy().getIncrementPool()::setMaxOverflowPoolSize);
        migrateProperty("flexy-pool.acquiring-strategy.increment-pool.timeout-millis", Integer.class, properties.getFlexyPool().getAcquiringStrategy().getIncrementPool()::setTimeoutMillis);
        migrateProperty("flexy-pool.metrics.reporter.log.millis", Long.class, properties.getFlexyPool().getMetrics().getReporter().getLog()::setMillis);
        migrateProperty("flexy-pool.metrics.reporter.jmx.enabled", Boolean.class, properties.getFlexyPool().getMetrics().getReporter().getJmx()::setEnabled);
        migrateProperty("flexy-pool.metrics.reporter.jmx.auto-start", Boolean.class, properties.getFlexyPool().getMetrics().getReporter().getJmx()::setAutoStart);
        migrateProperty("flexy-pool.threshold.connection.acquire", Long.class, properties.getFlexyPool().getThreshold().getConnection()::setAcquire);
        migrateProperty("flexy-pool.threshold.connection.lease", Long.class, properties.getFlexyPool().getThreshold().getConnection()::setLease);
    }

    private <T> void migrateProperty(String propertyName, Class<T> propertyClass, Consumer<T> propertySetter) {
        T propertyValue = environment.getProperty("spring.datasource.decorator." + propertyName, propertyClass);
        if (propertyValue != null) {
            log.warn("Property spring.datasource.decorator." + propertyName + " is deprecated, please use decorator.datasource." + propertyName + " instead");
            propertySetter.accept(propertyValue);
        }
    }
}
