package com.github.gavlyukovskiy.boot.jdbc.decorator.p6spy;

import com.p6spy.engine.spy.P6SpyOptions;
import com.p6spy.engine.spy.option.P6OptionsRepository;

public class RuntimeListenerSupportLoadableOptions extends P6SpyOptions {

    public RuntimeListenerSupportLoadableOptions(P6OptionsRepository optionsRepository) {
        super(optionsRepository);
    }
}
