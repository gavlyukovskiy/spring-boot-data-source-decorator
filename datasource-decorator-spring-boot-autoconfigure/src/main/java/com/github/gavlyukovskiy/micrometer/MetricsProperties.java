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

package com.github.gavlyukovskiy.micrometer;

import lombok.Getter;
import lombok.Setter;

import javax.sql.DataSource;

/**
 * Configuration for integration with micrometer.
 *
 * Aligned with HikariCP metrics, but works for all data sources.
 *  - datasource.connections.acquire  - time to acquire connection
 *  - datasource.connections.usage    - time of connection usage, usually transaction time
 *  - datasource.connections.active   - number of currently active connections
 *  - datasource.connections.pending  - number of threads pending for free connection
 *  - datasource.connections.created  - number of total connections acquired
 *  - datasource.connections.failed   - number of total connections acquisition fails
 *
 * @author Arthur Gavlyukovskiy
 * @since 1.4.0
 */
@Getter
@Setter
public class MetricsProperties {

    /**
     * Enables {@link DataSource} metrics.
     */
    private boolean enabled = true;
}
