package com.github.gavlyukovskiy.boot.jdbc.decorator.p6spy;

import com.p6spy.engine.event.CompoundJdbcEventListener;
import com.p6spy.engine.event.JdbcEventListener;
import com.p6spy.engine.spy.P6Factory;
import com.p6spy.engine.spy.P6LoadableOptions;
import com.p6spy.engine.spy.option.P6OptionsRepository;
import org.springframework.util.Assert;

import java.util.List;

public class RuntimeListenerSupportFactory implements P6Factory {
    @Override
    public P6LoadableOptions getOptions(P6OptionsRepository optionsRepository) {
        return new RuntimeListenerSupportLoadableOptions(optionsRepository);
    }

    @Override
    public JdbcEventListener getJdbcEventListener() {
        List<JdbcEventListener> listeners = RuntimeListenerHolder.getListeners();
        Assert.state(listeners != null, "This factory should not been initialized if there are no listeners available");
        return new CompoundJdbcEventListener(listeners);
    }
}
