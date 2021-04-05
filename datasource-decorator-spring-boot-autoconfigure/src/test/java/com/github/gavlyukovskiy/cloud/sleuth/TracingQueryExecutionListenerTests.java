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

package com.github.gavlyukovskiy.cloud.sleuth;

import com.github.gavlyukovskiy.boot.jdbc.decorator.DataSourceDecoratorAutoConfiguration;
import com.github.gavlyukovskiy.boot.jdbc.decorator.HidePackagesClassLoader;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.sleuth.autoconfig.brave.BraveAutoConfiguration;

import java.util.concurrent.ThreadLocalRandom;

class TracingQueryExecutionListenerTests extends TracingListenerStrategyTests {

    TracingQueryExecutionListenerTests() {
        super(new ApplicationContextRunner()
                      .withConfiguration(AutoConfigurations.of(
                              DataSourceAutoConfiguration.class,
                              DataSourceDecoratorAutoConfiguration.class,
                              BraveAutoConfiguration.class,
                              SleuthListenerAutoConfiguration.class,
                              TestSpanHandlerConfiguration.class,
                              PropertyPlaceholderAutoConfiguration.class
                      ))
                      .withPropertyValues("spring.datasource.initialization-mode=never",
                                          "spring.datasource.url:jdbc:h2:mem:testdb-" + ThreadLocalRandom.current().nextInt(),
                                          "spring.datasource.hikari.pool-name=test")
                      .withClassLoader(new HidePackagesClassLoader("com.vladmihalcea.flexypool", "com.p6spy")));
    }
}
