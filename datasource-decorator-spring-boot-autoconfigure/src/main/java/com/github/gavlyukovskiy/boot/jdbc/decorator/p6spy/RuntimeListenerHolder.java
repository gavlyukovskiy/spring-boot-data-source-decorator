package com.github.gavlyukovskiy.boot.jdbc.decorator.p6spy;

import com.p6spy.engine.event.JdbcEventListener;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

class RuntimeListenerHolder {

    private static List<JdbcEventListener> listeners = Collections.emptyList();

    private RuntimeListenerHolder() {
    }

    static List<JdbcEventListener> getListeners() {
        return listeners;
    }

    static void setListeners(List<JdbcEventListener> listeners) {
        RuntimeListenerHolder.listeners = Objects.requireNonNull(listeners, "listeners should not be null");
    }
}
