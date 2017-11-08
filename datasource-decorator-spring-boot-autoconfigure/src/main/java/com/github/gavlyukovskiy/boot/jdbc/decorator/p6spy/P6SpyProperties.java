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

package com.github.gavlyukovskiy.boot.jdbc.decorator.p6spy;

import com.p6spy.engine.logging.P6LogFactory;
import lombok.Getter;
import lombok.Setter;

/**
 * Properties for configuring p6spy.
 *
 * @author Arthur Gavlyukovskiy
 */
@Getter
@Setter
public class P6SpyProperties {

    /**
     * Enables logging JDBC events.
     *
     * @see P6LogFactory
     */
    private boolean enableLogging = true;
    /**
     * Enables multiline output.
     */
    private boolean multiline = true;
    /**
     * Logging to use for logging queries.
     */
    private P6SpyLogging logging = P6SpyLogging.SLF4J;
    /**
     * Name of log file to use (only for logging=file).
     */
    private String logFile = "spy.log";

    public enum P6SpyLogging {
        SYSOUT,
        SLF4J,
        FILE
    }
}
