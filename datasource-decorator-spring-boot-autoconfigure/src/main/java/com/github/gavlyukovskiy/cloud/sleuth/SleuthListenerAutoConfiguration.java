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

import com.github.gavlyukovskiy.boot.jdbc.decorator.DataSourceDecoratorAutoConfiguration;
import com.github.gavlyukovskiy.boot.jdbc.decorator.dsproxy.ProxyDataSourceDecorator;
import com.github.gavlyukovskiy.boot.jdbc.decorator.p6spy.P6SpyDataSourceDecorator;
import com.github.gavlyukovskiy.boot.jdbc.decorator.p6spy.P6SpyDataSourceNameResolver;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.cloud.sleuth.autoconfig.TraceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for integration with spring-cloud-sleuth.
 *
 * @author Arthur Gavlyukovskiy
 * @since 1.2
 */
@Configuration
@ConditionalOnClass(Tracer.class)
@ConditionalOnBean(Tracer.class)
@AutoConfigureAfter({ TraceAutoConfiguration.class, DataSourceDecoratorAutoConfiguration.class })
public class SleuthListenerAutoConfiguration {

    public static final String SPAN_SQL_QUERY_TAG_NAME = "sql";
    public static final String SPAN_ROW_COUNT_TAG_NAME = "row-count";
    public static final String SPAN_CONNECTION_POSTFIX = "/connection";
    public static final String SPAN_QUERY_POSTFIX = "/query";

    @Configuration
    @ConditionalOnBean(P6SpyDataSourceDecorator.class)
    static class P6SpyConfiguration {

        @Bean
        public TracingJdbcEventListener tracingJdbcEventListener(Tracer tracer, P6SpyDataSourceNameResolver p6SpyDataSourceNameResolver) {
            return new TracingJdbcEventListener(tracer, p6SpyDataSourceNameResolver);
        }
    }

    @Configuration
    @ConditionalOnBean(ProxyDataSourceDecorator.class)
    @ConditionalOnMissingBean(P6SpyConfiguration.class)
    static class ProxyDataSourceConfiguration {

        @Bean
        public TracingQueryExecutionListener tracingQueryExecutionListener(Tracer tracer) {
            return new TracingQueryExecutionListener(tracer);
        }
    }
}
