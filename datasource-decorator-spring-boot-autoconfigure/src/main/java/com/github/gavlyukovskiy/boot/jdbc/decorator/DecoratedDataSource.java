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

import org.springframework.aop.RawTargetAccess;

import javax.sql.DataSource;

import java.util.List;

/**
 * Interface that implicitly added to the CGLIB proxy of {@link DataSource}.
 *
 * Returns link of both real {@link DataSource}, decorated {@link DataSource}
 * and all decorating chain including decorator bean name, instance and result of decorating.
 *
 * @see DataSourceDecoratorInterceptor
 *
 * @author Arthur Gavlyukovskiy
 * @since 1.2.2
 */
public interface DecoratedDataSource extends RawTargetAccess {

    /**
     * Returns data source bean name.
     *
     * @return data source bean name
     */
    String getBeanName();

    /**
     * Returns initial data source, before applying any decorator.
     *
     * @return initial data source
     */
    DataSource getRealDataSource();

    /**
     * Returns data source resulted {@link DataSourceDecoratorInterceptor}.
     *
     * @return decorated data source
     */
    DataSource getDecoratedDataSource();

    /**
     * Returns list with all decorators applied on a {@link DataSource} reverse ordered with applying order.
     *
     * @return decorating information chain
     */
    List<DataSourceDecorationStage> getDecoratingChain();
}
