package com.github.gavlyukovskiy.boot.jdbc.decorator.dsproxy;

import net.ttddyy.dsproxy.ConnectionIdManager;

import java.util.function.Supplier;

public interface ConnectionIdManagerProvider extends Supplier<ConnectionIdManager> {
}
