### Spring Boot DataSource Decorator

![Build status](https://github.com/gavlyukovskiy/spring-boot-data-source-decorator/workflows/Build/badge.svg)
[![Latest release](https://img.shields.io/badge/dynamic/xml.svg?label=Maven%20Central&color=green&query=%2F%2Fmetadata%2Fversioning%2Flatest&url=https%3A%2F%2Frepo1.maven.org%2Fmaven2%2Fcom%2Fgithub%2Fgavlyukovskiy%2Fdatasource-decorator-spring-boot-autoconfigure%2Fmaven-metadata.xml)](https://mvnrepository.com/artifact/com.github.gavlyukovskiy/datasource-decorator-spring-boot-autoconfigure/)

[Spring Boot](https://github.com/spring-projects/spring-boot) auto-configuration for integration with
* [P6Spy](https://github.com/p6spy/p6spy) - adds ability to intercept and log sql queries, including interception of a most `Connection`, `Statement` and `ResultSet` methods invocations
* [Datasource Proxy](https://github.com/ttddyy/datasource-proxy) - adds ability to intercept all queries and `Connection`, `Statement` and `ResultSet` method calls
* [FlexyPool](https://github.com/vladmihalcea/flexy-pool) - adds connection pool metrics (jmx, codahale, dropwizard) and flexible strategies for adjusting pool size on demand

#### Why not wrap DataSource in a configuration?

Instead of using the library you can manually wrap your `DataSource`, but this library also provides
* ability to use `@ConfigurationProperties` provided by Spring Boot (`spring.datasource.hikari.*`, `spring.datasource.dbcp2.*`)
* disabling decorating by deployment property `decorator.datasource.enabled=true/false`
* configure proxies through spring properties `application.properties/yml` and customize proxies by defining beans in the spring context

#### Quick Start

Add one of the starters to the classpath of a Spring Boot application and your datasources (auto-configured or custom) will be wrapped into one of a datasource proxy providers below.

The latest release version is [![Latest release](https://img.shields.io/badge/dynamic/xml.svg?label=&color=green&query=%2F%2Fmetadata%2Fversioning%2Flatest&url=https%3A%2F%2Frepo1.maven.org%2Fmaven2%2Fcom%2Fgithub%2Fgavlyukovskiy%2Fdatasource-decorator-spring-boot-autoconfigure%2Fmaven-metadata.xml)](https://mvnrepository.com/artifact/com.github.gavlyukovskiy/datasource-decorator-spring-boot-autoconfigure/)

If you want to use [P6Spy](https://github.com/p6spy/p6spy)
```groovy
implementation("com.github.gavlyukovskiy:p6spy-spring-boot-starter:${version}")
```
```xml
<dependency>
    <groupId>com.github.gavlyukovskiy</groupId>
    <artifactId>p6spy-spring-boot-starter</artifactId>
    <version>${version}</version>
</dependency>
```

or [Datasource Proxy](https://github.com/ttddyy/datasource-proxy):
```groovy
implementation("com.github.gavlyukovskiy:datasource-proxy-spring-boot-starter:${version}")
```
```xml
<dependency>
    <groupId>com.github.gavlyukovskiy</groupId>
    <artifactId>datasource-proxy-spring-boot-starter</artifactId>
    <version>${version}</version>
</dependency>
```

or [FlexyPool](https://github.com/vladmihalcea/flexy-pool)
> To use FlexyPool with connection pool other than HikariCP you must add `PoolAdapter` for your [particular connection pool](https://github.com/vladmihalcea/flexy-pool/wiki/Installation-Guide#connection-pool-settings).
```groovy
implementation("com.github.gavlyukovskiy:flexy-pool-spring-boot-starter:${version}")
```
```xml
<dependency>
    <groupId>com.github.gavlyukovskiy</groupId>
    <artifactId>flexy-pool-spring-boot-starter</artifactId>
    <version>${version}</version>
</dependency>
```

##### What if I add multiple decorators?

You can use all decorators at the same time if you need, if so decorating order will be:

```P6DataSource -> ProxyDataSource -> FlexyPoolDataSource -> DataSource```

#### P6Spy

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

All beans of type `JdbcEventListener` are registered in P6Spy:
```java
@Bean
public JdbcEventListener myListener() {
    return new JdbcEventListener() {
        @Override
        public void onAfterGetConnection(ConnectionInformation connectionInformation, SQLException e) {
            System.out.println("got connection");
        }

        @Override
        public void onAfterConnectionClose(ConnectionInformation connectionInformation, SQLException e) {
            System.out.println("connection closed");
        }
    };
}
```

This done by adding `RuntimeListenerSupportFactory` into P6Spy `modulelist`, overriding this property will cause to not registering factory thus listeners will not be applied

You can configure small set of parameters in your `application.properties`:
```properties
# Register P6LogFactory to log JDBC events
decorator.datasource.p6spy.enable-logging=true
# Use com.p6spy.engine.spy.appender.MultiLineFormat instead of com.p6spy.engine.spy.appender.SingleLineFormat
decorator.datasource.p6spy.multiline=true
# Use logging for default listeners [slf4j, sysout, file, custom]
decorator.datasource.p6spy.logging=slf4j
# Log file to use (only with logging=file)
decorator.datasource.p6spy.log-file=spy.log
# Class file to use (only with logging=custom). The class must implement com.p6spy.engine.spy.appender.FormattedLogger
decorator.datasource.p6spy.custom-appender-class=my.custom.LoggerClass
# Custom log format, if specified com.p6spy.engine.spy.appender.CustomLineFormat will be used with this log format
decorator.datasource.p6spy.log-format=
# Use regex pattern to filter log messages. If specified only matched messages will be logged.
decorator.datasource.p6spy.log-filter.pattern=
# Report the effective sql string (with '?' replaced with real values) to tracing systems.
# NOTE this setting does not affect the logging message.
decorator.datasource.p6spy.tracing.include-parameter-values=true
```

Also you can configure P6Spy manually using one of available configuration methods. For more information please refer to the [P6Spy Configuration Guide](http://p6spy.readthedocs.io/en/latest/configandusage.html)

#### Datasource Proxy

After adding datasource-proxy starter you'll start getting all sql queries in the logs with level `DEBUG` and slow sql queries with level `WARN`:
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
2017-06-07 22:10:50.478  WARN 8492 --- [pool-1-thread-1] n.t.d.l.logging.SLF4JSlowQueryListener   :
Name:, Time:0, Success:False
Type:Statement, Batch:False, QuerySize:1, BatchSize:0
Query:["SELECT SLEEP(301000)"]
Params:[]
```

You can add custom `QueryExecutionListener` by registering them in the context, as well you can override `ParameterTransformer`, `QueryTransformer` and `ConnectionIdManager`:
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

@Bean
public ConnectionIdManagerProvider connectionIdManagerProvider() {
    return MyConnectionIdManager::new;
}
```
You can configure logging, query/slow query listeners and more using your `application.properties`:
```properties
# One of logging libraries (slf4j, jul, common, sysout)
decorator.datasource.datasource-proxy.logging=slf4j

decorator.datasource.datasource-proxy.query.enable-logging=true
decorator.datasource.datasource-proxy.query.log-level=debug
# Logger name to log all queries, default depends on chosen logging, e.g. net.ttddyy.dsproxy.listener.logging.SLF4JQueryLoggingListener
decorator.datasource.datasource-proxy.query.logger-name=

decorator.datasource.datasource-proxy.slow-query.enable-logging=true
decorator.datasource.datasource-proxy.slow-query.log-level=warn
decorator.datasource.datasource-proxy.slow-query.logger-name=
# Number of seconds to consider query as slow and log it
decorator.datasource.datasource-proxy.slow-query.threshold=300

decorator.datasource.datasource-proxy.multiline=true

# Formats the SQL for better readability. Uses Hibernate's formatter if present on the class path. If you opted in for a different JPA provider you need to add https://github.com/vertical-blank/sql-formatter as a runtime dependency to your app  to enable this. 
# Mutually exclusive with json-format=true
decorator.datasource.datasource-proxy.format-sql=true
decorator.datasource.datasource-proxy.json-format=false

# Enable Query Metrics
decorator.datasource.datasource-proxy.count-query=false
```

#### Flexy Pool

If the `flexy-pool-spring-boot-starter` is added to the classpath your datasource will be wrapped to the `FlexyPoolDataSource`.
With default setting you will start getting messages about acquiring and leasing connections:
```text
2017-07-13 01:31:02.575  INFO 5432 --- [ool-1-worker-50] c.v.flexypool.FlexyPoolDataSource        : Connection leased for 1500 millis, while threshold is set to 1000 in dataSource FlexyPoolDataSource
2017-07-13 01:31:03.143  WARN 5432 --- [ool-1-worker-51] PoolOnTimeoutConnectionAcquiringStrategy : Connection was acquired in 1502 millis, timeoutMillis is set to 500
2017-07-13 01:31:03.143  INFO 5432 --- [ool-1-worker-51] PoolOnTimeoutConnectionAcquiringStrategy : Pool size changed from previous value 10 to 11
```
You can declare bean `MetricsFactory` and besides of JMX metrics will be exported to the metrics provider and to the logs:
```text
2017-07-13 02:07:04.265  INFO 5432 --- [rter-1-thread-1] c.v.f.metric.codahale.CodahaleMetrics    : type=HISTOGRAM, name=concurrentConnectionRequestsHistogram, count=4, min=0, max=1, mean=0.5, stddev=0.5, median=1.0, p75=1.0, p95=1.0, p98=1.0, p99=1.0, p999=1.0
2017-07-13 02:07:04.265  INFO 5432 --- [rter-1-thread-1] c.v.f.metric.codahale.CodahaleMetrics    : type=HISTOGRAM, name=concurrentConnectionsHistogram, count=4, min=0, max=1, mean=0.5, stddev=0.5, median=1.0, p75=1.0, p95=1.0, p98=1.0, p99=1.0, p999=1.0
2017-07-13 02:07:04.265  INFO 5432 --- [rter-1-thread-1] c.v.f.metric.codahale.CodahaleMetrics    : type=HISTOGRAM, name=maxPoolSizeHistogram, count=1, min=10, max=10, mean=10.0, stddev=0.0, median=10.0, p75=10.0, p95=10.0, p98=10.0, p99=10.0, p999=10.0
2017-07-13 02:07:04.265  INFO 5432 --- [rter-1-thread-1] c.v.f.metric.codahale.CodahaleMetrics    : type=HISTOGRAM, name=overflowPoolSizeHistogram, count=0, min=0, max=0, mean=0.0, stddev=0.0, median=0.0, p75=0.0, p95=0.0, p98=0.0, p99=0.0, p999=0.0
2017-07-13 02:07:04.265  INFO 5432 --- [rter-1-thread-1] c.v.f.metric.codahale.CodahaleMetrics    : type=HISTOGRAM, name=retryAttemptsHistogram, count=0, min=0, max=0, mean=0.0, stddev=0.0, median=0.0, p75=0.0, p95=0.0, p98=0.0, p99=0.0, p999=0.0
2017-07-13 02:07:04.265  INFO 5432 --- [rter-1-thread-1] c.v.f.metric.codahale.CodahaleMetrics    : type=TIMER, name=connectionAcquireMillis, count=2, min=0.0, max=39.0, mean=19.5, stddev=19.5, median=39.0, p75=39.0, p95=39.0, p98=39.0, p99=39.0, p999=39.0, mean_rate=0.07135042014375073, m1=0.02490778899904623, m5=0.006288975787638508, m15=0.002179432534806779, rate_unit=events/second, duration_unit=milliseconds
2017-07-13 02:07:04.265  INFO 5432 --- [rter-1-thread-1] c.v.f.metric.codahale.CodahaleMetrics    : type=TIMER, name=connectionLeaseMillis, count=2, min=3.0, max=7.0, mean=5.0, stddev=2.0, median=7.0, p75=7.0, p95=7.0, p98=7.0, p99=7.0, p999=7.0, mean_rate=0.07135743555785098, m1=0.02490778899904623, m5=0.006288975787638508, m15=0.002179432534806779, rate_unit=events/second, duration_unit=milliseconds
2017-07-13 02:07:04.265  INFO 5432 --- [rter-1-thread-1] c.v.f.metric.codahale.CodahaleMetrics    : type=TIMER, name=overallConnectionAcquireMillis, count=2, min=0.0, max=39.0, mean=19.5, stddev=19.5, median=39.0, p75=39.0, p95=39.0, p98=39.0, p99=39.0, p999=39.0, mean_rate=0.07135462550886962, m1=0.02490778899904623, m5=0.006288975787638508, m15=0.002179432534806779, rate_unit=events/second, duration_unit=milliseconds
```

All beans of type `ConnectionAcquiringStrategyFactory` are used to provide `ConnectionAcquiringStrategy` for the pool.

`MetricsFactory` and `ConnectionProxyFactory` beans can be used to customize metrics and connection decorators.

`EventListener<? extends Event>` beans can be registered to subscribe on events of flexy-pool (e.g. `ConnectionAcquireTimeThresholdExceededEvent`, `ConnectionLeaseTimeThresholdExceededEvent`).

You can configure your `FlexyPoolDataSource` by using bean `FlexyPoolConfigurationBuilderCustomizer` or properties:
```properties
# Increments pool size if connection acquire request has timed out
decorator.datasource.flexy-pool.acquiring-strategy.increment-pool.max-overflow-pool-size=15
decorator.datasource.flexy-pool.acquiring-strategy.increment-pool.timeout-millis=500

# Retries on getting connection
decorator.datasource.flexy-pool.acquiring-strategy.retry.attempts=2

# Enable metrics exporting to the JMX
decorator.datasource.flexy-pool.metrics.reporter.jmx.enabled=true
decorator.datasource.flexy-pool.metrics.reporter.jmx.auto-start=false

# Millis between two consecutive log reports
decorator.datasource.flexy-pool.metrics.reporter.log.millis=300000

# Enable logging and publishing ConnectionAcquireTimeThresholdExceededEvent when a connection acquire request has timed out
decorator.datasource.flexy-pool.threshold.connection.acquire=50
# Enable logging and publishing ConnectionLeaseTimeThresholdExceededEvent when a connection lease has exceeded the given time threshold
decorator.datasource.flexy-pool.threshold.connection.lease=1000
```

#### Spring Cloud Sleuth (deprecated)

##### For Spring Boot users, that DO NOT use Spring Cloud Sleuth
Nothing has changed, this project is continued to be supported and maintained, and can be used to enable JDBC logging
and provide auto-configuration of P6Spy, Datasource-Proxy and FlexyPool.


##### For Spring Cloud Sleuth users
As of release 1.8.0 Spring Cloud Sleuth integration was deprecated in favor of [Spring Cloud Sleuth: Spring JDBC](https://docs.spring.io/spring-cloud-sleuth/docs/3.1.0/reference/html/integrations.html#sleuth-jdbc-integration)
which provides JDBC instrumentation out of the box.

As of release 1.9.0 Spring Cloud Sleuth integration was removed.

Spring Cloud Sleuth JDBC was based on this project and keeps all functionality including logging, tracing, configuration and customizations.

##### Migration process:
1. If you are using Spring Cloud Sleuth you can migrate all properties from `decorator.datasource.*` to `spring.sleuth.jdbc.*` with minimal changes.
2. If you have query logging enabled (default state) then you need to explicitly enable logging using:
   - P6Spy: `spring.sleuth.jdbc.p6spy.enable-logging=true`
   - Datasource-Proxy: `spring.sleuth.jdbc.datasource-proxy.query.enable-logging=true`
3. If you were using decoration customizers please consult with Spring Cloud Sleuth documentation and migrate usage of those to appropriate alternatives in Spring Cloud Sleuth
4. _(Optional)_ Replace dependency on this starter with the particular library
   - P6Spy: replace `com.github.gavlyukovskiy:p6spy-spring-boot-starter` with `p6spy:p6spy`
   - Datasource-Proxy: replace `com.github.gavlyukovskiy:datasource-proxy-spring-boot-starter` with `net.ttddyy:datasource-proxy`
5. Any issues can be raised in Spring Cloud Sleuth  project on GitHub, you may tag me (@gavlyukovskiy) and I'll try to help.
6. Enjoy using JDBC instrumentation, and thank you for using this library :)
 
Due to similarities in implementation, using starters from this library together with Spring Cloud Sleuth 3.1.0 is possible, although decoration will be automatically disabled in favor of Spring Cloud Sleuth to avoid duplicated logging, tracing or any other potential issues.

#### Custom Decorators

Custom data source decorators are supported through declaring beans of type `DataSourceDecorator`
```java
@Bean
public DataSourceDecorator customDecorator() {
  return (beanName, dataSource) -> new DataSourceWrapper(dataSource);
}
```

#### Disable Decorating

If you want to disable decorating set `decorator.datasource.exclude-beans` with bean names you want to exclude.
Also, you can disable decorating for `AbstractRoutingDataSource` setting property `decorator.datasource.ignore-routing-data-sources` to `true`
Set `decorator.datasource.enabled` to `false` if you want to disable all decorators for all datasources. 