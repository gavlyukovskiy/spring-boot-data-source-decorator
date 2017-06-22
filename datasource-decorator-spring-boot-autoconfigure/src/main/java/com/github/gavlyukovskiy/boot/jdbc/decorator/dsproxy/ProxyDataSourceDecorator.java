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

package com.github.gavlyukovskiy.boot.jdbc.decorator.dsproxy;

import com.github.gavlyukovskiy.boot.jdbc.decorator.DataSourceDecorator;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.springframework.core.Ordered;

import javax.sql.DataSource;

class ProxyDataSourceDecorator implements DataSourceDecorator, Ordered {
    private final ProxyDataSourceBuilder proxyDataSourceBuilder;

    ProxyDataSourceDecorator(ProxyDataSourceBuilder proxyDataSourceBuilder) {
        this.proxyDataSourceBuilder = proxyDataSourceBuilder;
    }

    @Override
    public DataSource decorate(String beanName, DataSource dataSource) {
        return proxyDataSourceBuilder.dataSource(dataSource).name(beanName).build();
    }

    @Override
    public int getOrder() {
        return 20;
    }
}
