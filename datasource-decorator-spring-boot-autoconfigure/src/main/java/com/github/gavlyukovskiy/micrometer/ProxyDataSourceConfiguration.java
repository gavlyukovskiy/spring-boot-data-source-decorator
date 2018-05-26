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

import com.github.gavlyukovskiy.boot.jdbc.decorator.dsproxy.ProxyDataSourceDecorator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for datasource-proxy metrics listener.
 *
 * @author Arthur Gavlyukovskiy
 * @since 1.3.0
 */
@Configuration
@ConditionalOnBean(ProxyDataSourceDecorator.class)
@ConditionalOnMissingBean(P6SpyConfiguration.class)
class ProxyDataSourceConfiguration {

    @Bean
    public DataSourceMetricsBinder dataSourceMetricsBinder(ApplicationContext applicationContext) {
        return new DataSourceMetricsBinder(applicationContext);
    }

    @Bean
    public DataSourceMetricsMethodExecutionListener dataSourceMetricsMethodExecutionListener(DataSourceMetricsBinder dataSourceMetricsBinder) {
        return new DataSourceMetricsMethodExecutionListener(dataSourceMetricsBinder);
    }
}
