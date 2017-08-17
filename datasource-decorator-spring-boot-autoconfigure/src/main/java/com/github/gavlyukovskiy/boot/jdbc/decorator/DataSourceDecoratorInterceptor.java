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

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.RawTargetAccess;

import javax.sql.DataSource;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Interceptor that delegates all method calls to the decorated {@link DataSource}.
 * As well handles methods from {@link DecoratedDataSource}.
 *
 * @author Arthur Gavlyukovskiy
 * @since 1.2.2
 */
class DataSourceDecoratorInterceptor implements MethodInterceptor {

    private final DataSource realDataSource;
    private final DataSource decoratedDataSource;
    private final List<DecoratedDataSourceChainEntry> decoratingChain;

    DataSourceDecoratorInterceptor(DataSource realDataSource, DataSource decoratedDataSource, List<DecoratedDataSourceChainEntry> decoratingChain) {
        this.realDataSource = realDataSource;
        this.decoratedDataSource = decoratedDataSource;
        this.decoratingChain = decoratingChain;
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        if (invocation.getMethod().getName().equals("getConnection")) {
            if (invocation.getMethod().getParameterCount() == 0) {
                return decoratedDataSource.getConnection();
            }
            else if (invocation.getMethod().getParameterCount() == 2) {
                return decoratedDataSource.getConnection((String) invocation.getArguments()[0], (String) invocation.getArguments()[1]);
            }
        }
        if (invocation.getMethod().getName().equals("toString")) {
            return decoratingChain.stream()
                    .map(entry -> entry.getBeanName() + " [" + entry.getDataSource().getClass().getName() + "]")
                    .collect(Collectors.joining(" -> "));
        }
        if (invocation.getMethod().getDeclaringClass() == DecoratedDataSource.class) {
            if (invocation.getMethod().getName().equals("getRealDataSource")) {
                return realDataSource;
            }
            if (invocation.getMethod().getName().equals("getDecoratedDataSource")) {
                return decoratedDataSource;
            }
            if (invocation.getMethod().getName().equals("getDecoratingChain")) {
                return Collections.unmodifiableList(decoratingChain);
            }
        }
        return invocation.proceed();
    }
}
