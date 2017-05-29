Spring Boot autoconfiguration for integration with 
* https://github.com/p6spy/p6spy
* https://github.com/ttddyy/datasource-proxy
* https://github.com/vladmihalcea/flexy-pool

All user defined or autoconfigured data sources will be automatically wrapped with a one of datasource proxy providers above.

As well it supports custom data source decorators through declaring beans of type `DataSourceDecorator`
```
@Bean
public DataSourceDecorator customDecorator() {
  return (beanName, dataSource) -> {
    return new DataSourceWrapper(dataSource);
  }
}
```

If you want to disable decorating set `spring.datasource.decorator.excludeBeans` with bean names you want to exclude or set `spring.datasource.decorator.enabled` to `false` if you want to disable all decorators for all datasources.
