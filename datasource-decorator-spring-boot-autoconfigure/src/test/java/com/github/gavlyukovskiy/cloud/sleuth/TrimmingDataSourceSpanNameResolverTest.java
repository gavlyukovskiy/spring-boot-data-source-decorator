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

import org.junit.Before;
import org.junit.Test;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TrimmingDataSourceSpanNameResolverTest {

    private TrimmingDataSourceSpanNameResolver resolver = new TrimmingDataSourceSpanNameResolver();

    private Connection connection;
    private DatabaseMetaData databaseMetaData;

    @Before
    public void before() throws SQLException {
        connection = mock(Connection.class);
        databaseMetaData = mock(DatabaseMetaData.class);
        when(connection.getMetaData()).thenReturn(databaseMetaData);
    }

    @Test
    public void shouldTrimJdbcByDefault() throws Exception {
        when(databaseMetaData.getURL()).thenReturn("jdbc:h2:mem:testdb/test");
        String name = resolver.resolveName(connection);

        assertThat(name).isEqualTo("h2:mem:testdb/test");
    }

    @Test
    public void shouldTrimDbPortIfTooLong() throws Exception {
        when(databaseMetaData.getURL()).thenReturn("jdbc:h2:mem:very-long-database-name:3333/test");
        String name = resolver.resolveName(connection);

        assertThat(name).isEqualTo("h2:mem:very-long-database-name/test");
    }

    @Test
    public void shouldNotFailIfPortNotFound() throws Exception {
        when(databaseMetaData.getURL()).thenReturn("jdbc:h2:mem:very-very-long-database-name/test");
        String name = resolver.resolveName(connection);

        assertThat(name).isEqualTo("h2:mem:very-very-long-database-name/test");
    }
}