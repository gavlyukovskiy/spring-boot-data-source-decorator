package com.github.gavlyukovskiy.boot.jdbc.decorator.p6spy;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class P6SpyProperties {

    private boolean enableDynamicListeners = true;
    private boolean multiline = true;
}
