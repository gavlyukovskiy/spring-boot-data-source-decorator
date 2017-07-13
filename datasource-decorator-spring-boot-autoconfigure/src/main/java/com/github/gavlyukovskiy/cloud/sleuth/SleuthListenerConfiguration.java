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

package com.github.gavlyukovskiy.cloud.sleuth;

import com.github.gavlyukovskiy.boot.jdbc.decorator.dsproxy.ProxyDataSourceDecorator;
import com.github.gavlyukovskiy.boot.jdbc.decorator.p6spy.P6DataSourceDecorator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.context.annotation.Bean;

/**
 * Listeners configuration for p6spy and datasource-proxy.
 *
 * @author Arthur Gavlyukovskiy
 * @since 1.2
 */
class SleuthListenerConfiguration {

    @ConditionalOnBean(P6DataSourceDecorator.class)
    static class P6SpyConfiguration {

        @Bean
        public TracingJdbcEventListener tracingJdbcEventListener(Tracer tracer) {
            return new TracingJdbcEventListener(tracer);
        }
    }

    @ConditionalOnBean(ProxyDataSourceDecorator.class)
    @ConditionalOnMissingBean(P6SpyConfiguration.class)
    static class ProxyDataSourceConfiguration {

        @Bean
        public TracingQueryExecutionListener tracingQueryExecutionListener(Tracer tracer) {
            return new TracingQueryExecutionListener(tracer);
        }
    }
}
