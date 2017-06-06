package com.github.gavlyukovskiy.boot.jdbc.decorator.p6spy;

import com.p6spy.engine.event.CompoundJdbcEventListener;
import com.p6spy.engine.event.JdbcEventListener;
import com.p6spy.engine.spy.P6Factory;
import com.p6spy.engine.spy.P6LoadableOptions;
import com.p6spy.engine.spy.option.P6OptionsRepository;
import org.springframework.util.Assert;

import java.util.List;

/**
 * Factory to support all defined {@link JdbcEventListener} in the context.
 * If any listener is added into context this factory will be added to the P6Spy 'modulelist'.
 *
 * If P6Spy 'modulelist' is overridden, factory will not be registered thus listeners will not be applied.
 */
public class RuntimeListenerSupportFactory implements P6Factory {

    private final CompoundJdbcEventListener listener;

    public RuntimeListenerSupportFactory() {
        List<JdbcEventListener> listeners = RuntimeListenerHolder.getListeners();
        Assert.state(listeners != null, "This factory should not been initialized if there are no listeners available");
        listener = new CompoundJdbcEventListener(listeners);
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
