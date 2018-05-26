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

package com.github.gavlyukovskiy.micrometer;

import com.github.gavlyukovskiy.boot.jdbc.decorator.DataSourceDecorator;
import com.zaxxer.hikari.HikariDataSource;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import javax.sql.DataSource;

/**
 * Sets micrometer's {@link MeterRegistry} to the {@link HikariDataSource} in order to use embedded hikari metrics.
 *
 * @author Arthur Gavlyukovskiy
 * @since 1.3.4
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
class HikariMetricsDataSourceDecorator implements DataSourceDecorator {
    private final MeterRegistry meterRegistry;

    public HikariMetricsDataSourceDecorator(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public DataSource decorate(String beanName, DataSource dataSource) {
        if (dataSource instanceof HikariDataSource) {
            ((HikariDataSource) dataSource).setMetricRegistry(meterRegistry);
        }
        return dataSource;
    }
}
