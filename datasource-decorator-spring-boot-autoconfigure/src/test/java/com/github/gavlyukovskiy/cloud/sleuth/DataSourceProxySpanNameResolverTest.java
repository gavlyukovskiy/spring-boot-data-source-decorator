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

import net.ttddyy.dsproxy.ExecutionInfo;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Statement;

@RunWith(MockitoJUnitRunner.class)
public class DataSourceProxySpanNameResolverTest {

    private DataSourceProxySpanNameResolver resolver = new DataSourceProxySpanNameResolver();
    @Mock
    private ExecutionInfo executionInfo;
    @Mock
    private Connection connection;
    @Mock
    private Statement statement;
    @Mock
    private DatabaseMetaData databaseMetaData;

    @Before
    public void setup() throws Exception {
        Mockito.when(executionInfo.getStatement()).thenReturn(statement);
        Mockito.when(executionInfo.getDataSourceName()).thenReturn("dataSource");
        Mockito.when(statement.getConnection()).thenReturn(connection);
        Mockito.when(connection.getMetaData()).thenReturn(databaseMetaData);
    }

    @Test
    public void testShouldReturnQuerySpanName() throws Exception {
        Mockito.when(databaseMetaData.getURL()).thenReturn("jdbc:h2:mem:testdb/test");
        String querySpanName = resolver.querySpanName(executionInfo);

        Assertions.assertThat(querySpanName).isEqualTo("h2:mem:testdb/test/query");
    }

    @Test
    public void testShouldCacheReturnValue() throws Exception {
        Mockito.when(databaseMetaData.getURL()).thenReturn("jdbc:h2:mem:testdb/test");
        resolver.querySpanName(executionInfo);
        resolver.querySpanName(executionInfo);

        Mockito.verify(connection).getMetaData();
        Mockito.verifyNoMoreInteractions(connection);
    }
}