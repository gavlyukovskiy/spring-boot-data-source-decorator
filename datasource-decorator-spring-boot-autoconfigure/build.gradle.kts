plugins {
    `java-library`
}

dependencies {
    implementation("org.springframework.boot:spring-boot:${project.extra["springBootVersion"]}")
    implementation("org.springframework.boot:spring-boot-autoconfigure:${project.extra["springBootVersion"]}")
    implementation("org.springframework.boot:spring-boot-starter-jdbc:${project.extra["springBootVersion"]}")

    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor:${project.extra["springBootVersion"]}")

    compileOnly("org.apache.commons:commons-dbcp2:2.6.0")
    compileOnly("org.apache.tomcat:tomcat-jdbc:9.0.22")
    compileOnly("com.zaxxer:HikariCP:3.3.1")

    compileOnly("p6spy:p6spy:${project.extra["p6SpyVersion"]}")
    compileOnly("net.ttddyy:datasource-proxy:${project.extra["datasourceProxyVersion"]}")
    compileOnly("com.vladmihalcea.flexy-pool:flexy-pool-core:${project.extra["flexyPoolVersion"]}")
    compileOnly("com.vladmihalcea.flexy-pool:flexy-dbcp2:${project.extra["flexyPoolVersion"]}")
    compileOnly("com.vladmihalcea.flexy-pool:flexy-hikaricp:${project.extra["flexyPoolVersion"]}")
    compileOnly("com.vladmihalcea.flexy-pool:flexy-tomcatcp:${project.extra["flexyPoolVersion"]}")
    compileOnly("com.vladmihalcea.flexy-pool:flexy-micrometer-metrics:${project.extra["flexyPoolVersion"]}")

    compileOnly("org.springframework.boot:spring-boot-starter-actuator:${project.extra["springBootVersion"]}")
    compileOnly("org.springframework.cloud:spring-cloud-starter-sleuth:${project.extra["sleuthVersion"]}")

    testImplementation("com.h2database:h2:1.4.199")
    testImplementation("org.springframework.boot:spring-boot-starter-test:${project.extra["springBootVersion"]}")

    testImplementation("p6spy:p6spy:${project.extra["p6SpyVersion"]}")
    testImplementation("net.ttddyy:datasource-proxy:${project.extra["datasourceProxyVersion"]}")
    testImplementation("com.vladmihalcea.flexy-pool:flexy-pool-core:${project.extra["flexyPoolVersion"]}")
    testImplementation("com.vladmihalcea.flexy-pool:flexy-dbcp2:${project.extra["flexyPoolVersion"]}")
    testImplementation("com.vladmihalcea.flexy-pool:flexy-hikaricp:${project.extra["flexyPoolVersion"]}")
    testImplementation("com.vladmihalcea.flexy-pool:flexy-tomcatcp:${project.extra["flexyPoolVersion"]}")
    testImplementation("com.vladmihalcea.flexy-pool:flexy-micrometer-metrics:${project.extra["flexyPoolVersion"]}")

    testImplementation("org.springframework.cloud:spring-cloud-starter-sleuth:${project.extra["sleuthVersion"]}")
    testImplementation("io.zipkin.brave:brave-tests:5.13.3")

    testImplementation("commons-dbcp:commons-dbcp:1.4")
    testImplementation("org.apache.commons:commons-dbcp2:2.6.0")
    testImplementation("org.apache.tomcat:tomcat-jdbc:9.0.22")
    testImplementation("com.zaxxer:HikariCP:3.3.1")
}

tasks {
    compileJava {
        dependsOn(processResources)
    }

    withType<JavaCompile> {
        options.compilerArgs.add("-Xlint:unchecked")
        options.compilerArgs.add("-Xlint:deprecation")
    }

    test {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        }
    }
}
