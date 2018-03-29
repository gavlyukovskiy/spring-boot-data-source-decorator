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
import com.github.gavlyukovskiy.boot.jdbc.decorator.DecoratedDataSource;
import com.github.gavlyukovskiy.boot.jdbc.decorator.HidePackagesClassLoader;
import net.ttddyy.dsproxy.listener.ChainListener;
import net.ttddyy.dsproxy.support.ProxyDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.sleuth.autoconfig.TraceAutoConfiguration;
import org.springframework.cloud.sleuth.log.SleuthLogAutoConfiguration;

import javax.sql.DataSource;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class SleuthProxyDataSourceListenerAutoConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    DataSourceAutoConfiguration.class,
                    DataSourceDecoratorAutoConfiguration.class,
                    TraceAutoConfiguration.class,
                    SleuthLogAutoConfiguration.class,
                    SleuthListenerAutoConfiguration.class,
                    PropertyPlaceholderAutoConfiguration.class
            ))
            .withPropertyValues("spring.datasource.initialization-mode=never",
                    "spring.datasource.url:jdbc:h2:mem:testdb-" + new Random().nextInt())
            .withClassLoader(new HidePackagesClassLoader("com.vladmihalcea.flexypool", "com.p6spy"));

    @Test
    void testAddsDatasourceProxyListener() {
        contextRunner.run(context -> {
            DataSource dataSource = context.getBean(DataSource.class);
            ProxyDataSource proxyDataSource = (ProxyDataSource) ((DecoratedDataSource) dataSource).getDecoratedDataSource();
            ChainListener chainListener = proxyDataSource.getProxyConfig().getQueryListener();
            assertThat(chainListener.getListeners()).extracting("class").contains(TracingQueryExecutionListener.class);
        });
    }

    @Test
    void testDoesNotAddDatasourceProxyListenerIfNoTracer() {
        ApplicationContextRunner contextRunner = this.contextRunner.withPropertyValues("spring.sleuth.enabled:false");

        contextRunner.run(context -> {
            DataSource dataSource = context.getBean(DataSource.class);
            ProxyDataSource proxyDataSource = (ProxyDataSource) ((DecoratedDataSource) dataSource).getDecoratedDataSource();
            ChainListener chainListener = proxyDataSource.getProxyConfig().getQueryListener();
            assertThat(chainListener.getListeners()).extracting("class").doesNotContain(TracingQueryExecutionListener.class);
        });
    }
}