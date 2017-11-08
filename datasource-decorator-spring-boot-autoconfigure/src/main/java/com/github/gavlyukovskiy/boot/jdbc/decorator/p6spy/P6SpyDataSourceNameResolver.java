package com.github.gavlyukovskiy.boot.jdbc.decorator.p6spy;

import com.github.gavlyukovskiy.boot.jdbc.decorator.DataSourceDecorationStage;
import com.github.gavlyukovskiy.boot.jdbc.decorator.DecoratedDataSource;
import org.springframework.context.ApplicationContext;

import javax.sql.CommonDataSource;
import javax.sql.DataSource;

import java.util.Map;
import java.util.Map.Entry;

public class P6SpyDataSourceNameResolver {

    private final ApplicationContext applicationContext;
    private Map<String, DataSource> dataSources;

    public P6SpyDataSourceNameResolver(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public String resolveDataSourceName(CommonDataSource dataSource) {
        if (dataSources == null) {
            this.dataSources = applicationContext.getBeansOfType(DataSource.class);
        }
        return dataSources.entrySet()
                .stream()
                .filter(entry -> {
                    DataSource candidate = entry.getValue();
                    if (candidate instanceof DecoratedDataSource) {
                        return matchesDataSource((DecoratedDataSource) candidate, dataSource);
                    }
                    return candidate == dataSource;
                })
                .findFirst()
                .map(Entry::getKey)
                .orElse("dataSource");
    }

    private boolean matchesDataSource(DecoratedDataSource decoratedCandidate, CommonDataSource dataSource) {
        return decoratedCandidate.getDecoratingChain().stream()
                .map(DataSourceDecorationStage::getDataSource)
                .anyMatch(candidate -> candidate == dataSource);
    }
}
