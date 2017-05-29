**Spring Boot DataSource Decorator**

Spring Boot autoconfiguration for integration with 
* [P6Spy](https://github.com/p6spy/p6spy)
* [Datasource Proxy](https://github.com/ttddyy/datasource-proxy)
* [FlexyPool](https://github.com/vladmihalcea/flexy-pool)

**Quick Start**

Add one of the starters to the classpath of a Spring Boot application and your datasources (autoconfigured or custom) will be wrapped into one of a datasource proxy providers above.

Gradle:
```groovy
compile('com.github.gavlyukovskiy:p6spy-spring-boot-starter:1.0.RELEASE')
compile('com.github.gavlyukovskiy:datasource-proxy-spring-boot-starter:1.0.RELEASE')
compile('com.github.gavlyukovskiy:flexy-pool-spring-boot-starter:1.0.RELEASE')
```

Maven:
```xml
<dependency>
    <groupId>com.github.gavlyukovskiy</groupId>
    <artifactId>p6spy-spring-boot-starter</artifactId>
    <version>1.0.RELEASE</version>
</dependency>
<dependency>
    <groupId>com.github.gavlyukovskiy</groupId>
    <artifactId>datasource-proxy-spring-boot-starter</artifactId>
    <version>1.0.RELEASE</version>
</dependency>
<dependency>
    <groupId>com.github.gavlyukovskiy</groupId>
    <artifactId>flexy-pool-spring-boot-starter</artifactId>
    <version>1.0.RELEASE</version>
</dependency>
```

**P6Spy**

After adding p6spy starter you'll start getting all sql queries to the file `spy.log` in the working directory.
You can add modules or change output format by creating file `spy.properties` in the working directory/classpath. Example:
```properties
modulelist=com.mycompany.MyP6SpyModule
logMessageFormat=com.p6spy.engine.spy.appender.MultiLineFormat
logfile=sql.log
```
For more information please refer to the [P6Spy Configuration Guide](http://p6spy.readthedocs.io/en/latest/configandusage.html)   

**Custom Decorators**

Custom data source decorators are supported through declaring beans of type `DataSourceDecorator`
```
@Bean
public DataSourceDecorator customDecorator() {
  return (beanName, dataSource) -> {
    return new DataSourceWrapper(dataSource);
  }
}
```

**Disable Decorating**

If you want to disable decorating set `spring.datasource.decorator.excludeBeans` with bean names you want to exclude or set `spring.datasource.decorator.enabled` to `false` if you want to disable all decorators for all datasources.
