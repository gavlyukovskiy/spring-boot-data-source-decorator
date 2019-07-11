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

import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;
import java.util.List;

/**
 * Configuration for integration with spring-cloud-sleuth.
 *
 * @author Arthur Gavlyukovskiy
 * @since 1.4.0
 */
@Getter
@Setter
public class SleuthProperties {

    /**
     * Creates span for every connection and query. Works only with p6spy or datasource-proxy.
     */
    private boolean enabled = true;

    private List<TraceType> include = Arrays.asList(TraceType.CONNECTION, TraceType.QUERY, TraceType.FETCH);

    public enum TraceType {
        CONNECTION,
        QUERY,
        FETCH
    }
}
