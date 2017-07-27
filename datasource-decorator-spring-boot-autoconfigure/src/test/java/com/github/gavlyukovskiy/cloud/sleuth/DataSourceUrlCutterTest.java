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

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class DataSourceUrlCutterTest {

    @Test
    public void testShouldTrimJdbcByDefault() throws Exception {
        String name = DataSourceUrlCutter.shorten("jdbc:h2:mem:testdb/test");

        assertThat(name).isEqualTo("h2:mem:testdb/test");
    }

    @Test
    public void testShouldTrimDbPortIfTooLong() throws Exception {
        String name = DataSourceUrlCutter.shorten("jdbc:h2:mem:very-long-database-name:3333/test");

        assertThat(name).isEqualTo("h2:mem:very-long-database-name/test");
    }

    @Test
    public void testShouldNotFailIfPortNotFound() throws Exception {
        String name = DataSourceUrlCutter.shorten("jdbc:h2:mem:very-very-long-database-name/test");

        assertThat(name).isEqualTo("h2:mem:very-very-long-database-name/test");
    }
}