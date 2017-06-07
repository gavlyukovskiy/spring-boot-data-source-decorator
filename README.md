**Spring Boot DataSource Decorator**

Spring Boot autoconfiguration for integration with 
* [P6Spy](https://github.com/p6spy/p6spy)
* [Datasource Proxy](https://github.com/ttddyy/datasource-proxy)
* [FlexyPool](https://github.com/vladmihalcea/flexy-pool)

**Quick Start**

Add one of the starters to the classpath of a Spring Boot application and your datasources (autoconfigured or custom) will be wrapped into one of a datasource proxy providers above.

Gradle:
```groovy
compile('com.github.gavlyukovskiy:p6spy-spring-boot-starter:1.1.0.RELEASE')
compile('com.github.gavlyukovskiy:datasource-proxy-spring-boot-starter:1.1.0.RELEASE')
compile('com.github.gavlyukovskiy:flexy-pool-spring-boot-starter:1.1.0.RELEASE')
```

Maven:
```xml
<dependency>
    <groupId>com.github.gavlyukovskiy</groupId>
    <artifactId>p6spy-spring-boot-starter</artifactId>
    <version>1.1.0.RELEASE</version>
</dependency>
<dependency>
    <groupId>com.github.gavlyukovskiy</groupId>
    <artifactId>datasource-proxy-spring-boot-starter</artifactId>
    <version>1.1.0.RELEASE</version>
</dependency>
<dependency>
    <groupId>com.github.gavlyukovskiy</groupId>
    <artifactId>flexy-pool-spring-boot-starter</artifactId>
    <version>1.1.0.RELEASE</version>
</dependency>
```

**P6Spy**

After adding p6spy starter you'll start getting all sql queries in the logs.

By default all `JdbcEventListener` beans are registered in P6Spy:
```java
@Bean
public JdbcEventListener myListener() {
    return new JdbcEventListener() {
        @Override
        public void onConnectionWrapped(ConnectionInformation connectionInformation) {
            System.out.println("connection wrapped");
        }

        @Override
        public void onAfterConnectionClose(ConnectionInformation connectionInformation, SQLException e) {
            System.out.println("connection closed");
        }
    };
}
```

This done by adding `RuntimeListenerSupportFactory` into P6Spy `modulelist`, overriding this property will cause to not registering factory thus listeners will not be applied  

You can configure small set of parameters in your application properties:
```properties
# Register RuntimeListenerSupportFactory if JdbcEventListener beans were found
spring.datasource.decorator.p6spy.enable-runtime-listeners=true
# Use com.p6spy.engine.spy.appender.MultiLineFormat instead of com.p6spy.engine.spy.appender.SingleLineFormat
spring.datasource.decorator.p6spy.multiline=true
# Use logging for default listeners [slf4j, sysout, file]
spring.datasource.decorator.p6spy.logging=slf4j
# Log file to use (only with logging=file)
spring.datasource.decorator.p6spy.log-file=spy.log
```

Also you can configure P6Spy manually using one of available configuration methods. For more information please refer to the [P6Spy Configuration Guide](http://p6spy.readthedocs.io/en/latest/configandusage.html)   

**Datasource Proxy**

After adding datasource-proxy starter you'll start getting all sql queries in the logs.

By default all `QueryExecutionListener` beans are registered in `ProxyDataSourceBuilder`:
```java
@Bean
public QueryExecutionListener queryExecutionListener() {
    return new QueryExecutionListener() {
        @Override
        public void beforeQuery(ExecutionInfo execInfo, List<QueryInfo> queryInfoList) {
            System.out.println("beforeQuery");
        }

        @Override
        public void afterQuery(ExecutionInfo execInfo, List<QueryInfo> queryInfoList) {
            System.out.println("afterQuery");
        }
    };
}
```

As well you can use Spring Context for `ParameterTransformer` and `QueryTransformer`.

You can use properties with prefix `spring.datasource.decorator.datasource-proxy` to configure query/slow query listeners, logging levels, thresholds and more.


**Custom Decorators**

Custom data source decorators are supported through declaring beans of type `DataSourceDecorator`
```
@Bean
public DataSourceDecorator customDecorator() {
  return (beanName, dataSource) -> new DataSourceWrapper(dataSource);
}
```

**Disable Decorating**

If you want to disable decorating set `spring.datasource.decorator.excludeBeans` with bean names you want to exclude or set `spring.datasource.decorator.enabled` to `false` if you want to disable all decorators for all datasources.
