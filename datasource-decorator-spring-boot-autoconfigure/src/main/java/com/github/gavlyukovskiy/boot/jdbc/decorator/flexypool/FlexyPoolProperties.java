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

package com.github.gavlyukovskiy.boot.jdbc.decorator.flexypool;

import lombok.Getter;
import lombok.Setter;

/**
 * Properties for configuring flexy-pool.
 *
 * @author Arthur Gavlyukovskiy
 * @since 1.2
 */
@Getter
@Setter
public class FlexyPoolProperties {

    private AcquiringStrategy acquiringStrategy = new AcquiringStrategy();

    private Metrics metrics = new Metrics();
    private Threshold threshold = new Threshold();

    @Getter
    @Setter
    public static class AcquiringStrategy {
        private Retry retry = new Retry();
        private IncrementPool incrementPool = new IncrementPool();

        @Getter
        @Setter
        public static class Retry {
            private int attempts = 2;
        }

        @Getter
        @Setter
        public static class IncrementPool {
            private int maxOverflowPoolSize = 15;
            private int timeoutMillis = 500;
        }
    }

    @Getter
    @Setter
    public static class Metrics {
        private Reporter reporter = new Reporter();

        @Getter
        @Setter
        public static class Reporter {
            private Log log = new Log();
            private Jmx jmx = new Jmx();

            @Getter
            @Setter
            public static class Log {
                private long millis = 300000;
            }

            @Getter
            @Setter
            public static class Jmx {
                private boolean enabled = true;
                private boolean autoStart = false;
            }
        }
    }

    @Getter
    @Setter
    public static class Threshold {
        private Connection connection = new Connection();

        @Getter
        @Setter
        public static class Connection {
            private long acquire = 50L;
            private long lease = 1000L;
        }
    }
}
