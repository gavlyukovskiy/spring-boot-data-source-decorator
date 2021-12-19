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

import brave.Tracer;
import com.github.gavlyukovskiy.boot.jdbc.decorator.DataSourceDecoratorAutoConfiguration;
import com.github.gavlyukovskiy.boot.jdbc.decorator.DataSourceDecoratorProperties;
import com.github.gavlyukovskiy.boot.jdbc.decorator.DataSourceNameResolver;
import com.github.gavlyukovskiy.boot.jdbc.decorator.dsproxy.ProxyDataSourceDecorator;
import com.github.gavlyukovskiy.boot.jdbc.decorator.p6spy.P6SpyDataSourceDecorator;
import net.ttddyy.dsproxy.proxy.ResultSetProxyLogicFactory;
import net.ttddyy.dsproxy.proxy.SimpleResultSetProxyLogicFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Deprecation note:
 *
 * <p>For Spring Boot users, that DO NOT use Spring Cloud Sleuth:
 * <p>
 * Nothing has changed, this project is continued to be supported and maintained, and can be used to enable JDBC
 * logging and provide auto-configuration of P6Spy, Datasource-Proxy and FlexyPool.
 * </p>
 *
 * <p>For Spring Cloud Sleuth users:
 * <p>
 * As of release 1.8.0 the "sleuth part" of this project was deprecated in favor of
 * <a href="https://docs.spring.io/spring-cloud-sleuth/docs/3.1.0/reference/html/integrations.html#sleuth-jdbc-integration">Spring Cloud Sleuth: Spring JDBC</a>
 * which provides JDBC instrumentation out of the box. Spring Cloud Sleuth JDBC was based on this project and keeps all
 * functionality including logging, tracing, configuration and customizations.
 * </p>
 *
 * <p>Migration process:
 * <ol>
 * <li>If you are using Spring Cloud Sleuth you can migrate all properties from {@code decorator.datasource.*}
 * to {@code spring.sleuth.jdbc.*} with minimal changes.</li>
 * <li>If you have query logging enabled (default state) then you need to explicitly enable logging using:
 *   <ul>
 *   <li>{@code P6Spy}: {@code spring.sleuth.jdbc.p6spy.enable-logging=true}</li>
 *   <li>{@code Datasource-Proxy}: {@code spring.sleuth.jdbc.datasource-proxy.query.enable-logging=true}</li>
 *   </ul>
 * </li>
 * <li>If you were using decoration customizers please consult with Spring Cloud Sleuth documentation and migrate usage
 * of those to appropriate alternatives in Spring Cloud Sleuth</li>
 * <li>(Optional) Replace dependency on this starter with the particular library.
 *   <ul>
 *   <li>{@code P6Spy}: replace {@code com.github.gavlyukovskiy:p6spy-spring-boot-starter} with {@code p6spy:p6spy}</li>
 *   <li>{@code Datasource-Proxy}: replace {@code com.github.gavlyukovskiy:datasource-proxy-spring-boot-starter} with {@code net.ttddyy:datasource-proxy}</li>
 *   </ul>
 * </li>
 * <li>Any issues can be raised in <a href="https://github.com/spring-cloud/spring-cloud-sleuth">Spring Cloud Sleuth</a>
 * project on GitHub, you may tag me (@gavlyukovskiy) and I'll try to help.</li>
 * <li>Enjoy using JDBC instrumentation, and thank you for using this library :)</li>
 * </ol>
 *
 * <p>
 * Due to similarities in implementation, using starters from this library together with Spring Cloud Sleuth 3.1.0
 * is possible, although decoration will be automatically disabled in favor of Spring Cloud Sleuth to avoid duplicated
 * logging, tracing or any other potential issues.
 * </p>
 *
 * @author Arthur Gavlyukovskiy
 * @since 1.2
 * @deprecated in 1.8.0 in favor of <a href="https://docs.spring.io/spring-cloud-sleuth/docs/3.1.0/reference/html/integrations.html#sleuth-jdbc-integration">Spring Cloud Sleuth: Spring JDBC</a>
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(Tracer.class)
@ConditionalOnBean(Tracer.class)
@ConditionalOnProperty(name = "decorator.datasource.sleuth.enabled", havingValue = "true", matchIfMissing = true)
// JDBC tracing auto-configuration from Spring Cloud Sleuth 3.1.0
@ConditionalOnMissingBean(type = "org.springframework.cloud.sleuth.autoconfig.instrument.jdbc.TraceJdbcAutoConfiguration")
@AutoConfigureAfter(
        value = DataSourceDecoratorAutoConfiguration.class,
        name = {
                "org.springframework.cloud.sleuth.autoconfig.brave.BraveAutoConfiguration",
                "org.springframework.cloud.sleuth.autoconfig.TraceAutoConfiguration",
                "org.springframework.cloud.sleuth.autoconfig.instrument.jdbc.TraceJdbcAutoConfiguration"
        })
@Deprecated
public class SleuthListenerAutoConfiguration {

    public static final String SPAN_SQL_QUERY_TAG_NAME = "sql";
    public static final String SPAN_ROW_COUNT_TAG_NAME = "row-count";
    public static final String SPAN_CONNECTION_POSTFIX = "/connection";
    public static final String SPAN_QUERY_POSTFIX = "/query";
    public static final String SPAN_FETCH_POSTFIX = "/fetch";

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnBean(P6SpyDataSourceDecorator.class)
    static class P6SpyConfiguration {

        @Autowired
        private DataSourceDecoratorProperties dataSourceDecoratorProperties;

        @Bean
        public TracingJdbcEventListener tracingJdbcEventListener(Tracer tracer, DataSourceNameResolver dataSourceNameResolver) {
            return new TracingJdbcEventListener(tracer, dataSourceNameResolver,
                    dataSourceDecoratorProperties.getSleuth().getInclude(),
                    dataSourceDecoratorProperties.getP6spy().getTracing().isIncludeParameterValues());
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnBean(ProxyDataSourceDecorator.class)
    @ConditionalOnMissingBean(P6SpyConfiguration.class)
    static class ProxyDataSourceConfiguration {

        @Autowired
        private DataSourceDecoratorProperties dataSourceDecoratorProperties;

        @Bean
        @ConditionalOnMissingBean
        public ResultSetProxyLogicFactory resultSetProxyLogicFactory() {
            return new SimpleResultSetProxyLogicFactory();
        }

        @Bean
        public TracingQueryExecutionListener tracingQueryExecutionListener(Tracer tracer) {
            return new TracingQueryExecutionListener(tracer, dataSourceDecoratorProperties.getSleuth().getInclude());
        }
    }
}
