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

import org.springframework.boot.context.properties.DeprecatedConfigurationProperty;

import java.util.Arrays;
import java.util.List;

/**
 * Configuration for integration with spring-cloud-sleuth.
 *
 * @author Arthur Gavlyukovskiy
 * @since 1.4.0
 * @deprecated in 1.8.0 in favor of <a href="https://docs.spring.io/spring-cloud-sleuth/docs/3.1.0/reference/html/integrations.html#sleuth-jdbc-integration">Spring Cloud Sleuth: Spring JDBC</a>
 */
@Deprecated
public class SleuthProperties {

    /**
     * Creates span for every connection and query. Works only with p6spy or datasource-proxy.
     */
    @Deprecated
    private boolean enabled = true;

    /**
     * Specify traces that will be created in tracing system.
     */
    @Deprecated
    private List<TraceType> include = Arrays.asList(TraceType.CONNECTION, TraceType.QUERY, TraceType.FETCH);

    @Deprecated
    @DeprecatedConfigurationProperty(
            reason = "JDBC instrumentation is provided in Spring Cloud Sleuth",
            replacement = "spring.sleuth.jdbc.enabled"
    )
    public boolean isEnabled() {
        return this.enabled;
    }

    @Deprecated
    @DeprecatedConfigurationProperty(
            reason = "JDBC instrumentation is provided in Spring Cloud Sleuth",
            replacement = "spring.sleuth.jdbc.includes"
    )
    public List<TraceType> getInclude() {
        return this.include;
    }

    @Deprecated
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Deprecated
    public void setInclude(List<TraceType> include) {
        this.include = include;
    }

    public enum TraceType {
        CONNECTION,
        QUERY,
        FETCH
    }
}
