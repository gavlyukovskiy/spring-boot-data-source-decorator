package com.github.gavlyukovskiy.boot.jdbc.decorator.metadata;

import javax.sql.DataSource;

import java.util.Map;

public interface DecoratedDataSourceMetadataProvider {

    Map<String, Number> getMetrics(DataSource dataSource);
}
