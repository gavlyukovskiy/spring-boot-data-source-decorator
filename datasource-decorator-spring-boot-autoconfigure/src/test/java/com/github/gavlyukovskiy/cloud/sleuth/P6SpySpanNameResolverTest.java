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

import javax.sql.CommonDataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;

@RunWith(MockitoJUnitRunner.class)
public class P6SpySpanNameResolverTest {

    private P6SpySpanNameResolver resolver = new P6SpySpanNameResolver();
    @Mock
    private CommonDataSource dataSource;
    @Mock
    private ConnectionInformation connectionInformation;
    @Mock
    private Connection connection;
    @Mock
    private StatementInformation statementInformation;
    @Mock
    private DatabaseMetaData databaseMetaData;

    @Before
    public void setup() throws Exception {
        Mockito.when(connectionInformation.getConnection()).thenReturn(connection);
        Mockito.when(statementInformation.getConnectionInformation()).thenReturn(connectionInformation);
        Mockito.when(connectionInformation.getDataSource()).thenReturn(dataSource);
        Mockito.when(connection.getMetaData()).thenReturn(databaseMetaData);
    }

    @Test
    public void testShouldReturnConnectionSpanName() throws Exception {
        Mockito.when(databaseMetaData.getURL()).thenReturn("jdbc:h2:mem:testdb/test");
        String querySpanName = resolver.connectionSpanName(connectionInformation);

        Assertions.assertThat(querySpanName).isEqualTo("h2:mem:testdb/test/connection");
    }

    @Test
    public void testShouldReturnQuerySpanName() throws Exception {
        Mockito.when(databaseMetaData.getURL()).thenReturn("jdbc:h2:mem:testdb/test");
        String querySpanName = resolver.querySpanName(statementInformation);

        Assertions.assertThat(querySpanName).isEqualTo("h2:mem:testdb/test/query");
    }

    @Test
    public void testShouldCacheReturnValue() throws Exception {
        Mockito.when(databaseMetaData.getURL()).thenReturn("jdbc:h2:mem:testdb/test");
        resolver.connectionSpanName(connectionInformation);
        resolver.querySpanName(statementInformation);

        Mockito.verify(connection).getMetaData();
        Mockito.verifyNoMoreInteractions(connection);
    }

}