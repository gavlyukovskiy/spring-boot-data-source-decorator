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

import com.p6spy.engine.event.CompoundJdbcEventListener;
import com.p6spy.engine.event.JdbcEventListener;
import com.p6spy.engine.spy.P6Factory;
import com.p6spy.engine.spy.P6LoadableOptions;
import com.p6spy.engine.spy.option.P6OptionsRepository;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Objects;

/**
 * Factory to support all defined {@link JdbcEventListener} in the context.
 * If any listener is added into context this factory will be added to the P6Spy 'modulelist'.
 *
 * If P6Spy 'modulelist' is overridden, factory will not be registered thus listeners will not be applied.
 *
 * @author Arthur Gavlyukovskiy
 */
public class RuntimeListenerSupportFactory implements P6Factory {

    private static List<JdbcEventListener> listeners;

    private final CompoundJdbcEventListener listener;

    public RuntimeListenerSupportFactory() {
        Assert.state(listeners != null, "This factory should not been initialized if there are no listeners are set");
        listener = new CompoundJdbcEventListener(listeners);
    }

    static void setListeners(List<JdbcEventListener> listeners) {
        Assert.state(RuntimeListenerSupportFactory.listeners == null, "Listeners are already set");
        Objects.requireNonNull(listeners, "listeners should not be null");
        RuntimeListenerSupportFactory.listeners = listeners;
    }

    static void unsetListeners() {
        RuntimeListenerSupportFactory.listeners = null;
    }

    @Override
    public P6LoadableOptions getOptions(P6OptionsRepository optionsRepository) {
        return new RuntimeListenerSupportLoadableOptions(optionsRepository);
    }

    @Override
    public JdbcEventListener getJdbcEventListener() {
        return listener;
    }
}
