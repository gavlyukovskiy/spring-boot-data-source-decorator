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

package com.github.gavlyukovskiy.boot.jdbc.decorator.flexypool;

import com.github.gavlyukovskiy.boot.jdbc.decorator.DataSourceDecorator;
import com.vladmihalcea.flexypool.FlexyPoolDataSource;
import org.springframework.core.Ordered;

import javax.sql.DataSource;

class FlexyPoolDataSourceDecorator implements DataSourceDecorator, Ordered {

    @Override
    public DataSource decorate(String beanName, DataSource dataSource) {
        try {
            return new FlexyPoolDataSource<>(dataSource);
        }
        catch (Exception e) {
            return dataSource;
        }
    }

    @Override
    public int getOrder() {
        return 10;
    }
}
