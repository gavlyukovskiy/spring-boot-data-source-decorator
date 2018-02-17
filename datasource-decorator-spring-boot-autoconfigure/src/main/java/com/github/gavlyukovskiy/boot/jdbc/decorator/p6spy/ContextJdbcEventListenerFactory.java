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
import com.p6spy.engine.spy.JdbcEventListenerFactory;
import org.springframework.util.Assert;

import java.util.List;

/**
 * Factory to support all defined {@link JdbcEventListener} in the context.
 *
 * @author Arthur Gavlyukovskiy
 * @since 1.3.0
 */
public class ContextJdbcEventListenerFactory implements JdbcEventListenerFactory {

    private final CompoundJdbcEventListener compoundJdbcEventListener;

    ContextJdbcEventListenerFactory(JdbcEventListenerFactory delegate, List<JdbcEventListener> listeners) {
        Assert.notNull(delegate, "JdbcEventListenerFactory should not be null");
        Assert.notEmpty(listeners, "Listeners should not be empty");

        JdbcEventListener jdbcEventListener = delegate.createJdbcEventListener();
        if (jdbcEventListener instanceof CompoundJdbcEventListener) {
            compoundJdbcEventListener = (CompoundJdbcEventListener) jdbcEventListener;
        }
        else {
            compoundJdbcEventListener = new CompoundJdbcEventListener();
            compoundJdbcEventListener.addListender(jdbcEventListener);
        }
        listeners.forEach(compoundJdbcEventListener::addListender);
    }

    @Override
    public JdbcEventListener createJdbcEventListener() {
        return compoundJdbcEventListener;
    }
}
