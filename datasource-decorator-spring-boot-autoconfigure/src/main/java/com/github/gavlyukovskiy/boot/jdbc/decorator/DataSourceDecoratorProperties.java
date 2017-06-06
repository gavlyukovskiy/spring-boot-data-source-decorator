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

import com.github.gavlyukovskiy.boot.jdbc.decorator.dsproxy.DataSourceProxyProperties;
import com.github.gavlyukovskiy.boot.jdbc.decorator.p6spy.P6SpyProperties;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import java.util.Collection;
import java.util.Collections;

@Getter
@Setter
@ConfigurationProperties(prefix = "spring.datasource.decorator")
public class DataSourceDecoratorProperties {

    private boolean enabled = true;
    private Collection<String> excludeBeans = Collections.emptyList();

    @NestedConfigurationProperty
    private DataSourceProxyProperties dataSourceProxy = new DataSourceProxyProperties();

    @NestedConfigurationProperty
    private P6SpyProperties p6spy = new P6SpyProperties();
}
