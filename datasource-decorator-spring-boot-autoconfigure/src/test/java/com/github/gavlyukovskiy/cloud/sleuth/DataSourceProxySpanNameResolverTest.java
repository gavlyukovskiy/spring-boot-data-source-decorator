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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DataSourceProxySpanNameResolverTest {

    private DataSourceProxySpanNameResolver resolver = new DataSourceProxySpanNameResolver();
    @Mock
    private ExecutionInfo executionInfo;

    @Test
    public void testShouldReturnQuerySpanNameFromDataSourceName() throws Exception {
        Mockito.when(executionInfo.getDataSourceName()).thenReturn("myDs");
        String querySpanName = resolver.querySpanName(executionInfo);

        Assertions.assertThat(querySpanName).isEqualTo("jdbc:/myDs/query");
    }

    @Test
    public void testShouldReturnQuerySpanNameUsingDefault() throws Exception {
        String querySpanName = resolver.querySpanName(executionInfo);

        Assertions.assertThat(querySpanName).isEqualTo("jdbc:/dataSource/query");
    }
}