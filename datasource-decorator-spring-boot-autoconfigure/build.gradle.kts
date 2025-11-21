plugins {
    `java-library`
    alias(libs.plugins.test.logger)
}

dependencies {
    implementation(platform(libs.spring.boot.dependencies))
    annotationProcessor(platform(libs.spring.boot.dependencies))
    compileOnly(platform(libs.spring.boot.dependencies))
    testImplementation(platform(libs.junit.bom))
    testImplementation(platform(libs.spring.boot.dependencies))

    implementation(libs.spring.boot)
    implementation(libs.spring.boot.autoconfigure)
    implementation(libs.spring.boot.jdbc)

    annotationProcessor(libs.spring.boot.configuration.processor)

    compileOnly(libs.commons.dbcp2)
    compileOnly(libs.tomcat.jdbc)
    compileOnly(libs.hikari.cp)

    compileOnly(libs.p6spy)
    compileOnly(libs.datasource.proxy)
    compileOnly(libs.flexy.pool.core)
    compileOnly(libs.flexy.pool.dbcp2)
    compileOnly(libs.flexy.pool.hikaricp)
    compileOnly(libs.flexy.pool.tomcatcp)
    compileOnly(libs.flexy.pool.micrometer.metrics)

    compileOnly(libs.spring.boot.actuator)

    // optional (compileOnly) dependencies for SQL formatting
    compileOnly(libs.hibernate.core)
    compileOnly(libs.sql.formatter)

    testImplementation(libs.junit.platform.launcher)
    testImplementation(libs.h2)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.flyway)
    testImplementation(libs.spring.boot.jdbc.test)

    testImplementation(libs.p6spy)
    testImplementation(libs.datasource.proxy)
    testImplementation(libs.flexy.pool.core)
    testImplementation(libs.flexy.pool.dbcp2)
    testImplementation(libs.flexy.pool.hikaricp)
    testImplementation(libs.flexy.pool.tomcatcp)
    testImplementation(libs.flexy.pool.micrometer.metrics)

    testImplementation(libs.commons.dbcp)
    testImplementation(libs.commons.dbcp2)
    testImplementation(libs.tomcat.jdbc)
    testImplementation(libs.hikari.cp)
    testImplementation(libs.flyway.core)
}

tasks {
    compileJava {
        dependsOn(processResources)
    }

    withType<JavaCompile> {
        options.compilerArgs.add("-Xlint:unchecked")
        options.compilerArgs.add("-Xlint:deprecation")
        options.encoding = "UTF-8"
    }

    javadoc {
        val options = options as StandardJavadocDocletOptions
        options.addBooleanOption("html5", true)
        options.addStringOption("Xdoclint:all,-missing", "-quiet")
    }

    test {
        useJUnitPlatform()
    }
}
