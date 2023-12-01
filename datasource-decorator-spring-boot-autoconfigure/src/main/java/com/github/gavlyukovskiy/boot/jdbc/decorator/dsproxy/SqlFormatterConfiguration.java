package com.github.gavlyukovskiy.boot.jdbc.decorator.dsproxy;

import com.github.vertical_blank.sqlformatter.SqlFormatter;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(SqlFormatter.class)
@ConditionalOnProperty(name = "decorator.datasource.datasource-proxy.format-sql", havingValue = "true")
class SqlFormatterConfiguration {

    private static final Logger log = LoggerFactory.getLogger(SqlFormatterConfiguration.class);

    @Bean
    @ConditionalOnMissingBean // let users define their own
    public ProxyDataSourceBuilder.FormatQueryCallback sqlFormatterFormatQueryCallback() {
        log.debug("{} will be used as formatter", SqlFormatter.class.getName());
        return SqlFormatter::format;
    }
}