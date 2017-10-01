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

import com.p6spy.engine.common.ConnectionInformation;
import com.p6spy.engine.common.StatementInformation;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.context.ApplicationContext;

import javax.sql.DataSource;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public class P6SpySpanNameResolverTest {

    private P6SpySpanNameResolver resolver = new P6SpySpanNameResolver();
    @Mock
    private DataSource dataSource;
    @Mock
    private ConnectionInformation connectionInformation;
    @Mock
    private StatementInformation statementInformation;
    @Mock
    private ApplicationContext applicationContext;

    @Before
    public void setup() throws Exception {
        Mockito.when(statementInformation.getConnectionInformation()).thenReturn(connectionInformation);
        Mockito.when(connectionInformation.getDataSource()).thenReturn(dataSource);
        resolver.setApplicationContext(applicationContext);
    }

    @Test
    public void testShouldReturnConnectionSpanNameFromBeanName() throws Exception {
        Map<String, DataSource> dataSources = new HashMap<>();
        dataSources.put("myDs", dataSource);
        Mockito.when(applicationContext.getBeansOfType(DataSource.class)).thenReturn(dataSources);
        resolver.initialize();

        String querySpanName = resolver.connectionSpanName(connectionInformation);

        Assertions.assertThat(querySpanName).isEqualTo("jdbc:/myDs/connection");
    }

    @Test
    public void testShouldReturnQuerySpanNameFromBeanName() throws Exception {
        Map<String, DataSource> dataSources = new HashMap<>();
        dataSources.put("myDs", dataSource);
        Mockito.when(applicationContext.getBeansOfType(DataSource.class)).thenReturn(dataSources);
        resolver.initialize();

        String querySpanName = resolver.querySpanName(statementInformation);

        Assertions.assertThat(querySpanName).isEqualTo("jdbc:/myDs/query");
    }

    @Test
    public void testShouldReturnConnectionSpanNameUsingDefault() throws Exception {
        Mockito.when(applicationContext.getBeansOfType(DataSource.class)).thenReturn(Collections.emptyMap());
        resolver.initialize();

        String querySpanName = resolver.connectionSpanName(connectionInformation);

        Assertions.assertThat(querySpanName).isEqualTo("jdbc:/dataSource/connection");
    }

    @Test
    public void testShouldReturnQuerySpanNameUsingDefault() throws Exception {
        Mockito.when(applicationContext.getBeansOfType(DataSource.class)).thenReturn(Collections.emptyMap());
        resolver.initialize();

        String querySpanName = resolver.querySpanName(statementInformation);

        Assertions.assertThat(querySpanName).isEqualTo("jdbc:/dataSource/query");
    }
}