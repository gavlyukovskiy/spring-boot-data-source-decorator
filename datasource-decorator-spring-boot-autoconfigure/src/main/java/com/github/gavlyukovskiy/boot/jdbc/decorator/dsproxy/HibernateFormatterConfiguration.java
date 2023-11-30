package com.github.gavlyukovskiy.boot.jdbc.decorator.dsproxy;

import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.hibernate.engine.jdbc.internal.BasicFormatterImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(BasicFormatterImpl.class)
@ConditionalOnProperty(name = "decorator.datasource.datasource-proxy.format-sql", havingValue = "true")
class HibernateFormatterConfiguration {

    private static final Logger log = LoggerFactory.getLogger(HibernateFormatterConfiguration.class);

    @Bean
    @ConditionalOnMissingBean // let users define their own
    public ProxyDataSourceBuilder.FormatQueryCallback hibernateFormatQueryCallback() {
        log.debug("{} will be used as formatter", BasicFormatterImpl.class.getName());
        BasicFormatterImpl hibernateFormatter = new BasicFormatterImpl();
        return hibernateFormatter::format;
    }
}