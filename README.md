**Spring Boot DataSource Decorator**

Spring Boot autoconfiguration for integration with 
* [P6Spy](https://github.com/p6spy/p6spy)
* [Datasource Proxy](https://github.com/ttddyy/datasource-proxy)
* [FlexyPool](https://github.com/vladmihalcea/flexy-pool)

**Why Should I Care**

Of course you can just create `DataSource` bean wrapped in any proxy you want, but what will you get using this library:
* ability to configure your datasource using `spring.datasource.hikari.*`, `spring.datasource.dbcp2.*`, `spring.datasource.tomcat.*`
* `/metrics` - will display your actual datasource stats (active, usage)
* ability to disable proxying quick on appropriate environment
* configure each library using only Spring Context without pain

**Quick Start**

Add one of the starters to the classpath of a Spring Boot application and your datasources (autoconfigured or custom) will be wrapped into one of a datasource proxy providers above.

Gradle:
```groovy
compile('com.github.gavlyukovskiy:p6spy-spring-boot-starter')
compile('com.github.gavlyukovskiy:datasource-proxy-spring-boot-starter')
compile('com.github.gavlyukovskiy:flexy-pool-spring-boot-starter')
```

Maven:
```xml
<dependency>
    <groupId>com.github.gavlyukovskiy</groupId>
    <artifactId>p6spy-spring-boot-starter</artifactId>
</dependency>
<dependency>
    <groupId>com.github.gavlyukovskiy</groupId>
    <artifactId>datasource-proxy-spring-boot-starter</artifactId>
</dependency>
<dependency>
    <groupId>com.github.gavlyukovskiy</groupId>
    <artifactId>flexy-pool-spring-boot-starter</artifactId>
</dependency>
```

**P6Spy**

After adding p6spy starter you'll start getting all sql queries in the logs:
```text
2017-06-07 21:42:08.120  INFO 5456 --- [ool-1-worker-57] p6spy                                    : #1496860928120 | took 0ms | statement | connection 0|SELECT NOW()
;
2017-06-07 21:51:07.802  INFO 5456 --- [ool-1-worker-50] p6spy                                    : #1496861467802 | took 0ms | statement | connection 1|SELECT NOW()
;
2017-06-07 21:51:07.803  INFO 5456 --- [ool-1-worker-43] p6spy                                    : #1496861467803 | took 0ms | statement | connection 2|SELECT NOW()
;
2017-06-07 21:51:08.806  INFO 5456 --- [ool-1-worker-36] p6spy                                    : #1496861468806 | took 0ms | statement | connection 3|SELECT NOW()
;
```

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

After adding datasource-proxy starter you'll start getting all sql queries in the logs with level `DEBUG`:
```text
2017-06-07 21:58:06.630  DEBUG 8492 --- [ool-1-worker-57] n.t.d.l.l.SLF4JQueryLoggingListener      : 
Name:, Time:0, Success:True
Type:Statement, Batch:False, QuerySize:1, BatchSize:0
Query:["SELECT NOW()"]
Params:[]
2017-06-07 21:58:06.630  DEBUG 8492 --- [ool-1-worker-43] n.t.d.l.l.SLF4JQueryLoggingListener      : 
Name:, Time:0, Success:True
Type:Statement, Batch:False, QuerySize:1, BatchSize:0
Query:["SELECT NOW()"]
Params:[]
2017-06-07 21:58:06.630  DEBUG 8492 --- [ool-1-worker-50] n.t.d.l.l.SLF4JQueryLoggingListener      : 
Name:, Time:0, Success:True
Type:Statement, Batch:False, QuerySize:1, BatchSize:0
Query:["SELECT NOW()"]
Params:[]
```

By default all `QueryExecutionListener` beans are registered in `ProxyDataSourceBuilder`, as well you can use Spring Context for `ParameterTransformer` and `QueryTransformer`.:
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

@Bean
public ParameterTransformer parameterTransformer() {
    return new MyParameterTransformer();
}

@Bean
public QueryTransformer queryTransformer() {
    return new MyQueryTransformer();
}
```
You can configure logging, query/slow query listeners and more using your `application.properties`:
```text
# One of logging libraries (slf4j, jul, common, sysout)
spring.datasource.decorator.datasource-proxy.logging=slf4j

spring.datasource.decorator.datasource-proxy.query.enable-logging=true
spring.datasource.decorator.datasource-proxy.query.log-level=debug
# Logger name to log all queries, default depends on chosen logging, e.g. net.ttddyy.dsproxy.listener.logging.SLF4JQueryLoggingListener
spring.datasource.decorator.datasource-proxy.query.logger-name=

spring.datasource.decorator.datasource-proxy.slow-query.enable-logging=true
spring.datasource.decorator.datasource-proxy.slow-query.log-level=warn
spring.datasource.decorator.datasource-proxy.slow-query.logger-name=
# Number of seconds to consider query as slow and log it
spring.datasource.decorator.datasource-proxy.slow-query.threshold=300

spring.datasource.decorator.datasource-proxy.multiline=true
spring.datasource.decorator.datasource-proxy.json-format=false
# Enable Query Metrics
spring.datasource.decorator.datasource-proxy.count-query=false
```

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
