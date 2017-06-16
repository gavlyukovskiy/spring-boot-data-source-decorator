package com.github.gavlyukovskiy.boot.jdbc.decorator.dsproxy;

import com.github.gavlyukovskiy.boot.jdbc.decorator.DataSourceDecorator;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.springframework.core.Ordered;

import javax.sql.DataSource;

class ProxyDataSourceDecorator implements DataSourceDecorator, Ordered {
    private final ProxyDataSourceBuilder proxyDataSourceBuilder;

    ProxyDataSourceDecorator(ProxyDataSourceBuilder proxyDataSourceBuilder) {
        this.proxyDataSourceBuilder = proxyDataSourceBuilder;
    }

    @Override
    public DataSource decorate(String beanName, DataSource dataSource) {
        return proxyDataSourceBuilder.dataSource(dataSource).name(beanName).build();
    }

    @Override
    public int getOrder() {
        return 20;
    }
}
