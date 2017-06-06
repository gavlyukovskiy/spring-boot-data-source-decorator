package com.github.gavlyukovskiy.boot.jdbc.decorator.p6spy;

import com.p6spy.engine.spy.P6SpyOptions;
import com.p6spy.engine.spy.option.P6OptionsRepository;

class RuntimeListenerSupportLoadableOptions extends P6SpyOptions {

    RuntimeListenerSupportLoadableOptions(P6OptionsRepository optionsRepository) {
        super(optionsRepository);
    }
}
