package com.github.gavlyukovskiy.boot.jdbc.decorator.p6spy;

import com.p6spy.engine.event.JdbcEventListener;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class P6SpyProperties {

    /**
     * Enables runtime listeners, uses all {@link JdbcEventListener} beans.
     *
     * @see RuntimeListenerSupportFactory
     */
    private boolean enableRuntimeListeners = true;
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
