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

import org.springframework.boot.context.properties.DeprecatedConfigurationProperty;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * Properties for configuring flexy-pool.
 *
 * @author Arthur Gavlyukovskiy
 * @since 1.2
 */
public class FlexyPoolProperties {

    private AcquisitionStrategy acquisitionStrategy = new AcquisitionStrategy();
    private AcquiringStrategy acquiringStrategy = new AcquiringStrategy();

    private Metrics metrics = new Metrics();
    private Threshold threshold = new Threshold();

    public AcquisitionStrategy getAcquisitionStrategy() {
        return acquisitionStrategy;
    }

    public Metrics getMetrics() {
        return this.metrics;
    }

    public Threshold getThreshold() {
        return this.threshold;
    }

    public void setAcquisitionStrategy(AcquisitionStrategy acquisitionStrategy) {
        this.acquisitionStrategy = acquisitionStrategy;
    }

    @Deprecated(since = "1.10.0", forRemoval = true)
    public AcquiringStrategy getAcquiringStrategy() {
        return acquiringStrategy;
    }

    @Deprecated(since = "1.10.0", forRemoval = true)
    public void setAcquiringStrategy(AcquiringStrategy acquiringStrategy) {
        this.acquiringStrategy = acquiringStrategy;
    }

    public void setMetrics(Metrics metrics) {
        this.metrics = metrics;
    }

    public void setThreshold(Threshold threshold) {
        this.threshold = threshold;
    }

    public static class AcquisitionStrategy {
        private Retry retry = new Retry();
        private IncrementPool incrementPool = new IncrementPool();

        public Retry getRetry() {
            return this.retry;
        }

        public IncrementPool getIncrementPool() {
            return this.incrementPool;
        }

        public void setRetry(Retry retry) {
            this.retry = retry;
        }

        public void setIncrementPool(IncrementPool incrementPool) {
            this.incrementPool = incrementPool;
        }

        public static class Retry {
            private int attempts = 2;

            public int getAttempts() {
                return this.attempts;
            }

            public void setAttempts(int attempts) {
                this.attempts = attempts;
            }
        }

        public static class IncrementPool {
            private int maxOvergrowPoolSize = 15;
            private int timeoutMillis = 500;

            public int getMaxOvergrowPoolSize() {
                return this.maxOvergrowPoolSize;
            }

            public int getTimeoutMillis() {
                return this.timeoutMillis;
            }

            public void setMaxOvergrowPoolSize(int maxOvergrowPoolSize) {
                this.maxOvergrowPoolSize = maxOvergrowPoolSize;
            }

            public void setTimeoutMillis(int timeoutMillis) {
                this.timeoutMillis = timeoutMillis;
            }
        }
    }

    @Deprecated(since = "1.10.0", forRemoval = true)
    public static class AcquiringStrategy {
        @Deprecated(since = "1.10.0", forRemoval = true)
        private Retry retry = new Retry();
        @Deprecated(since = "1.10.0", forRemoval = true)
        private IncrementPool incrementPool = new IncrementPool();

        @Deprecated(since = "1.10.0", forRemoval = true)
        public Retry getRetry() {
            return this.retry;
        }

        @Deprecated(since = "1.10.0", forRemoval = true)
        public IncrementPool getIncrementPool() {
            return this.incrementPool;
        }

        @Deprecated(since = "1.10.0", forRemoval = true)
        public void setRetry(Retry retry) {
            this.retry = retry;
        }

        @Deprecated(since = "1.10.0", forRemoval = true)
        public void setIncrementPool(IncrementPool incrementPool) {
            this.incrementPool = incrementPool;
        }

        public static class Retry {
            @Deprecated(since = "1.10.0", forRemoval = true)
            private Integer attempts;

            @Deprecated(since = "1.10.0", forRemoval = true)
            @DeprecatedConfigurationProperty(
                    reason = "FlexyPool 3.0 has renamed this property",
                    replacement = "decorator.datasource.flexy-pool.acquisition-strategy.retry.attempts",
                    since = "1.10.0"
            )
            public Integer getAttempts() {
                return this.attempts;
            }

            @Deprecated(since = "1.10.0", forRemoval = true)
            public void setAttempts(int attempts) {
                this.attempts = attempts;
            }
        }

        public static class IncrementPool {
            @Deprecated(since = "1.10.0", forRemoval = true)
            private Integer maxOverflowPoolSize;
            @Deprecated(since = "1.10.0", forRemoval = true)
            private Integer timeoutMillis;

            @Deprecated(since = "1.10.0", forRemoval = true)
            @DeprecatedConfigurationProperty(
                    reason = "FlexyPool 3.0 has renamed this property",
                    replacement = "decorator.datasource.flexy-pool.acquisition-strategy.increment-pool.timeout-millis",
                    since = "1.10.0"
            )
            public Integer getTimeoutMillis() {
                return this.timeoutMillis;
            }

            @Deprecated(since = "1.10.0", forRemoval = true)
            @DeprecatedConfigurationProperty(
                    reason = "FlexyPool 3.0 has renamed this property",
                    replacement = "decorator.datasource.flexy-pool.acquisition-strategy.increment-pool.max-overgrow-pool-size",
                    since = "1.10.0"
            )
            public Integer getMaxOverflowPoolSize() {
                return this.maxOverflowPoolSize;
            }

            @Deprecated(since = "1.10.0", forRemoval = true)
            public void setTimeoutMillis(int timeoutMillis) {
                this.timeoutMillis = timeoutMillis;
            }

            @Deprecated(since = "1.10.0", forRemoval = true)
            public void setMaxOverflowPoolSize(int maxOverflowPoolSize) {
                this.maxOverflowPoolSize = maxOverflowPoolSize;
            }
        }
    }

    public static class Metrics {
        private Reporter reporter = new Reporter();

        public Reporter getReporter() {
            return this.reporter;
        }

        public void setReporter(Reporter reporter) {
            this.reporter = reporter;
        }

        public static class Reporter {
            private Log log = new Log();
            private Jmx jmx = new Jmx();

            public Log getLog() {
                return this.log;
            }

            public Jmx getJmx() {
                return this.jmx;
            }

            public void setLog(Log log) {
                this.log = log;
            }

            public void setJmx(Jmx jmx) {
                this.jmx = jmx;
            }

            public static class Log {
                private long millis = 300000;

                public long getMillis() {
                    return this.millis;
                }

                public void setMillis(long millis) {
                    this.millis = millis;
                }
            }

            public static class Jmx {
                private boolean enabled = true;
                private boolean autoStart = false;

                public boolean isEnabled() {
                    return this.enabled;
                }

                public boolean isAutoStart() {
                    return this.autoStart;
                }

                public void setEnabled(boolean enabled) {
                    this.enabled = enabled;
                }

                public void setAutoStart(boolean autoStart) {
                    this.autoStart = autoStart;
                }
            }
        }
    }

    public static class Threshold {
        private Connection connection = new Connection();

        public Connection getConnection() {
            return this.connection;
        }

        public void setConnection(Connection connection) {
            this.connection = connection;
        }

        public static class Connection {
            private long acquire = 50L;
            private long lease = 1000L;

            public long getAcquire() {
                return this.acquire;
            }

            public long getLease() {
                return this.lease;
            }

            public void setAcquire(long acquire) {
                this.acquire = acquire;
            }

            public void setLease(long lease) {
                this.lease = lease;
            }
        }
    }
}
