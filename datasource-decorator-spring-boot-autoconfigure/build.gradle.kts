dependencies {
    compile("org.springframework:spring-jdbc:5.1.8.RELEASE")
    compile("org.springframework.boot:spring-boot:${project.extra["springBootVersion"]}")
    compile("org.springframework.boot:spring-boot-autoconfigure:${project.extra["springBootVersion"]}")

    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor:${project.extra["springBootVersion"]}")
    annotationProcessor("org.projectlombok:lombok:1.18.8")
    compileOnly("org.springframework.boot:spring-boot-configuration-processor:${project.extra["springBootVersion"]}")
    compileOnly("org.projectlombok:lombok:1.18.8")

    compileOnly("org.springframework.boot:spring-boot-actuator:${project.extra["springBootVersion"]}")

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
    compileOnly("org.springframework.cloud:spring-cloud-sleuth-core:${project.extra["sleuthVersion"]}")

    testCompile("org.junit.jupiter:junit-jupiter-api:5.5.0")
    testCompile("org.junit.jupiter:junit-jupiter-params:5.5.0")
    testCompile("org.junit.jupiter:junit-jupiter-engine:5.5.0")
    testCompile("com.h2database:h2:1.4.199")
    testCompile("org.assertj:assertj-core:3.12.2")
    testCompile("org.springframework.boot:spring-boot-starter-actuator:${project.extra["springBootVersion"]}")
    testCompile("org.springframework.boot:spring-boot-starter-test:${project.extra["springBootVersion"]}")

    testCompile("p6spy:p6spy:${project.extra["p6SpyVersion"]}")
    testCompile("net.ttddyy:datasource-proxy:${project.extra["datasourceProxyVersion"]}")
    testCompile("com.vladmihalcea.flexy-pool:flexy-pool-core:${project.extra["flexyPoolVersion"]}")
    testCompile("com.vladmihalcea.flexy-pool:flexy-dbcp2:${project.extra["flexyPoolVersion"]}")
    testCompile("com.vladmihalcea.flexy-pool:flexy-hikaricp:${project.extra["flexyPoolVersion"]}")
    testCompile("com.vladmihalcea.flexy-pool:flexy-tomcatcp:${project.extra["flexyPoolVersion"]}")
    testCompile("com.vladmihalcea.flexy-pool:flexy-micrometer-metrics:${project.extra["flexyPoolVersion"]}")

    testCompile("org.springframework.cloud:spring-cloud-sleuth-core:${project.extra["sleuthVersion"]}")

    testCompile("commons-dbcp:commons-dbcp:1.4")
    testCompile("org.apache.commons:commons-dbcp2:2.6.0")
    testCompile("org.apache.tomcat:tomcat-jdbc:9.0.22")
    testCompile("com.zaxxer:HikariCP:3.3.1")

    testAnnotationProcessor("org.projectlombok:lombok:1.18.8")
    testCompileOnly("org.projectlombok:lombok:1.18.8")
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
    }
}
